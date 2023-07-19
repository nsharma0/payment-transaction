package com.oriental.paymenttransaction.dto.web;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

// Author : nsharma0, 01/27/2020, Transaction History TDD

@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = Include.NON_EMPTY)
public class TransactionHistoryResponse implements Serializable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5445933717242427064L;
	
    @JsonProperty
    private Integer totalPages;

    @JsonProperty
    private Long totalRecords;
	
	private List<TransactionHistoryRecord> txnHistoryRecords;

	public List<TransactionHistoryRecord> getTxnHistoryRecords() {
		return txnHistoryRecords;
	}

	public void setTxnHistoryRecords(List<TransactionHistoryRecord> txnHistoryRecords) {
		this.txnHistoryRecords = txnHistoryRecords;
	}

	public Integer getTotalPages() {
		return totalPages;
	}

	public void setTotalPages(Integer totalPages) {
		this.totalPages = totalPages;
	}

	public Long getTotalRecords() {
		return totalRecords;
	}

	public void setTotalRecords(Long totalRecords) {
		this.totalRecords = totalRecords;
	}

	@Override
	public String toString() {
		return "TransactionHistoryResponse [txnHistoryRecords=" + txnHistoryRecords + "]";
	}
	
}
