package com.oriental.paymentgatewaypayload.dto.web;

import java.io.Serializable;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

// Author : nsharma0, 01/27/2020, Transaction History TDD

@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = Include.NON_EMPTY)
public class TransactionHistoryRecord implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1543842349442827461L;

	@JsonProperty("transactiondate")
	private String transactiondate;
	
	@JsonProperty("transactionTime")
	private String transactionTime;
	
	@JsonProperty("transactionTimeStamp")
	private String transactionTimeStamp;
	
	@JsonProperty("transactionAmount")
	private BigDecimal TransactionAmount;
	
	@JsonProperty("transactionAction")
	private String TransactionAction;
	
	@JsonProperty("creditCardNumber")
	private String creditCardNumber;
	
	@JsonProperty("transactionType")
	private String transactionType;
	
	@JsonProperty("authCode")
	private String authCode;
	
	@JsonProperty("expirationDate")
	private String expirationDate;
	
	@JsonProperty("customerNumber")
	private Long customerNumber;
	
	@JsonProperty("orderNumber")
	private Integer orderNumber;
	
	@JsonProperty("releaseNumber")
	private Integer releaseNumber;
	
	@JsonProperty("businessUnit")
	private Integer businessUnit;
	
	@JsonProperty("cardHolderName")
	private String cardholderName;
	
	@JsonProperty("sourceOfentry")
	private String sourceOfentry;
	
	@JsonProperty("avsCode")
	private String avsCode;
	
	@JsonProperty("returnCode")
	private String returnCode;
	
	@JsonProperty("returnCodeDescription")
	private String returnCodeDescription;
	
	@JsonProperty("token")
	private String token;
	
	@JsonProperty("paymentMethod")
	private String paymentMethod;
	
	private String txnDateString;
	private String buString;
	
	private String agentId;
	
	//nbhutani	
	@JsonProperty("transactionId")
	private String transactionId;

	public String getTransactiondate() {
		return transactiondate;
	}

	public void setTransactiondate(String transactiondate) {
		this.transactiondate = transactiondate;
	}

	public String getTxnDateString() {
		return txnDateString;
	}

	public void setTxnDateString(String txnDateString) {
		this.txnDateString = txnDateString;
	}

	public String getTransactionTime() {
		return transactionTime;
	}

	public void setTransactionTime(String transactionTime) {
		this.transactionTime = transactionTime;
	}

	public BigDecimal getTransactionAmount() {
		return TransactionAmount;
	}

	public void setTransactionAmount(BigDecimal transactionAmount) {
		TransactionAmount = transactionAmount;
	}

	public String getTransactionAction() {
		return TransactionAction;
	}

	public void setTransactionAction(String transactionAction) {
		TransactionAction = transactionAction;
	}

	public String getCreditCardNumber() {
		return creditCardNumber;
	}

	public void setCreditCardNumber(String creditCardNumber) {
		this.creditCardNumber = creditCardNumber;
	}

	public String getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}

	public String getAuthCode() {
		return authCode;
	}

	public void setAuthCode(String authCode) {
		this.authCode = authCode;
	}

	public String getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(String expirationDate) {
		this.expirationDate = expirationDate;
	}

	public Long getCustomerNumber() {
		return customerNumber;
	}

	public void setCustomerNumber(Long customerNumber) {
		this.customerNumber = customerNumber;
	}

	public Integer getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(Integer orderNumber) {
		this.orderNumber = orderNumber;
	}

	public Integer getReleaseNumber() {
		return releaseNumber;
	}

	public void setReleaseNumber(Integer releaseNumber) {
		this.releaseNumber = releaseNumber;
	}

	public Integer getBusinessUnit() {
		return businessUnit;
	}

	public void setBusinessUnit(Integer businessUnit) {
		this.businessUnit = businessUnit;
	}

	public String getBuString() {
		return buString;
	}

	public void setBuString(String buString) {
		this.buString = buString;
	}

	public String getCardholderName() {
		return cardholderName;
	}

	public void setCardholderName(String cardholderName) {
		this.cardholderName = cardholderName;
	}

	public String getSourceOfentry() {
		return sourceOfentry;
	}

	public void setSourceOfentry(String sourceOfentry) {
		this.sourceOfentry = sourceOfentry;
	}

	public String getAvsCode() {
		return avsCode;
	}

	public void setAvsCode(String avsCode) {
		this.avsCode = avsCode;
	}

	public String getReturnCode() {
		return returnCode;
	}

	public void setReturnCode(String returnCode) {
		this.returnCode = returnCode;
	}

	public String getReturnCodeDescription() {
		return returnCodeDescription;
	}

	public void setReturnCodeDescription(String returnCodeDescription) {
		this.returnCodeDescription = returnCodeDescription;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getTransactionTimeStamp() {
		return transactionTimeStamp;
	}

	public void setTransactionTimeStamp(String transactionTimeStamp) {
		this.transactionTimeStamp = transactionTimeStamp;
	}
	
	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}
	
	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}
	

	public String getTransactionId() {

		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	@Override
	public String toString() {
		return "TransactionHistoryRecord [transactiondate=" + transactiondate + ", transactionTime=" + transactionTime
				+ ", transactionTimeStamp=" + transactionTimeStamp + ", TransactionAmount=" + TransactionAmount
				+ ", TransactionAction=" + TransactionAction + ", creditCardNumber=" + creditCardNumber
				+ ", transactionType=" + transactionType + ", authCode=" + authCode + ", expirationDate="
				+ expirationDate + ", customerNumber=" + customerNumber + ", orderNumber=" + orderNumber
				+ ", releaseNumber=" + releaseNumber + ", businessUnit=" + businessUnit + ", cardholderName="
				+ cardholderName + ", sourceOfentry=" + sourceOfentry + ", avsCode=" + avsCode + ", returnCode="
				+ returnCode + ", returnCodeDescription=" + returnCodeDescription + ", token=" + token + "]";
	}


}
