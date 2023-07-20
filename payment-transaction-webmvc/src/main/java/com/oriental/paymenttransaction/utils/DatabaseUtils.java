package com.oriental.paymenttransaction.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oriental.comet.db2.customer.dto.AccountTransactionHistoryDBResponse;
import com.oriental.comet.db2.dao.PaymentService;
import com.oriental.comet.db2.payment.entity.FinanceCheckBatch;
import com.oriental.paymenttransaction.dto.web.TransactionDetailRequestDTO;
import com.oriental.paymenttransaction.dto.web.TransactionHistoryRecord;
import com.oriental.paymenttransaction.dto.web.TransactionHistoryRequestDTO;
import com.oriental.paymenttransaction.dto.web.TransactionHistoryResponse;
import com.oriental.paymenttransaction.enums.AddressVerificationCodeEnum;

@Service
public class DatabaseUtils {

	private static Logger logger = LoggerFactory.getLogger(DatabaseUtils.class);

	@Autowired
	private PaymentService paymentService;

	private ObjectMapper objMapper = new ObjectMapper();

	/**
	 * Author : nsharma0, 01/27/2020, Transaction History TDD
	 * @param requestDTO
	 * @return TransactionHistoryResponse
	 */
	public TransactionHistoryResponse getTransactionHistory(TransactionHistoryRequestDTO requestDTO) throws Exception {
		// TODO Auto-generated method stub
		if(StringUtils.isNotBlank(requestDTO.getCreditCardNumber())){
			requestDTO.setCreditCardNumber(null);
		}
		TransactionHistoryResponse response = new TransactionHistoryResponse();
		AccountTransactionHistoryDBResponse dbResponse = null;
		if(null != requestDTO.getOrderNumber() && null != requestDTO.getCustomerNumber()){
			dbResponse = paymentService.getTransactionHistoryByOrderNumber(requestDTO.getOrderNumber().intValue(), requestDTO.getCustomerNumber().intValue(), requestDTO.getPageNumber().intValue());
		} else if(StringUtils.isNotBlank(requestDTO.getCreditCardNumber()) && null != requestDTO.getCustomerNumber()){
			dbResponse = paymentService.getTransactionHistoryByCCToken(requestDTO.getCreditCardNumber(), requestDTO.getCustomerNumber().intValue(), requestDTO.getPageNumber().intValue());
		}else if(null != requestDTO.getCustomerNumber()){
			dbResponse = paymentService.getTransactionHistoryByCustomerNumber(requestDTO.getCustomerNumber().intValue(), requestDTO.getPageNumber().intValue());
		} else if (null != requestDTO.getOrderNumber() && null == requestDTO.getCustomerNumber()) {//comet@web, nsharma0, transaction history with global order search without assume identity
			dbResponse = paymentService.getTransactionHistoryByOrderNumber(requestDTO.getOrderNumber().intValue(), null, requestDTO.getPageNumber().intValue());
		}
		if (null != dbResponse.getTxnHistoryRecords()){
			response = populateTransactionHistoryResponse(dbResponse.getTxnHistoryRecords());
			response.setTotalPages(dbResponse.getTotalPages());
			response.setTotalRecords(dbResponse.getTotalRecords());
		}
		return response;
	}
		
	private TransactionHistoryResponse populateTransactionHistoryResponse(List<Object[]> list) {
		TransactionHistoryResponse response = new TransactionHistoryResponse();
		
		List<TransactionHistoryRecord> entityList = new ArrayList<TransactionHistoryRecord>();
		TransactionHistoryRecord txnHistoryDetail = null;
		for (Object[] objectRow : list) {
			if(null != objectRow[1] && "RESPONSE".equals(objectRow[1].toString().trim())) {
			txnHistoryDetail = convertToTransactionHistoryDetail(objectRow);
			entityList.add(txnHistoryDetail);
			}
		}
		response.setTxnHistoryRecords(entityList);
		return response;
	}

	private TransactionHistoryRecord convertToTransactionHistoryDetail(Object[] objectRow) {
		TransactionHistoryRecord txnHistoryDetail;
		txnHistoryDetail = new TransactionHistoryRecord();
		txnHistoryDetail.setPaymentMethod(null != objectRow[0] ? objectRow[0].toString().trim() : null);
		txnHistoryDetail.setTransactiondate(null != objectRow[10] ? objectRow[10].toString().trim() : null);
		txnHistoryDetail.setTransactionAmount(null != objectRow[9] ? (BigDecimal)objectRow[9] : null);
		txnHistoryDetail.setTransactionAction(null != objectRow[12] ? processREqOp(objectRow[12].toString().trim()) : "");
		txnHistoryDetail.setCreditCardNumber(null != objectRow[26] ? objectRow[26].toString().trim() : "");
		txnHistoryDetail.setTransactionType(null != objectRow[18] ? objectRow[18].toString().trim() : "");
		txnHistoryDetail.setAuthCode(null != objectRow[8] ? objectRow[8].toString().trim() : "");
		txnHistoryDetail.setExpirationDate(null != objectRow[7] ? objectRow[7].toString().trim() : "");
		txnHistoryDetail.setCustomerNumber(null != objectRow[3] ? ((BigDecimal)objectRow[3]).longValue() : null);
		txnHistoryDetail.setOrderNumber(null != objectRow[4] ? ((BigDecimal)objectRow[4]).intValue() : null);
		txnHistoryDetail.setReleaseNumber(null != objectRow[17] ? ((BigDecimal)objectRow[17]).intValue() : null);
		txnHistoryDetail.setBusinessUnit(null != objectRow[2] ? ((BigDecimal)objectRow[2]).intValue() : null);
		txnHistoryDetail.setCardholderName(null != objectRow[5] ? WordUtils.capitalizeFully(objectRow[5].toString().trim()) : "");
		txnHistoryDetail.setTransactionTime(null != objectRow[11] ? objectRow[11].toString().trim() : "");
		txnHistoryDetail.setSourceOfentry(null != objectRow[20] ? objectRow[20].toString().trim() : "");
		txnHistoryDetail.setTransactionTimeStamp(null != objectRow[21] ? objectRow[21].toString().trim() : "");
		//nbhutani
		txnHistoryDetail.setTransactionId(null != objectRow[19] ? objectRow[19].toString().trim() : "");
		try {
			txnHistoryDetail.setAvsCode(null != objectRow[16] ? AddressVerificationCodeEnum.valueOf(objectRow[16].toString().trim()).getDescription() : "");
		} catch (Exception ex) {
			txnHistoryDetail.setAvsCode("");
		}
		
		//nbhutani HPQC fix 473 start
		String pgTransactionDescription="";
		if(null != objectRow[24]){
		pgTransactionDescription= objectRow[24].toString().trim();
		}
		String PG_STATUS="FAIL";
		String RETURN_CODE="05";
		String RETURN_CODE_DESCRIPTION="SYSTEM DOWN";
		//mmuppidathy To display the Return and Transaction Description as System down when the Paymentgateway status is fail.
		if(pgTransactionDescription.equals(PG_STATUS)){
		txnHistoryDetail.setReturnCode(RETURN_CODE);
		txnHistoryDetail.setReturnCodeDescription(RETURN_CODE_DESCRIPTION);
		}else{
		txnHistoryDetail.setReturnCode(null != objectRow[13] ? objectRow[13].toString().trim() : "");
		txnHistoryDetail.setReturnCodeDescription(null != objectRow[15] ? objectRow[15].toString().trim() : "");
		}
		//txnHistoryDetail.setReturnCodeDescription(null != objectRow[15] ? objectRow[15].toString().trim() : "");
		//txnHistoryDetail.setReturnCode(null != objectRow[13] ? objectRow[13].toString().trim() : "");
		//nbhutani HPQC fix 473 end
		
		txnHistoryDetail.setToken(null != objectRow[6] ? objectRow[6].toString().trim() : "");
		return txnHistoryDetail;
	}
	
	private String processREqOp (String action) {
		if ("AUTH".equalsIgnoreCase(action))
			action = "AUTHORIZE";
		action = WordUtils.capitalizeFully(action);
		return action;
	}
	
	/**
	 * Author : nsharma0, 01/27/2020, Transaction History TDD
	 * @param requestDTO
	 * @return TransactionHistoryResponse
	 */
	public TransactionHistoryResponse getTransactionDetails(TransactionDetailRequestDTO requestDTO) {
		// TODO Auto-generated method stub
		TransactionHistoryResponse response = new TransactionHistoryResponse();
		List<Object[]> transactionHistoryList = null;
		if(null != requestDTO && StringUtils.isNotBlank(requestDTO.getTimeStamp())){
			transactionHistoryList = paymentService.getTransactionDetails(requestDTO.getTimeStamp());
		}
		if (null != transactionHistoryList){
			response = populateTransactionDetailResponse(transactionHistoryList);
		}
		return response;
	}
	
	private TransactionHistoryResponse populateTransactionDetailResponse(List<Object[]> list) {
		TransactionHistoryResponse response = new TransactionHistoryResponse();
		
		List<TransactionHistoryRecord> entityList = new ArrayList<TransactionHistoryRecord>();
		TransactionHistoryRecord txnHistoryDetail = null;
		if(!list.isEmpty()){
			Object[] objectRow = list.get(0);
			txnHistoryDetail = new TransactionHistoryRecord();
			txnHistoryDetail.setCardholderName(null != objectRow[0] ? objectRow[0].toString().trim() : "");
			txnHistoryDetail.setSourceOfentry(null != objectRow[1] ? objectRow[1].toString().trim() : "");
			txnHistoryDetail.setTransactionTime(null != objectRow[2] ? objectRow[2].toString().trim() : "");
			//txnHistoryDetail.setReturnCodeDescription(null != objectRow[3] ? objectRow[3].toString().trim() : "");
			txnHistoryDetail.setAvsCode(null != objectRow[4] ? objectRow[4].toString().trim() : "");
			txnHistoryDetail.setToken(null != objectRow[5] ? objectRow[5].toString().trim() : "");
			//txnHistoryDetail.setReturnCode(null != objectRow[6] ? objectRow[6].toString().trim() : "");
			
			//nbhutani HPQC fix 473 start
			String pgTransactionDescription="";
			if(null != objectRow[7]){
			pgTransactionDescription= objectRow[7].toString().trim();
			}
			String PG_STATUS="FAIL";
			String RETURN_CODE="05";
			String RETURN_CODE_DESCRIPTION="SYSTEM DOWN";
			//mmuppidathy To display the Return and Transaction Description as System down when the Paymentgateway status is fail.
			if(pgTransactionDescription.equals(PG_STATUS)){
			txnHistoryDetail.setReturnCode(RETURN_CODE);
			txnHistoryDetail.setReturnCodeDescription(RETURN_CODE_DESCRIPTION);
			}else{
			txnHistoryDetail.setReturnCode(null != objectRow[6] ? objectRow[6].toString().trim() : "");
			txnHistoryDetail.setReturnCodeDescription(null != objectRow[3] ? objectRow[3].toString().trim() : "");
			}
			//nbhutani HPQC fix 473 end
			entityList.add(txnHistoryDetail);
		}
		response.setTxnHistoryRecords(entityList);
		return response;
	}
	
	/**
	 * nsharma0, 09/25/2020, search by cc token
	 * */
	public List<Long> searchCustomerByToken(String token) {
		logger.info("> In DatabaseUtils.searchCustomerByToken with ordnbr");
		List<Long> customerNums  = paymentService.searchCustomerByToken(token);
		return customerNums;
	}

	/**
	 * Author : nbhutani
	 * @param requestDTO
	 * @return TransactionHistoryResponse
	 */
	public TransactionHistoryResponse findPGAuditInfoByOrderNbr(TransactionDetailRequestDTO requestDTO) {
		// TODO Auto-generated method stub
		TransactionHistoryResponse response = new TransactionHistoryResponse();
		List<Object[]> transactionHistoryList = null;
		if(null != requestDTO && null !=requestDTO.getOrderNumber()){
			transactionHistoryList = paymentService.findPGAuditInfoByOrderNbr(requestDTO.getOrderNumber());
		}
		if (null != transactionHistoryList){
			response = populatePGAuditResponse(transactionHistoryList);
		}
		return response;
	}
	private TransactionHistoryResponse populatePGAuditResponse(List<Object[]> list) {
		TransactionHistoryResponse response = new TransactionHistoryResponse();
		
		List<TransactionHistoryRecord> entityList = new ArrayList<TransactionHistoryRecord>();
		TransactionHistoryRecord txnHistoryDetail = null;
		for (Object[] objectRow : list) {
			if(null != objectRow) {
			txnHistoryDetail = convertToTransactionHistoryDetail(objectRow);
			entityList.add(txnHistoryDetail);
			}
		}
		response.setTxnHistoryRecords(entityList);
		return response;
	}
}
