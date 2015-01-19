package gov.cida.cdat.control;

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
 * TODO impl custom workers
 * 
 * @author duselman
 *
 */
public class Message extends HashMap<String, String> { // TODO maybe concurrent hashmap, read only map, message id
	private static final long serialVersionUID = 1L;

	// TODO should this be private or exposed?
	private Message() {
		
	}
	
	/**
	 * This is not a constructor because originally I had intended on returning the Map
	 * but I like the clarity of the stronger Message type, I suppose these could be constructors now.
	 * 
	 * @return
	 */
	public static Message create() {
		return new Message();
	}
	public static Message create(Map<String, String> map) {
		Message message = new Message();
		message.putAll(map);
		return message;
	}
	public static Message create(Object name) {
		return create(name, null);
	}
	public static Message create(Object name, String value) {
		if (name == null || "".equals(name)) {
			throw new NullPointerException("Message name is required.");
		}
		Message msg = create();
		msg.put(name.toString(), value);
		return msg;
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
	public static int getInt(Map<String, String> msg, String name, int defaultValue) {
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
}
