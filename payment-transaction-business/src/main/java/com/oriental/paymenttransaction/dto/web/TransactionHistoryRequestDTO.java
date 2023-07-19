package com.oriental.paymenttransaction.dto.web;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

//Author : nsharma0, 01/27/2020, Transaction History TDD

@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = Include.NON_EMPTY)
public class TransactionHistoryRequestDTO implements Serializable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 866633356329476985L;
	
	@JsonProperty("customerNumber")
	private Long customerNumber;
	@JsonProperty("orderNumber")
	private Long orderNumber;
	@JsonProperty("creditCardNumber")
	private String creditCardNumber;
	@JsonProperty("creditCardExpDate")
	private String creditCardExpDate;
	@JsonProperty
    private Integer pageNumber;
		
	public Long getCustomerNumber() {
		return customerNumber;
	}
	
	public void setCustomerNumber(Long customerNumber) {
		this.customerNumber = customerNumber;
	}
	public Long getOrderNumber() {
		return orderNumber;
	}
	public void setOrderNumber(Long orderNumber) {
		this.orderNumber = orderNumber;
	}
	public String getCreditCardNumber() {
		return creditCardNumber;
	}
	public void setCreditCardNumber(String creditCardNumber) {
		this.creditCardNumber = creditCardNumber;
	}
	

	public String getCreditCardExpDate() {
		if(null == this.creditCardExpDate) {
			this.creditCardExpDate = "";
		}
		return creditCardExpDate;
	}

	public void setCreditCardExpDate(String creditCardExpDate) {
		this.creditCardExpDate = creditCardExpDate;
	}

	public Integer getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(Integer pageNumber) {
		this.pageNumber = pageNumber;
	}

	@Override
	public String toString() {
		return "TransactionHistoryRequestDTO [customerNumber=" + customerNumber + ", orderNumber=" + orderNumber
				+ ", creditCardNumber=" + creditCardNumber + "]";
	}

}