package com.oriental.paymenttransaction.enums;

/**
 * @author Santhosh
 * 
 */

// TODO: Need to check how to include 0 in the list and add this to CardAuthForAmountResponse.addressVerificationCode and CardAuthentication.addressVerificationCode
public enum AddressVerificationCodeEnum {
	
		A ("Address Match"),  // Address Match
		B ("Address Matched apart from zip code"),	// Address Matched apart from zip code
		C ("Service Unavailable"),	// Service Unavailable
		D ("Exact Match"),	// Exact Match
		F ("Exact Match for UK transactions"),	// Exact Match for UK transactions
		G ("Version Unavailable. Applies for NON-US issuers"),	// Version Unavailable. Applies for NON-US issuers
		I ("Version Unavailable. AVS info not available"),	// Version Unavailable. AVS info not available
		M ("Exact Match"),	// Exact Match
		N ("NEITHER ADDRESS OR ZIP MATCH"),	// No Match
		P ("ZIP Match"),	// ZIP Match
		R ("Retry"),	// Retry
		S ("Serv Unavailable"),	// Serv Unavailable
		U ("Ver Unavailable"),	// Ver Unavailable
		W ("Zip Match. 9 char zip code"),	// Zip Match. 9 char zip code
		X ("Exact Match. 9 char zip code"),	// Exact Match. 9 char zip code
		Y ("ADDRESS AND ZIP EXACT MATCH"),	// Exact Match. 5 char zip code
		Z ("ZIP Match. 9 char zip code");	// ZIP Match. 9 char zip code
	//0,	// Approved. Address Verification not requested.

		private AddressVerificationCodeEnum(String description) {
			this.description = description;
		}
		
		private String description;

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

}
