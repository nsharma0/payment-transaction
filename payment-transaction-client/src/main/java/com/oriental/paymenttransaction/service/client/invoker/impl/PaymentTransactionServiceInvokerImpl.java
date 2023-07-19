package com.oriental.paymenttransaction.service.client.invoker.impl;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oriental.paymenttransaction.business.constants.URLMappingConstants;
import com.oriental.paymenttransaction.dto.web.TransactionHistoryRequestDTO;
import com.oriental.paymenttransaction.dto.web.TransactionHistoryResponse;
import com.oriental.paymenttransaction.service.client.exception.PaymentTransactionServiceException;
import com.oriental.paymenttransaction.service.client.invoker.PaymentTransactionServiceInvoker;
/**
 * @author nsharma0
 * 
 */
@Service("paymentTransactionServiceInvoker")
public class PaymentTransactionServiceInvokerImpl implements PaymentTransactionServiceInvoker {
	
	private static Logger logger = LoggerFactory.getLogger(PaymentTransactionServiceInvokerImpl.class);
	
	

	private ObjectMapper objectMapper = new ObjectMapper();
	
	private String endpointurl;
	
	private static Integer MAX_CONNECTION_TIME_OUT =  45000;
	
	public PaymentTransactionServiceInvokerImpl() { 
		super();
	}
 
	private String getEndpointurl() {
		return endpointurl;
	}
	
	public void setEndpointurl(String endpointurl) {
		this.endpointurl = endpointurl;
		logger.info("EndPoint url[{}]"+endpointurl); 
	}
	
	public void setTimeout(int timeout) {
		MAX_CONNECTION_TIME_OUT = timeout;
	}
	
	public static String getJsonReply(String reply) {
		int start = StringUtils.indexOf(reply, "entity");
		int endIndex = StringUtils.indexOf(reply,"entityType");
		String jsonReply = StringUtils.substring(reply,start+8, endIndex -2);
		return jsonReply;
	}
	
	@Override
	public TransactionHistoryResponse getTransactionHistory(TransactionHistoryRequestDTO requestDTO){ 
		String url = getEndpointurl() + URLMappingConstants.SEARCH_TRANSACTION_HISTORY;
		logger.info("url [{}] jsonRequest [{}]", url);
		try {
			HttpEntity<TransactionHistoryRequestDTO> entity = new HttpEntity<TransactionHistoryRequestDTO>(requestDTO);
			long startTime = System.currentTimeMillis();
			RestTemplate client = new RestTemplate();
			String response = client.postForObject(url, entity, String.class);
			logger.info("Execution took " + (System.currentTimeMillis() - startTime) + " ms");
			String jsonReply = getJsonReply(response);
			TransactionHistoryResponse responseDTO = null;
			responseDTO = objectMapper.readValue(jsonReply, TransactionHistoryResponse.class);
			return responseDTO;
		} catch (JsonGenerationException e) {
			logger.error("JsonGenerationException in /paymenttransaction/searchTransactionHistory customerNbr=[{}] Error=[{}]",
					requestDTO.getCustomerNumber(), e);
			throw new PaymentTransactionServiceException("JsonGenerationException in /paymenttransaction/searchTransactionHistory customerNbr["
					+ requestDTO.getCustomerNumber() + "] and Error" + "[{" + e + "}]");
		} catch (JsonMappingException e) {
			logger.error("JsonMappingException in /paymenttransaction/searchTransactionHistory customerNbr=[{}] Error=[{}]"
					+ requestDTO.getCustomerNumber(), e);
			throw new PaymentTransactionServiceException("JsonMappingException in /paymenttransaction/searchTransactionHistory customerNbr["
					+ requestDTO.getCustomerNumber() + "] and Error" + "[{" + e + "}]");
		} catch (IOException e) {
			logger.error("IOException in /paymenttransaction/searchTransactionHistory customerNbr=[{}] Error=[{}]"
					+ requestDTO.getCustomerNumber(), e);
			throw new PaymentTransactionServiceException("IOException in /paymenttransaction/searchTransactionHistory customerNbr["
					+ requestDTO.getCustomerNumber() + "] and Error" + "[{" + e + "}]");
		} catch (Exception e) {
			logger.error("Exception in /paymenttransaction/searchTransactionHistory customerNbr=[{}] Error=[{}]"
					+ requestDTO.getCustomerNumber(),e);
			throw new PaymentTransactionServiceException("IOException in /paymenttransaction/searchTransactionHistory customerNbr["
					+ requestDTO.getCustomerNumber() + "] and Error" + "[{" + e + "}]");
		}
	}
 

}
