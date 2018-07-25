package com.gillsoft.client;

import java.util.HashMap;
import java.util.Map;

public enum IdentityDocument {
	/*
		"03"    => "Passport of foreign citizen"
        "00"    => "Passport of the Russian Federation citizen"
        "01"    => "Seaman's Passport"
        "02"    => "Foreign passport of the Russian Federation citizen"
        "04"    => "Birth certificate"
        "05"    => "Identity card of military man"
        "06"    => "Identity card of person without citizenship"
        "07"    => "Temporary identity card"
        "08"    => "Military registration card"
        "09"    => "Residence permit of foreign citizen or person without citizenship"
        "10"    => "Certificate of release from prison"
        "11"    => "Passport of the USSR citizen"
        "12"    => "Diplomatic passport"
        "13"    => "Service passport (except seaman's and diplomatic passport)"
        "14"    => "Certificate of return from CIS countries"
        "15"    => "Certificate of passport loss"
        "16"    => "Deputy identity card"
        "99"    => "Other types of documents"

	 */
	
	PASSPORT(1, "00"),
	
	MILITARY_ID(2, "08"),
	
	FOREIGN_DOCUMENT(3, "03"),
	
	FOREIGN_PASSPORT(4, "02"),
	
	SEAMAN(6, "01"),
	
	BIRTH_CERTIFICATE(7, "04"),
	
	SSSR_PASSPORT(8, "11");
	
	private int id;
	private String resourceDocId;
	
	private IdentityDocument(int id, String resourceDocId) {

		this.id = id;
		this.resourceDocId = resourceDocId;
		
	}
	
	private static Map<Integer, IdentityDocument> types = new HashMap<>();
	
	static {
		for (IdentityDocument item : IdentityDocument.values()) {
			types.put(item.id, item);
		}
	}
	
	public static IdentityDocument getResourceDocId(Integer id) {
		return types.get(id);
	}

	public String getResourceDocId() {
		return resourceDocId;
	}
	
}
