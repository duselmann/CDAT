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
}
