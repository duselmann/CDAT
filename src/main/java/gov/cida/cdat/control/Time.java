package gov.cida.cdat.control;

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
}
