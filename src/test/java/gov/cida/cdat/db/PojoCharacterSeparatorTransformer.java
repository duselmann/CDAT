package gov.cida.cdat.db;

import gov.cida.cdat.transform.Transformer;

public class PojoCharacterSeparatorTransformer extends Transformer<Pojo> {
	
	String separator = ",";
	String newline   = "\n";
	boolean firstCall = true;
	
	PojoCharacterSeparatorTransformer(String join) {
		separator = join;
	}
	PojoCharacterSeparatorTransformer(String join, String newline) {
		separator = join;
		this.newline = newline;
	}
	
	@Override
	public byte[] transform(Pojo person) {
		System.out.println("transform person pojo to bytes");
		
		StringBuilder buf = new StringBuilder(200);
		
		if (firstCall) {
			System.out.println("transform adding HEADER");
			firstCall = false;
			createRow(buf, "name", "address", "phone");
		}
		createRow(buf, person.name, person.address, person.phone);
		
		return buf.toString().getBytes();
	}
	
	
	void createRow(StringBuilder buf, String name, String address, String phone) {
		buf.append('"').append(name).append('"').append(separator);
		buf.append('"').append(address).append('"').append(separator);
		buf.append('"').append(phone).append('"').append(newline);
	}
}
