package com.oriental.paymenttransaction.dto.web;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = Include.NON_EMPTY)
public class TransactionDetailRequestDTO implements Serializable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2932122840601158597L;
	/**
	 * 
	 */
	
	@JsonProperty("timeStamp")
	private String timeStamp;

	@JsonProperty("orderNumber")
	private Integer orderNumber;
	
	public String getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}

	public Integer getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(Integer orderNumber) {
		this.orderNumber = orderNumber;
	}

	@Override
	public String toString() {
		return "TransactionDetailRequestDTO [timeStamp=" + timeStamp +"]";
	}

}
