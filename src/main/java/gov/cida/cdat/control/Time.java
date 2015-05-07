package gov.cida.cdat.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.util.Timeout;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public enum Time {
	MS(100, "milliseconds"),
	SECOND(  1, "second"),
	HALF_MINUTE( 30, "seconds"),
	MINUTE(  1, "minute"),
	HOUR(  1, "hour"),
	DAY(  1, "day");
		
	private static final Logger logger = LoggerFactory.getLogger(Time.class);
	
	public final FiniteDuration duration;
	
	Time(long amount, String unit) {
		duration =  Duration.create(amount, unit);
	}
	
	public long asMS() {
		return duration.toMillis();
	}
	public Timeout asTimeout() {
		return new Timeout(duration);
	}
	
	/**
	 * @return current time in milliseconds
	 */
	public static long now() {
		return System.currentTimeMillis();
	}
	
	/**
	 * @param duration milliseconds to add to the current time
	 * @return current time plus the given duration
	 */
	public static long later(long duration) {
		return now() + duration;
	}
	
	/**
	 * convenience method for scala lib durations
	 * @param duration an amount to add to the current time
	 * @return the current time plue the given duration
	 */
	public static long later(FiniteDuration duration) {
		return later(duration.toMillis());
	}
	
	/**
	 * @param timeInMilliseconds a time in milliseconds
	 * @return now - the given time (a duration)
	 */
	public static long duration(long timeInMilliseconds) {
		return now() - timeInMilliseconds;
	}
	
	// then wait for it to complete with a smaller cycle but not forever
	public static int waitForResponse(Object response[], long interval) {
		logger.trace("waiting for {} responses", response.length);
		
		int count=0;
		for (int r=0; r<response.length; r++) {
			while (null==response[r] && count++ < 100) {
				try {
					Thread.sleep(interval);
				} catch (InterruptedException e) {}
			}
		}
		logger.trace("waited {} intervals of {}ms for responses", count, interval);
		
		return count;
	}
	public static long waitForResponse(Object response[], Time interval, Time duration) {
		logger.trace("waiting for {} responses", response.length);
		
		long waitTime=0;
		for (int r=0; r<response.length; r++) {
			while (null==response[r] && waitTime < duration.asMS()) {
				try {
					logger.trace("Sleeping for interval {} of duration {}",interval.asMS(), duration.asMS());
					Thread.sleep(interval.asMS());
				} catch (InterruptedException e) {}
			}
		}
		logger.trace("waited a total of {}ms for response", waitTime);
		
		return waitTime;
	}
	
	public static void slumber(long milliseconds) {
		long later = later(milliseconds);
		
		while (now() < later) {
			try {
				Thread.sleep(milliseconds);
			} catch (Exception e) {
				// if interrupted then wait the remaining time
				milliseconds = later - now();
			}
		}
	}
}
