package gov.cida.cdat.transform;

import java.util.List;
import java.util.Map;

public interface IXmlMapping {

	String getRoot();

	String getRootNamespace();

	Map<String, List<String>> getStructure();

	Map<String, String> getHardBreak();
	
	Map<String, List<String>> getGrouping();
}
