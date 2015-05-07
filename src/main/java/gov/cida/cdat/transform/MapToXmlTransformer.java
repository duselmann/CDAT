package gov.cida.cdat.transform;


import java.io.UnsupportedEncodingException;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MapToXmlTransformer extends Transformer {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
    private Deque<String> nodes = new LinkedList<>();

	private IXmlMapping fieldMapping;
	private Map<String, String> groupings;
	
	private final String header;
	
	/** 
	 * Is this the first write to the stream.
	 */
	private boolean first = true;

	
	public MapToXmlTransformer(IXmlMapping fieldMapping, String header) {
		this.fieldMapping = fieldMapping;
		this.header  = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+header;
		
		groupings = new HashMap<>();
		for (String key : fieldMapping.getHardBreak().keySet()) {
			groupings.put(key, null);
		}
	}

	/** 
	 * The XML file header.
	 */
	String prefix() {
		if (first) {
			nodes.push(fieldMapping.getRoot());
			first = false;
			return header;
		}
		return "";
	}
	
	
	/**
	 * Extract the data and write to the stream.
	 */
	String prepareData(Map<String, Object> map) {

		StringBuilder sb = new StringBuilder(prefix());
		
		for (String key : fieldMapping.getHardBreak().keySet()) {
			Object value = map.get(key);
			if (value != null) {
				String encode = encode(value.toString());
				if ( ! encode.equalsIgnoreCase(groupings.get(key)) ) {
					if (nodes.contains(fieldMapping.getHardBreak().get(key))) {
						closeNodes(sb, fieldMapping.getHardBreak().get(key));
					}
					doGrouping(map, sb, key);
					groupings.put(key, encode);
				}
			}
		}
		
		return sb.toString();
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> byte[] transform(T map) {
		String xml = "";
		if (map instanceof Map) {
			xml = prepareData((Map<String, Object>)map);
		}
		
		try {
			return xml.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			return xml.getBytes();
		}
	}
	

	void doGrouping(Map<String, Object> map, StringBuilder sb, String key) {
		List<String> cols = fieldMapping.getGrouping().get(key);
		Iterator<String> i = cols.iterator();
		while (i.hasNext()) {
			String col = i.next();
			Object obj = map.get(col);
			if (obj==null) {
				continue;
			}
			String val = encode(obj.toString());
			doNode(sb, col, val);
		}
	}
	
	
	void doNode(StringBuilder sb, String key, String val) {
		String lNode = "Provider"; // TODO asdf refactor to constructor param
		List<String> pos = fieldMapping.getStructure().get(key);
		for (String node : pos) {
			log.trace("node - positionNode:" + node + " lastNode:" + lNode);
			if ( ! nodes.contains(node) ) {
				if ( ! lNode.equalsIgnoreCase(nodes.peek()) ) {
					closeNodes(sb, lNode);
				}
				sb.append("<").append(node).append(">");
				nodes.push(node);
			}
			lNode = node;
		}
		sb.append(val);
		sb.append("</").append(nodes.pop()).append(">");
	}
	
	
	void closeNodes(StringBuilder sb, String targetNode) {
		log.trace("closeNodes - targetNode: " + targetNode);
		
    	while (!nodes.isEmpty() && !fieldMapping.getRoot().equalsIgnoreCase(nodes.peek())) {
    		String node = nodes.peek();
    		log.trace("closeNodes - currentNode: " + node);
    		if (targetNode.equalsIgnoreCase(node)) {
    			break;
    		} else {
	    		sb.append("</").append(node).append(">");
	    		nodes.pop();
    		}
    	}

	}
	
	
	// TODO asdf this is a copy from Json
	@Override
	public byte[] getRemaining() {
		StringBuilder footer = new StringBuilder();
		
    	while ( ! nodes.isEmpty() ) {
    		footer.append("</" + nodes.element() + ">");
    		nodes.pop();
    	}
    	
		if (StringUtils.isEmpty(footer)) {
			return new byte[0];
		}
		try {
			return footer.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			return footer.toString().getBytes();
		}
	}

	
	@Override
	public String encode(String value) {
		return StringEscapeUtils.escapeXml11(value);
	}
	
}
