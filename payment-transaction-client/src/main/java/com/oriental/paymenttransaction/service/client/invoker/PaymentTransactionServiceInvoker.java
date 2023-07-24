package com.oriental.paymenttransaction.service.client.invoker;

import java.util.List;

import com.oriental.paymenttransaction.dto.web.TransactionDetailRequestDTO;
import com.oriental.paymenttransaction.dto.web.TransactionHistoryRequestDTO;
import com.oriental.paymenttransaction.dto.web.TransactionHistoryResponse;

/**
 * @author nsharma0
 *
 */

public interface PaymentTransactionServiceInvoker {
	
	public void setEndpointurl(String endpointurl);
	
	public TransactionHistoryResponse getTransactionHistory(TransactionHistoryRequestDTO requestDTO);
	
	public TransactionHistoryResponse getTransactionDetails(TransactionDetailRequestDTO requestDTO);
	
	public List<Long> searchCustomerByCC(String queryBy);
	
	public TransactionHistoryResponse findPGAuditInfoByOrderNbr(TransactionDetailRequestDTO requestDTO);
	
}
