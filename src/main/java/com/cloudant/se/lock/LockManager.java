package com.cloudant.se.lock;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

public class LockManager {
	private static final boolean								enabled		= true;
	protected static final ConcurrentMap<String, Semaphore>		locksKnown	= new ConcurrentHashMap<>();
	protected static final ThreadLocal<Map<String, Semaphore>>	locksHeld	= new ThreadLocal<>();

	public static void acquire(String key) throws InterruptedException {
		if (enabled) {
			setupLocksHeld();

			locksKnown.putIfAbsent(key, new Semaphore(1));
			Semaphore s = locksKnown.get(key);

			s.acquire();
			locksHeld.get().put(key, s);
		}
	}

	public static void release(String key) {
		if (enabled) {
			setupLocksHeld();

			if (locksHeld.get().containsKey(key)) {
				Semaphore s = locksHeld.get().remove(key);
				s.release();
			} else {
				throw new RuntimeException("Asked to release a lock on a key that has not held - \"" + key + "\"");
			}
		}
	}

	private static void setupLocksHeld() {
		if (locksHeld.get() == null) {
			locksHeld.set(new HashMap<String, Semaphore>());
		}
	}
}
