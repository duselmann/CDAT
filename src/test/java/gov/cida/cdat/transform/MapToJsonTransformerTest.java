package gov.cida.cdat.transform;

import static org.junit.Assert.*;
import java.util.LinkedHashMap;

import org.junit.Test;

public class MapToJsonTransformerTest {

	@Test
	public void test() {
		MapToJsonTransformer json = new MapToJsonTransformer("[","]"); 
		// note the footer is written by the transforming stream because it knows when rows are done.
		
		LinkedHashMap<String,Object> map = new LinkedHashMap<String,Object>();
		map.put("key1", "value1");
		map.put("key2", "value2");

		LinkedHashMap<String,Object> sub = new LinkedHashMap<String,Object>();
		sub.put("subkey1", "subvalue1");
		sub.put("subkey2", "subvalue2");
		
		map.put("sub",sub);
		
		String actual = new String( json.transform(map) );
		String expect = "[{\"key1\":\"value1\",\"key2\":\"value2\",\"sub\":{\"subkey1\":\"subvalue1\",\"subkey2\":\"subvalue2\"}}";
		System.out.println(actual);
		
		assertEquals(expect, actual);
	}

}
