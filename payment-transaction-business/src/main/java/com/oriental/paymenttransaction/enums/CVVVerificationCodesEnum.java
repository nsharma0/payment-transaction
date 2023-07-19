package com.oriental.paymenttransaction.enums;

/**
 * @author Santhosh
 * 
 */

public enum CVVVerificationCodesEnum {
	
	M("M"),      // Verification Successful
	P("P"),		// Verification Not performed
	U("U"),		// Verification not available
	N("N"),		// Verification fail/mismatch
	S("S");		// code not present on card
	
	private String value;
	
	private CVVVerificationCodesEnum() {
	}
	
	private CVVVerificationCodesEnum(String value){
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
