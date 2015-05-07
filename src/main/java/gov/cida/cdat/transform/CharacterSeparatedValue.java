package gov.cida.cdat.transform;

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharacterSeparatedValue extends Transformer {
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public static final Pattern QUOTE = Pattern.compile("\"");
	public static final String  COMMA = ",";
	public static final String  TAB = "\t";
	public static final String  NEW_LINE = "\n";
	public static final String  NEW_LINE_CR = "\r\n";
	
	public static final CharacterSeparatedValue CSV = new CharacterSeparatedValue(COMMA);
	public static final CharacterSeparatedValue TSV = new CharacterSeparatedValue(TAB);

	private final String valueSeparator;
	private final String entrySeparator;

	boolean doHeader = true;
	
	public CharacterSeparatedValue() {
		this(COMMA, NEW_LINE);
	}
	
	public CharacterSeparatedValue(String valueSeparator) {
		this(valueSeparator, NEW_LINE);
	}
	
	public CharacterSeparatedValue(String valueSeparator, String entrySeparator) {
		if (valueSeparator == null || entrySeparator == null) {
			throw new RuntimeException("Separators must provided for " + getClass());
		}
		if (valueSeparator == null || entrySeparator == null) { // TODO this should check for blank
			throw new RuntimeException("Separators must provided for " + getClass());
		}
		this.valueSeparator = valueSeparator;
		this.entrySeparator = entrySeparator;
	}
	
	
	@Override
	public <T> byte[] transform(T obj) {
		StringBuilder csv = new StringBuilder();
		if (obj instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String,Object> map = (Map<String,Object>) obj;
			csv.append( prefix(map) );
			csv.append( transform(map) );
		}
		try {
			return csv.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			return csv.toString().getBytes();
		}
	}
	
	public String transform(Map<String,Object> map) {		
		log.trace("need entry: {}", map);

		StringBuilder entry = new StringBuilder();
		String sep = "";
		for (Map.Entry<String,Object> column : map.entrySet()) {
			entry.append(sep);
			String initialValue = "";
			if ( null != column.getValue() ) {
				initialValue = column.getValue().toString();
			}
			String encodeValue = encode(initialValue);
			entry.append(encodeValue);
			sep = valueSeparator;
		}
//		stream.write( transformer.getEntrySeparator().getBytes() );
		return entry.toString();
	}
	

	String doHeader(Map<String,Object> headerEntry) {
		doHeader = false;
		// this makes the Java DRY
		Map<String,	Object> headerMap = new LinkedHashMap<String, Object>();
		for (Map.Entry<String,Object> column : headerEntry.entrySet()) {
			headerMap.put(column.getKey(), column.getKey());
		}
		return transform(headerMap);
	}
	
	
	@Override
	public String encode(String value) {
		if (value == null) {
			return "";
		}
		String processing = value;
		processing = XmlUtils.unEscapeXMLEntities(processing);
		processing = processing.replaceAll("[\n\r\t]", ""); // Handles newlines and carriage returns, and tabs
		if (COMMA.equals(valueSeparator)) { // Currently handles commas and quotes.
			boolean hasQuotes = processing.indexOf('"') >= 0;
			if (hasQuotes) {
				Matcher matcher = QUOTE.matcher(processing);
				processing = matcher.replaceAll("\"\""); // escape quotes by doubling them
			}
			boolean encloseInQuotes = hasQuotes || processing.indexOf(',')>=0;
			processing = (encloseInQuotes)  ?'"'+processing+'"'  :processing;
		}
		return processing;
	}
	
	
	/**
	 * The record prefix is the header for the first row
	 * and a simple comma for subsequent rows.
	 */
	String prefix(Map<String,Object> entry) {
		String prefix = "";
		if (doHeader) {
			prefix = doHeader(entry);
		}
		prefix += entrySeparator;
		return prefix;
	}
	
	
	public String getValueSeparator() {
		return valueSeparator;
	}
	public String getEntrySeparator() {
		return entrySeparator;
	}
}