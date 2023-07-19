package com.oriental.paymenttransaction.enums;

/**
 * @author Santhosh
 * 
 */

public enum CardTypeEnum {
	
	V("V"),	//VISA CARD
	M("M"),	//MASTER CARD
	R("R"),	//DISCOVER CARD
	X("X"),	//AMERICAN EXPRESS CARD
	J("J"),	//JCB CARD
	I("I"),	//DINERS CLUB CARD
	D("D"),	//DEBIT CARD
	A("A"),	//ATM CARD
	G("G"),	//PRIVATE LABEL GIFTCARD
	E("E"),	//EBT CARD
	U("U");	//UNKNOWN CARD
	
	private String value;

	private CardTypeEnum() {
	}

	private CardTypeEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
	
	
	
}
