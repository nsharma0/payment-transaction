package com.oriental.paymenttransaction.service.client.invoker;

import com.oriental.paymenttransaction.dto.web.TransactionHistoryRequestDTO;
import com.oriental.paymenttransaction.dto.web.TransactionHistoryResponse;

/**
 * @author nsharma0
 *
 */

public interface PaymentTransactionServiceInvoker {
	
	public TransactionHistoryResponse getTransactionHistory(TransactionHistoryRequestDTO requestDTO);
	
}
