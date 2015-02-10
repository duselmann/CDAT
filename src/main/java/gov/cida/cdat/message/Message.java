package gov.cida.cdat.message;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>Messages are strongly typed key:value maps of type (string,string) where the value is used
 * to qualify the message (the key is the message). For example, (stop,force) would abort a worker
 * where (stop,null) might wait for a clean exit (at least clean up resources if force does not).
 * </p>
 * <p>Each custom worker is responsible for addressing its own custom messages.
 * </p>
 * <p>Messages are also the return type for each message sent to a worker. For example, a config
 * status inquiry could return a message of key:value pairs for its config. As in: server, port, etc 
 * </p>
 * <p>It is not a concurrent map because we will enforce immutable messages. Each layer will create
 * a new message from the old to extend.
 * </p>
 * 
 * TODO impl custom workers
 * 
 * @author duselman
 *
 */
public class Message implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(Message.class);
	
	/**
	 * <p>The internal map containing the message. A message is a possible compose-able item.
	 * Many directives may be in a single message.
	 * </p>
	 * <pre>Example:
	 * isStart and isDone are a nice combo. 
	 * Suppose you needed to know if the work was done.
	 * Well, it is also important to know if the work was
	 * started if done is false. One request returns two inquires.
	 * </pre>
	 */
	private final Map<String, String> message;
	
	/**
	 * This is private to ensure that the messages are immutable.
	 */
	protected Message() {
		message = new HashMap<String,String>();
	}
	
	/**
	 * This is not a constructor because originally I had intended on returning the Map
	 * but I like the clarity of the stronger Message type, I suppose these could be constructors now.
	 * 
	 * @return new message with a protective copy of the given map message entries.
	 */
	public static Message create(Map<String, String> map) {
		Message msg = new Message();
		msg.message.putAll(map);
		trace(msg.message);
		return msg;
	}
	/**
	 * A convenience method for a message without a value - name only.
	 * @param name message name
	 * @return new simple message of the given name only
	 */
	public static Message create(Object name) {
		return create(name, null);
	}
	/**
	 * Name:value pair message creator method.
	 * This is useful for creating message returned to the user.
	 * For example, if the user sends a Status.isAlive then the return could be isAlive:true.
	 * Another example of potential use is the user sending a qualifier for a message.
	 * For example, Control.Stop:Force could be used to stop the current worker without waiting or cleanup.
	 * 
	 * @param name message name
	 * @param value message qualifier
	 * @return a message created from the name:value pair
	 */
	public static Message create(Object name, Object value) {
		if (name == null || "".equals(name)) {
			throw new NullPointerException("Message name is required.");
		}
		Message msg = new Message();
		
		String stringValue = null;
		if (value != null) {
			stringValue = value.toString();
		}
		msg.message.put(name.toString(), stringValue);
		trace(msg.message);
		return msg;
	}
	/**
	 * Since messages are immutable and this helper method creates an extended copy of the 
	 * original message with an additional message entry.
	 * @param original the source of the original message the we want to extend
	 * @param name additional message name
	 * @param value additional message qualifier
	 * @return new extended message
	 */
	public static Message extend(Message original, Object name, String value) {
		if (null == original) {
			return create(name,value);
		}
		Message msg = create(original.message);
		msg.message.put(name.toString(), value);
		return msg;
	}
	/**
	 * convenience method for creating a boolean qualifier
	 * @param name message name
	 * @param value message qualifier
	 * @return new message qualified with the string representation of the boolean value
	 */
	public static Message create(Object name, boolean value) {
		return create(name, ""+value);
	}
	

	/**
	 * Parse an integer value from the message map if one exists;
	 * otherwise return the default value.
	 * 
	 * I like the static util nature of this method and others to follow like it.
	 * It implies that they act on an instance rather then this/self.
	 * 
	 * @param msg the message map to act on. message are maps and Message class is a helper
	 * @param name the message key name to fetch
	 * @param defaultValue the default value to return if there is no entry or it is invalid
	 * @return integer message value or the given default
	 */
	public static int getInt(Message msg, String name, int defaultValue) {
		String value = msg.get(name);
		if (value == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	/**
	 * Delegate get to internal map. Since this is a
	 * Map<String,String> we use the toString to ensure
	 * we find a potential match.
	 * 
	 * @param key
	 * @return the message value of the given key
	 */
	public String get(Object key) {
		if (key==null) {
			return null;
		}
		return message.get(key.toString());
	}
	
	/**
	 * Delegate contains to internal map. Since this is a
	 * Map<String,String> we use the toString to ensure
	 * we find a potential match.
	 * 
	 * @param key
	 * @return true if the message has the given key
	 */
	public boolean contains(Object key) {
		if (key == null) {
			return false;
		}

		return message.containsKey(key.toString());
	}
	
	/**
	 * Created  a nice string representations of the message(s)
	 * in the Map with no nulls printed when the optional value
	 * is omitted. When nulls print it looks like an error.
	 */
	@Override
	public String toString() {
		return message.toString().replaceAll("=null", "");
	}
	
	
	/**
	 * This is a very convenient method that adds the caller line to the message 
	 * when in TRACE logging mode. In conjunction with the DeadLetterLogger, it is
	 * a an invaluable troubleshooting duo.
	 * 
	 * @see gov.cida.cdat.service.DeadLetterLogger
	 * 
	 * @param message
	 */
	static void trace(Map<String, String> message) {
		if ( ! logger.isTraceEnabled() ) {
			return;
		}
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		int entry = 3;
		StackTraceElement last = stack[entry];
		while (last.getClassName().endsWith("Message")) {
			last = stack[++entry];
		}
		String caller = last.getClassName() +"."+ last.getMethodName() 
				+":"+ last.getLineNumber();
		message.put("TRACE", caller);
	}
}
