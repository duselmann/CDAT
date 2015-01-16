package gov.cida.cdat.control;

import java.util.HashMap;
import java.util.Map;


// standard message map
public class Message extends HashMap<String, String> { // TODO maybe concurrent hashmap, read only map, message id
	private static final long serialVersionUID = 1L;

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
