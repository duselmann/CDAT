package gov.cida.cdat.control;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public class Time {
	public static final FiniteDuration MILLIS    = Duration.create(100, "milliseconds");
	public static final FiniteDuration SECOND    = Duration.create(  1, "second");
	public static final FiniteDuration HALF_MIN  = Duration.create( 30, "seconds");
	public static final FiniteDuration MINUTE    = Duration.create(  1, "minute");
	public static final FiniteDuration HOUR      = Duration.create(  1, "hour");
	public static final FiniteDuration DAY       = Duration.create(  1, "day");
	
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
}
