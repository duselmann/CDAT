package gov.cida.cdat.transform;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;

public class MapToJsonTransformer extends Transformer {

	private Gson gson = new Gson();

	private final String header;
	private final String footer;
	
	/** 
	 * Has the first record written to the stream
	 */
	private boolean wroteFirst;

	
	public MapToJsonTransformer(String header, String footer) {
		this.header = header;
		this.footer = footer;
	}
		
	
	@Override
	public <T> byte[] transform(T map) {
		String json = prefix();
		if (map instanceof Map) {
			json += gson.toJson(map);
		}
		try {
			return json.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			return json.getBytes();
		}
	}
	
	
	@Override
	public String encode(String value) {
		return StringEscapeUtils.escapeJson(value);
	}

	
	/**
	 * The record prefix is the header for the first row
	 * and a simple comma for subsequent rows.
	 */
	String prefix() {
		String prefix;
		if (wroteFirst) {
			prefix = ",";
		} else {
			prefix = header;
			wroteFirst = true;
		}
		return prefix;
	}
	
	@Override
	public byte[] getRemaining() {
		if (StringUtils.isEmpty(footer)) {
			return new byte[0];
		}
		try {
			return footer.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			return footer.getBytes();
		}
	}
	
}
