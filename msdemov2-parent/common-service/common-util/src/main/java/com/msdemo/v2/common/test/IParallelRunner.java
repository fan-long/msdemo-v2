package com.msdemo.v2.common.test;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface IParallelRunner {

	Logger logger = LoggerFactory.getLogger(IParallelRunner.class);
	
	default void logic(){};
	
	default void logic1(){};
	default void logic2(){};
	
	default void run(int count){
		final CountDownLatch countdown = new CountDownLatch(1);
		final CyclicBarrier cyclicBarrier = new CyclicBarrier(count+1);
		for (int i = 0; i < count; i++) {
			new Thread(() -> {
				try {
					countdown.await();					
					logic();
				} catch (Exception e) {
					logger.error(e.getMessage());
				} finally {
					try {
						cyclicBarrier.await();
					} catch (InterruptedException | BrokenBarrierException e) {
						e.printStackTrace();;
					}
				}
			}, "logic-" + i).start();
		}
		countdown.countDown();
		try {
			cyclicBarrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			e.printStackTrace();
		}
	}
	
	default void run2(int count1,int count2){
		final CountDownLatch countdown = new CountDownLatch(1);
		final CyclicBarrier cyclicBarrier = new CyclicBarrier(count1+count2+1);
		for (int i = 0; i < count1; i++) {
			new Thread(() -> {
				try {
					countdown.await();					
					logic1();
				} catch (Exception e) {
					logger.error(e.getMessage());
				} finally {
					try {
						cyclicBarrier.await();
					} catch (InterruptedException | BrokenBarrierException e) {
						e.printStackTrace();;
					}
				}
			}, "logic1-" + i).start();
		}
		for (int i = 0; i < count2; i++) {
			new Thread(() -> {
				try {
					countdown.await();					
					logic2();
				} catch (Exception e) {
					logger.error(e.getMessage());
				} finally {
					try {
						cyclicBarrier.await();
					} catch (InterruptedException | BrokenBarrierException e) {
						e.printStackTrace();;
					}
				}
			}, "logic2-" + i).start();
		}
		countdown.countDown();
		try {
			cyclicBarrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			e.printStackTrace();
		}
		logger.info("finished");
	}
	
	
}
