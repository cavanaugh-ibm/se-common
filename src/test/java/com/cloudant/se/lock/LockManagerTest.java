package com.cloudant.se.lock;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class LockManagerTest {
	@Test
	public void testAcquire() {
		try {
			LockManager.acquire("testing");

			assertTrue("The locks known does not have an entry for our key", LockManager.locksKnown.containsKey("testing"));
			assertTrue("Help locks should contain the lock we just acquired", LockManager.locksHeld.get().containsKey("testing"));
		} catch (InterruptedException e) {
			fail("Should not have been interrupted");
		}
	}

	@Test
	public void testRelease() {
		LockManager.release("testing");

		try {
			LockManager.release("testing2");
		} catch (RuntimeException e) {
			if (!StringUtils.contains(e.getMessage(), "Asked to release a lock on a key that has not held")) {
				fail("Should not have allowed a release on a key we didn't lock");
			}
		}
	}
}
