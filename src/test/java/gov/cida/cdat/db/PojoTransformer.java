package gov.cida.cdat.db;

import gov.cida.cdat.transform.Transformer;

public class PojoTransformer extends Transformer {
	
	private String  separator = ",";
	private String  newline   = "\n";
	private boolean firstCall = true;
	
	
	PojoTransformer(String join) {
		separator = join;
	}
	PojoTransformer(String join, String newline) {
		separator = join;
		this.newline = newline;
	}
	
	
	@Override
	public byte[] transform(Object obj) {
		if ( ! (obj instanceof  Pojo) ) {
			return new byte[0];
		}
		Pojo person = (Pojo)obj;
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
		// Here be your favorite CSV framework implementation
		
		buf.append('"').append(name).append('"').append(separator);
		buf.append('"').append(address).append('"').append(separator);
		buf.append('"').append(phone).append('"').append(newline);
	}
}
