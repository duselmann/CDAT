package gov.cida.cdat;

import static org.junit.Assert.*;

import java.lang.reflect.Field;

public class TestUtils {

	public static void log(Object ... objs) {
		// used to print a result that stands out from the logs
		// use logging or println itself if this is not needed
		
		System.out.println();
		for (Object obj:objs) {
			System.out.print(obj==null ?"null" :obj.toString());
			System.out.print(" ");
		}
		System.out.println();
		System.out.println();
	}
	
	
	
	public static Object reflectValue(Class<?> classToReflect, String fieldNameValueToFetch) {
		// reflection access is a bit wonky - this is way many of my classes are package access
		// cannot do it this time because the PipeWorker is a subclass of Worker which does not have a pipe member variable
		try {
			Field reflectField  = reflectField(classToReflect, fieldNameValueToFetch);
			reflectField.setAccessible(true);
			Object reflectValue = reflectField.get(classToReflect);
			return reflectValue;
		} catch (Exception e) {
			fail("Failed to reflect "+fieldNameValueToFetch);
		}
		return null;
	}
	public static Object reflectValue(Object objToReflect, String fieldNameValueToFetch) {
		// reflection access is a bit wonky - this is way many of my classes are package access
		// cannot do it this time because the PipeWorker is a subclass of Worker which does not have a pipe member variable
		try {
			Field reflectField  = reflectField(objToReflect.getClass(), fieldNameValueToFetch);
			Object reflectValue = reflectField.get(objToReflect);
			return reflectValue;
		} catch (Exception e) {
			fail("Failed to reflect "+fieldNameValueToFetch);
		}
		return null;
	}
	public static Field reflectField(Class<?> classToReflect, String fieldNameValueToFetch) {
		// reflection access is a bit wonky - this is way many of my classes are package access
		// cannot do it this time because the PipeWorker is a subclass of Worker which does not have a pipe member variable
		try {
			Field reflectField = null;
			Class<?> classForReflect = classToReflect;
			do {
				try {
					reflectField = classForReflect.getDeclaredField(fieldNameValueToFetch);
				} catch (NoSuchFieldException e) {
					classForReflect = classForReflect.getSuperclass();
				}
			} while (reflectField==null || classForReflect==null);
			reflectField.setAccessible(true);
			return reflectField;
		} catch (Exception e) {
			fail("Failed to reflect "+fieldNameValueToFetch +" from "+ classToReflect);
		}
		return null;
	}
	
	public static void refectSetValue(Object objToReflect, String fieldNameToSet, Object valueToSet) {
		// reflection access is a bit wonky - this is way many of my classes are package access
		// cannot do it this time because the PipeWorker is a subclass of Worker which does not have a pipe member variable
		try {
			Field reflectField  = reflectField(objToReflect.getClass(), fieldNameToSet);
			reflectField.set(objToReflect, valueToSet);
		} catch (Exception e) {
			fail("Failed to reflectively set "+ fieldNameToSet +"="+ valueToSet);
		}
	}
	
	
	// fill a stream with capitol chars other than tag strings
	public static byte[] sampleData() {
		StringBuilder buf = new StringBuilder();
		
		// this ensures that we do not loose the first bytes
		buf.append("begin");
		for (int c=0; c<10000; c++) {
			buf.append((char)(Math.random()*26+65));
		}
		// this this give us something to look for within the data stream
		buf.append("middle");
		for (int c=0; c<10000; c++) {
			buf.append((char)(Math.random()*26+65));
		}
		// this ensures that we do not drop the last bytes
		buf.append("end");
		
		return buf.toString().getBytes();
	}
	
	// sleep with swallowed exceptions for tests
	public static void sleepQuietly(int milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}
