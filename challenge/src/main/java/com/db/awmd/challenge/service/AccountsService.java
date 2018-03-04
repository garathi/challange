package com.db.awmd.challenge.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.InsufficientFundsException;
import com.db.awmd.challenge.exception.InvalidAccountException;
import com.db.awmd.challenge.repository.AccountsRepository;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AccountsService {

	@Getter
	private final AccountsRepository accountsRepository;

	private NotificationService notificationService;

	//Can be configure in yml.
	private static final long TIME_OUT = 2000L;

	private static final TimeUnit TIME_UNIT_MILISECONDS = TimeUnit.MILLISECONDS;
	

  @Autowired
  public AccountsService(AccountsRepository accountsRepository) {
    this.accountsRepository = accountsRepository;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }
  
  public boolean transferAmount(final String fromAccountId, final String toAccountId, BigDecimal amount)
			throws InsufficientFundsException, InterruptedException, InvalidAccountException {
		boolean isTransferSuccessful = false;
		if (StringUtils.isNotEmpty(fromAccountId) && StringUtils.isNotEmpty(toAccountId) && amount.compareTo(BigDecimal.ZERO) == 1) {

			Account fromAccount = this.getAccount(fromAccountId);
			Account toAccount = this.getAccount(toAccountId);
			isTransferSuccessful = this.transfer(fromAccount, toAccount, amount);
		}
		return isTransferSuccessful;

	}

	private boolean transfer(final Account fromAcct, final Account toAcct, final BigDecimal amount)
			throws InsufficientFundsException, InterruptedException, InvalidAccountException {

		final Account[] accounts = new Account[] { fromAcct, toAcct };
		Arrays.sort(accounts);
		if (accounts[0].getLock().tryLock(TIME_OUT, TIME_UNIT_MILISECONDS)) {
			try {
				if (accounts[1].getLock().tryLock(TIME_OUT, TIME_UNIT_MILISECONDS)) {
					try {
						return transferMoney(fromAcct, toAcct, amount);

					} finally {

						accounts[1].getLock().unlock();
					}
				}
			} finally {
				accounts[0].getLock().unlock();
			}
		}

		log.warn("Lock not acquired,Treansaction could not be completed.Exiting gracefully");
		return false;

	}

	private boolean transferMoney(final Account fromAcct, final Account toAcct, final BigDecimal amount)
			throws InsufficientFundsException, InvalidAccountException {
		if (fromAcct.getBalance().compareTo(amount) < 0)
			throw new InsufficientFundsException("Available balance is less that amount to transfer" + amount);
		else {

			fromAcct.setBalance(fromAcct.getBalance().subtract(amount));
			toAcct.setBalance(toAcct.getBalance().add(amount));
			log.info("Amount {} successfuly transfered from account {} to acoount {}",amount,fromAcct.getAccountId(),toAcct.getAccountId());
			notificationService.notifyAboutTransfer(fromAcct,
					"Account " + fromAcct.getAccountId() + " debited with amount " + amount);
			notificationService.notifyAboutTransfer(toAcct,
					"Account " + toAcct.getAccountId() + " debited with amount " + amount);
			
			return true;
		}
	}
	
}
