package gov.cida.cdat.db;

import gov.cida.cdat.transform.Transformer;

public class PojoCharacterSeparatorTransformer extends Transformer<Pojo> {
	
	String separator = ",";
	
	PojoCharacterSeparatorTransformer(String join) {
		separator = join;
	}
	
	@Override
	public byte[] transform(Pojo person) {
			
		StringBuilder buf = new StringBuilder(200);
		buf.append('"').append(person.name).append('"').append(separator);
		buf.append('"').append(person.address).append('"').append(separator);
		buf.append('"').append(person.phone).append('"');
		
		return buf.toString().getBytes();
	}
}
