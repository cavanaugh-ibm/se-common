package com.cloudant.se.concurrent;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 * This class is a statusing extension of the NotifyingBlockingThreadPoolExecutor class.
 *
 * The functionality added to this class is that it will status every period of time what the pool is doing (threads, queue sizes, etc)
 *
 * @author Joe Cavanaugh
 */
public class StatusingNotifyingBlockingThreadPoolExecutor extends NotifyingBlockingThreadPoolExecutor {
	protected static final Logger	log		= Logger.getLogger(StatusingNotifyingBlockingThreadPoolExecutor.class);
	protected static final String	LOG_MSG	= "STATUS - [ActiveT=%d][CurrentT=%d][MaxT=%d][WaitingTasks=%d][CompletedTasks=%d][TotalTasks=%d]";
	protected static final Timer	timer	= new Timer();

	/**
	 * This constructor is used in order to maintain the first functionality of the NotifyingBlockingThreadPoolExecutor. It does so by using an ArrayBlockingQueue and the BlockThenRunPolicy that is
	 * defined in this class. This constructor allows to give a timeout for the wait on new task insertion and to react upon such a timeout if occurs.
	 *
	 * @param poolSize
	 *            is the amount of threads that this pool may have alive at any given time
	 * @param queueSize
	 *            is the size of the queue. This number should be at least as the pool size to make sense (otherwise there are unused threads), thus if the number sent is smaller, the poolSize is used
	 *            for the size of the queue. Recommended value is twice the poolSize.
	 * @param keepAliveTime
	 *            is the amount of time after which an inactive thread is terminated
	 * @param keepAliveTimeUnit
	 *            is the unit of time to use with the previous parameter
	 * @param maxBlockingTime
	 *            is the maximum time to wait on the queue of tasks before calling the BlockingTimeout callback
	 * @param maxBlockingTimeUnit
	 *            is the unit of time to use with the previous parameter
	 * @param threadFactory
	 *            the factory to use when the executor creates a new thread - defaults to Executors.defaultThreadFactory if null
	 * @param blockingTimeCallback
	 *            is the callback method to call when a timeout occurs while blocking on getting a new task, the return value of this Callable is Boolean, indicating whether to keep blocking (true) or
	 *            stop (false). In case false is returned from the blockingTimeCallback, this executer will throw a RejectedExecutionException
	 */
	public StatusingNotifyingBlockingThreadPoolExecutor(int poolSize, int queueSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long maxBlockingTime, TimeUnit maxBlockingTimeUnit,
			ThreadFactory threadFactory, Callable<Boolean> blockingTimeCallback) {
		super(poolSize, queueSize, keepAliveTime, keepAliveTimeUnit, maxBlockingTime, maxBlockingTimeUnit, threadFactory, blockingTimeCallback);
		setupStatusTask();
	}

	/**
	 * This constructor is used in order to maintain the first functionality of the NotifyingBlockingThreadPoolExecutor. It does so by using an ArrayBlockingQueue and the BlockThenRunPolicy that is
	 * defined in this class. Using this constructor, waiting time on new task insertion is unlimited.
	 *
	 * @param poolSize
	 *            is the amount of threads that this pool may have alive at any given time.
	 * @param queueSize
	 *            is the size of the queue. This number should be at least as the pool size to make sense (otherwise there are unused threads), thus if the number sent is smaller, the poolSize is used
	 *            for the size of the queue. Recommended value is twice the poolSize.
	 * @param keepAliveTime
	 *            is the amount of time after which an inactive thread is terminated.
	 * @param unit
	 *            is the unit of time to use with the previous parameter.
	 * @param threadFactory
	 *            the factory to use when the executor creates a new thread - defaults to Executors.defaultThreadFactory if null
	 */
	public StatusingNotifyingBlockingThreadPoolExecutor(int poolSize, int queueSize, long keepAliveTime, TimeUnit unit, ThreadFactory threadFactory) {
		super(poolSize, queueSize, keepAliveTime, unit, threadFactory);
		setupStatusTask();
	}

	private void setupStatusTask() {
		long interval = TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS);
		timer.schedule(new StatusTask(), interval, interval);
	}

	protected void logStatus() {
		log.info(String.format(LOG_MSG, getActiveCount(), getPoolSize(), getMaximumPoolSize(), getQueue().size(), getCompletedTaskCount(), getTaskCount()));
	}

	@Override
	protected void terminated() {
		try {
			timer.cancel();
		} catch (Exception e) {
		}

		logStatus();
	}

	class StatusTask extends TimerTask {
		@Override
		public void run() {
			logStatus();
		}
	}
}
