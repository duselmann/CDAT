package gov.cida.cdat.transform;

import java.util.regex.Matcher;

public abstract class XmlUtils implements XmlConstants {
	
	/**
	 * Performs a quick, possibly incomplete removal or replacement of illegal
	 * characters in an element name. ".", "-", and "_" are legal in a name, and
	 * ":" is part of a namespace
	 *
	 * @param aName
	 * @return
	 */
	public static String xmlQuickSanitize(String aName) {
		if (aName.indexOf('/') >= 0 || aName.indexOf('@') >= 0) {
			Matcher matcher = NON_QNAME_SEPARATORS.matcher(aName);
			aName = matcher.replaceAll("_");
		}
		return aName;
	}
	
	/**
	 * Removes or replaces illegal characters in an element name. ".", "-", and
	 * "_" are legal in a name, and ":" is part of a namespace
	 *
	 * @param aName
	 * @return
	 */
	public static String xmlFullSanitize(String aName) {
		Matcher matcher = NON_QNAME_SEPARATORS.matcher(aName);
		matcher = NON_QNAME_CHARS.matcher(matcher.replaceAll("_"));
		return matcher.replaceAll("");
	}
	
	public static String escapeAngleBrackets(String aString) {
		if (aString.indexOf('<') >= 0) {
			aString = LEFT_ANGLE_BRACKET.matcher(aString).replaceAll("&lt;");
		}
		if (aString.indexOf('>') >= 0) {
			aString = RIGHT_ANGLE_BRACKET.matcher(aString).replaceAll("&gt;");
		}
		return aString;
	}
	
	public static String quickTagContentEscape(String aString) {
		if (aString.indexOf('&') >= 0
				&& aString.indexOf("&amp;") < 0
				&& aString.indexOf("&quot;") < 0
				&& aString.indexOf("&lt;") < 0
				&& aString.indexOf("&gt;") < 0
				&& aString.indexOf("&apos;") < 0) {
			aString = AMPERSAND.matcher(aString).replaceAll("&amp;");
		}
		return escapeAngleBrackets(aString);
	}
	
	/**
	 * Replace the five standard xml entities: &lt; &gt; &quot; &apos; &amp;
	 * @param aString
	 * @return
	 */
	public static String unEscapeXMLEntities(String aString) {
		//
		if (aString.indexOf('&') >= 0) {
			Matcher matcher = LESS_THAN.matcher(aString);
			String results = matcher.replaceAll("<");
			matcher = GREATER_THAN.matcher(results);
			results = matcher.replaceAll(">");
			matcher = QUOTE.matcher(results);
			results = matcher.replaceAll("\"");
			matcher = APOS.matcher(results);
			results = matcher.replaceAll("'");
			matcher = AMP.matcher(results);
			results = matcher.replaceAll("&");
			return results;
		}
		return aString;
	}
}