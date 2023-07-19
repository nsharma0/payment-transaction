package com.oriental.paymenttransaction.enums;

/**
 * @author Santhosh
 * 
 */

public enum CardDataSourceEnum {
	
	SWIPE,
	NFC,
	EMV,
	EMV_CONTACTLESS,
	FALLBACK_SWIPE,
	BAR_CODE,
	MANUAL("MANUAL"),
	PHONE("PHONE"),
	MAIL,
	INTERNET("INTERNET");
	
	private String value;

	private CardDataSourceEnum() {
	}

	private CardDataSourceEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
	
}
