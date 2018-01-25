package com.db.awmd.challenge;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.InsufficientFundsException;
import com.db.awmd.challenge.exception.InvalidAccountException;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;

import lombok.extern.slf4j.Slf4j;

/**
 * This test class is to demonstrate that account transfer service work with
 * multiple threads and there is no deadlock occurs when multiple threads are
 * invoked.
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class TransferServiceConcurrencyTest {

	NotificationService emailNotificationService = Mockito.mock(NotificationService.class);

	@Autowired
	@InjectMocks
	private AccountsService accountsService;

	private static AtomicInteger successCount = new AtomicInteger(0);
	private static AtomicInteger failureCount = new AtomicInteger(0);

	@Before
	public void setup() throws InvalidAccountException {
		log.debug("setup is run***************************************");

		accountsService.createAccount(new Account("Id-124", new BigDecimal("3000")));
		log.info("intial balance in Id-124 :" + accountsService.getAccount("Id-124").getBalance());
		accountsService.createAccount(new Account("Id-125", new BigDecimal("4000")));
		log.info("intial balance in Id-124 :" + accountsService.getAccount("Id-125").getBalance());

	}

	@Test
	public void testConcurrentTransactions() {
		Runnable transfer1 = () -> {
			try {

				setSuccessFailureCounters(accountsService.transferAmount("Id-124", "Id-125", new BigDecimal("1")));
			} catch (InsufficientFundsException | InterruptedException | InvalidAccountException e) {
				log.error("" + e);
			}
		};

		Runnable transfer2 = () -> {
			try {
				setSuccessFailureCounters(accountsService.transferAmount("Id-125", "Id-124", new BigDecimal("2")));
			} catch (InsufficientFundsException | InterruptedException | InvalidAccountException e) {
				log.error("" + e);
			}
		};
		ExecutorService service = Executors.newCachedThreadPool();
		IntStream.range(0, 1000).parallel().forEach(counter -> {
			service.execute(transfer1);
			service.execute(transfer2);

		});
		service.shutdown();

	}

	@After
	public void tearDown() throws InvalidAccountException, InterruptedException {
		Thread.sleep(1000);

		log.info("Number of successful transfers" + successCount);
		log.info("Number of failed transfers" + failureCount);
		log.info("The balance in Id-124 is " + accountsService.getAccount("Id-124").getBalance());
		log.info("The balance in Id-125 is " + accountsService.getAccount("Id-125").getBalance());
		assertTrue(accountsService.getAccount("Id-124").getBalance().intValue() == 4000);
		assertTrue(accountsService.getAccount("Id-125").getBalance().intValue() == 3000);

	}

	private void setSuccessFailureCounters(boolean isTransferSuccessful) {
		if (isTransferSuccessful) {
			successCount.getAndIncrement();
		} else {
			failureCount.getAndIncrement();
		}
	}
}
