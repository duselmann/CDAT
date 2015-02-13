package gov.cida.cdat;

import static org.junit.Assert.assertFalse;

import java.lang.reflect.Field;

public class TestUtils {

	public static int waitAlittleWhileForResponse(Object response[]) {
		TestUtils.log("waiting for ", response.length, " responses");
		
		int count=0;
		for (int r=0; r<response.length; r++) {
			while (null==response[r] && count++ < 100) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}
			}
		}
		return count;
	}

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
			Field pipeField     = classToReflect.getDeclaredField(fieldNameValueToFetch);
			pipeField.setAccessible(true);
			Object reflectValue = pipeField.get(classToReflect);
			return reflectValue;
		} catch (Exception e) {
			assertFalse("Failed to reflect "+fieldNameValueToFetch, true);
		}
		return null;
	}
	public static Object reflectValue(Object objToReflect, String fieldNameValueToFetch) {
		// reflection access is a bit wonky - this is way many of my classes are package access
		// cannot do it this time because the PipeWorker is a subclass of Worker which does not have a pipe member variable
		try {
			Field pipeField     = objToReflect.getClass().getDeclaredField(fieldNameValueToFetch);
			pipeField.setAccessible(true);
			Object reflectValue = pipeField.get(objToReflect);
			return reflectValue;
		} catch (Exception e) {
			assertFalse("Failed to reflect "+fieldNameValueToFetch, true);
		}
		return null;
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
	
}
