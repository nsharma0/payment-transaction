package com.oriental.paymenttransaction.service.client.exception;
 

public class PaymentTransactionServiceException extends RuntimeException{
 

	/**
	 * 
	 */
	private static final long serialVersionUID = -7283139934639065773L;

	public PaymentTransactionServiceException() {
	}

	public PaymentTransactionServiceException(String message) {
		super(message);
	}

	public PaymentTransactionServiceException(Throwable cause) {
		super(cause);
	}

	public PaymentTransactionServiceException(String message, Throwable cause) {
		super(message, cause);
	}
	
	@Override 
	public String toString() { 
		return super.toString(); 
	}
}
