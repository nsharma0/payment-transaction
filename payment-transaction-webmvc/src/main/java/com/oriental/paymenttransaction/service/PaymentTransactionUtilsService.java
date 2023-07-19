package com.oriental.paymenttransaction.service;

import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.oriental.paymenttransaction.dto.web.TransactionDetailRequestDTO;
import com.oriental.paymenttransaction.dto.web.TransactionHistoryRequestDTO;
import com.oriental.paymenttransaction.dto.web.TransactionHistoryResponse;
import com.oriental.paymenttransaction.utils.DatabaseUtils;

/**
 * @author Santhosh
 * 
 */
@Service
public class PaymentTransactionUtilsService {

	private static Logger logger = LoggerFactory.getLogger(PaymentTransactionUtilsService.class);
	
	private static SimpleDateFormat transactionTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private DatabaseUtils dbUtils;


	public DatabaseUtils getDbUtils() {
		return dbUtils;
	}

	public void setDbUtils(DatabaseUtils dbUtils) {
		this.dbUtils = dbUtils;
	}


	
	/**
	 * Author : nsharma0, 01/27/2020, Transaction History TDD
	 * @param requestDTO
	 * @return TransactionHistoryResponse
	 * @throws PaymentGatewayException
	 */
	
	public TransactionHistoryResponse getTransactionHistory(TransactionHistoryRequestDTO requestDTO) throws Exception {
		logger.info("> In PaymentUtilService.getTransactionHistory()");
		TransactionHistoryResponse response = new TransactionHistoryResponse();
		if(StringUtils.isNotBlank(requestDTO.getCreditCardNumber())){
			requestDTO.setCreditCardNumber(null);
		}
		response = dbUtils.getTransactionHistory(requestDTO);
		logger.info("< In PaymentUtilService.getTransactionHistory()");
		return response;
	}
	
	
	
	private boolean isNumeric(String s){
	    String pattern= "\\d+";
	    return s.matches(pattern);
	}
	
	
	/**
	 * Author : nsharma0, 01/27/2020, Transaction History TDD
	 * @param requestDTO
	 * @return TransactionHistoryResponse
	 * @throws PaymentGatewayException
	 */
	public TransactionHistoryResponse getTransactionDetails(TransactionDetailRequestDTO requestDTO) throws Exception {
		logger.info("> In PaymentUtilService.getTransactionDetails()");
		TransactionHistoryResponse response = new TransactionHistoryResponse();
		response = dbUtils.getTransactionDetails(requestDTO);
		logger.info("< In PaymentUtilService.getTransactionDetails()");
		return response;
	}
	
	
	/**
	 * nsharma0, 09/25/2020, search by cc token
	 * */
	public List<Long> searchCustomerByCC(String queryBy) throws Exception {
		logger.info("> In PaymentUtilService.searchCustomerByCC()");
		List<Long> response = null;
		String token = null;
		/*if(StringUtils.isNotBlank(queryBy)){
			if(isNumeric(queryBy)){
				PaymentResponse paymentResponse = null;
				PaymentRequest paymentRequest = buildCCPaymentRequest(queryBy, "");
				Calendar startTimeCal = Calendar.getInstance();
				try {
				paymentResponse = paymentGatewayService.processRequest(paymentRequest, startTimeCal);
				} catch (Exception ex) {
					ex.getMessage();
				}
				token = paymentResponse.getCcResponse().getCcTokenNumber();
			} else {
				token = queryBy;
			}
		}*/
		response = dbUtils.searchCustomerByToken(token);
		logger.info("< In PaymentUtilService.searchCustomerByCC()");
		return response;
	}
	/**
	 * Author : nbhutani
	 * @param requestDTO
	 * @return TransactionHistoryResponse
	 * @throws PaymentGatewayException
	 */
	public TransactionHistoryResponse findPGAuditInfoByOrderNbr(TransactionDetailRequestDTO requestDTO) throws Exception {
		logger.info("> In PaymentUtilService.findPGAuditInfoByOrderNbr()");
		TransactionHistoryResponse response = new TransactionHistoryResponse();
		response = dbUtils.findPGAuditInfoByOrderNbr(requestDTO);
		logger.info("< In PaymentUtilService.findPGAuditInfoByOrderNbr()");
		return response;
	}
}
