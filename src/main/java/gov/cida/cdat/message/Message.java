package gov.cida.cdat.message;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 * Messages are strongly typed key:value maps of type (string,string) where the value is used
 * to qualify the message (the key is the message). For example, (stop,force) would abort a worker
 * where (stop,null) might wait for a clean exit (at least clean up resources if force does not).
 * 
 * Each custom worker is responsible for addressing its own custom messages.
 * 
 * Messages are also the return type for each message sent to a worker. For example, a config
 * status inquiry could return a message of key:value pairs for its config. As in: server, port, etc 
 * 
 * It is not a concurrent map because we will enforce immutable messages. Each layer will create a new message from the old to extend.
 * 
 * TODO impl custom workers
 * 
 * @author duselman
 *
 */
public class Message implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * the internal map containing the message.
	 * A message is a possible composable item.
	 * Many directives may be in a single message.
	 * 
	 * Example:
	 * isStart and isDone are a nice combo. 
	 * Suppose you needed to know if the work was done.
	 * Well, it is also important to know if the work was
	 * started if done is false. One request returns two inquires.
	 * 
	 */
	private final Map<String, String> message;
	
	// TODO should this be private or exposed?
	protected Message() {
		message = new HashMap<String,String>();
	}
	
	/**
	 * This is not a constructor because originally I had intended on returning the Map
	 * but I like the clarity of the stronger Message type, I suppose these could be constructors now.
	 * 
	 * @return
	 */
	public static Message create(Map<String, String> map) {
		Message msg = new Message();
		msg.message.putAll(map);
		return msg;
	}
	public static Message create(Object name) {
		return create(name, null);
	}
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
		return msg;
	}
	public static Message extend(Message original, Object name, String value) {
		if (null == original) {
			return create(name,value);
		}
		Message msg = create(original.message);
		msg.message.put(name.toString(), value);
		return msg;
	}
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
}
