package gov.cida.cdat.control;

import java.util.HashMap;


// standard message map
public class Message extends HashMap<String, String> {
	private static final long serialVersionUID = 1L;

	public static Message create() {
		return new Message();
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
}
