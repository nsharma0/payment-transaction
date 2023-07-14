package com.oriental.paymentgateway.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oriental.comet.db2.payment.entity.FinanceCheckBatch;
import com.oriental.edi.common.utilities.ObfuscationUtility;
import com.oriental.paymentgateway.controller.PAYMENTGATEWAYController;
import com.oriental.paymentgateway.exceptions.PaymentGatewayException;
import com.oriental.paymentgateway.utils.CommonConstants;
import com.oriental.paymentgateway.utils.DatabaseUtils;
import com.oriental.paymentgatewaypayload.dto.web.CCInfo;
import com.oriental.paymentgatewaypayload.dto.web.CheckPaymentDTO;
import com.oriental.paymentgatewaypayload.dto.web.OrderInfo;
import com.oriental.paymentgatewaypayload.dto.web.PaymentRequest;
import com.oriental.paymentgatewaypayload.dto.web.PaymentResponse;
import com.oriental.paymentgatewaypayload.dto.web.PaymentToken;
import com.oriental.paymentgatewaypayload.dto.web.PendingPayments;
import com.oriental.paymentgatewaypayload.dto.web.TransactionDetailRequestDTO;
import com.oriental.paymentgatewaypayload.dto.web.TransactionHistoryRequestDTO;
import com.oriental.paymentgatewaypayload.dto.web.TransactionHistoryResponse;
import com.oriental.paymentgatewaypayload.enums.RequesterApps;
import com.oriental.paymentgatewaypayload.utils.BusinessConstants;

/**
 * @author Santhosh
 * 
 */
@Service
public class PaymentUtilsService {

	private static Logger logger = LoggerFactory.getLogger(PaymentUtilsService.class);
	
	private static SimpleDateFormat transactionTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	//blenagh 4/28/2023 - solve circular dependency
	private DatabaseUtils dbUtils;

	@Autowired
	private PaymentGatewayService paymentGatewayService;

	public DatabaseUtils getDbUtils() {
		return dbUtils;
	}

	public void setDbUtils(DatabaseUtils dbUtils) {
		this.dbUtils = dbUtils;
	}


	public String savePendingPayments(PendingPayments pendingPaymentsRequest) throws PaymentGatewayException {
		logger.info("> In PaymentUtilService.savePendingPayments start");

		String status = dbUtils.saveToPendingPayments(pendingPaymentsRequest);

		logger.info("< In PaymentUtilService.savePendingPayments end");
		return status;
	}

	/**
	 * @author SAdiraju 6/23/2016
	 * 
	 *         call to update PGWTRAADT table with the customer number from the
	 *         request.
	 */
	public String saveCustomerNumberToAuditTrail(PaymentToken paymentTokenRequest) throws PaymentGatewayException {
		logger.info("> In PaymentUtilService.saveCustomerNumberToAuditTrail start");

		String status = dbUtils.saveCustomerNumberToAuditTrail(paymentTokenRequest);

		logger.info("< In PaymentUtilService.saveCustomerNumberToAuditTrail end");
		return status;
	}

	public String deletePendingPayments(PendingPayments pendingPaymentsRequest) throws PaymentGatewayException {
		logger.info("> In PaymentUtilService.deletePendingPayments start");

		String status = dbUtils.deletePendingPayments(pendingPaymentsRequest);

		logger.info("< In PaymentUtilService.deletePendingPayments end");
		return status;
	}

	public PaymentToken getTokenInfo(String token, BigDecimal businessUnitNbr) {
		logger.info("> In PaymentUtilService.findTokenByBU start");

		PaymentToken tokenDto = dbUtils.findTokenByBU(token, businessUnitNbr);

		logger.info("< In PaymentUtilService.findTokenByBU end");
		return tokenDto;
	}

	//sadiraju 01/26/2017
	//this method uses our obfuscation util and returns an encrypted string
	//this method will be exposed to the web team to encrypt any info ex: cc
	public String getEncryptedData(String data) {
		logger.info("In PaymentUtilService.getEncryptedData start");
		String encryptedString="";
		if (!data.isEmpty()) {
			encryptedString = ObfuscationUtility.obfuscate(data);
		}
		logger.info("< In PaymentUtilService.getEncryptedData end");
		return encryptedString;
	}
	
	public String getSwitchStatus(String businessUnit, String paymentType) {
		logger.info("> In PaymentUtilService.getSwitchStatus start");
		String switchStatus = CommonConstants.OFF;
		Map<String, String> buProperties = PAYMENTGATEWAYController.buProperties;
		if (null == businessUnit && null == paymentType) {
			Set<String> keySet = buProperties.keySet();
			for (String key : keySet) {
				if (buProperties.get(key).equalsIgnoreCase(CommonConstants.ON)) {
					switchStatus = CommonConstants.ON;
					break;
				}
			}
		} else {
			if (paymentType.equalsIgnoreCase(com.oriental.paymentgatewaypayload.utils.BusinessConstants.PAYPAL))
				switchStatus = buProperties.get(businessUnit + "-" + CommonConstants.PAYPAL_SWITCH);
			else if (paymentType.equalsIgnoreCase(com.oriental.paymentgatewaypayload.utils.BusinessConstants.CREDIT_CARD))
				switchStatus = buProperties.get(businessUnit + "-" + CommonConstants.TSYS_SWITCH);
			else
				switchStatus = CommonConstants.INVALID_REQUEST;
		}
		logger.info("> In PaymentUtilService.getSwitchStatus start");
		return switchStatus;
	}

	public List<Integer> getAllBus() {
		logger.info("> In PaymentUtilService.getAllBus start");
		List<Integer> buList = dbUtils.findAllBUs();
		logger.info("> In PaymentUtilService.getAllBus start");
		return buList;
	}

	public String findAuthStatus(PaymentToken tokenRequest) {
		logger.info("> In PaymentUtilService.findAuthStatus()");
		String result = dbUtils.findAuthStatus(tokenRequest);
		logger.info("< In PaymentUtilService.findAuthStatus()");
		return result;
	}
	//mmuppidathy Added to check the PaypalAuthStatus 
	public String findPayPalAuthStatus(PaymentToken tokenRequest) {
		logger.info("> In PaymentUtilService.findPaypalAuthStatus()");
		String result = dbUtils.findPayPalAuthStatus(tokenRequest);
		logger.info("< In PaymentUtilService.findPaypalAuthStatus()");
		return result;
	}
	
//sadiraju 07/11/2016 updated method to include check for payment method and call appropriate methods
	public List<PaymentToken> findPaymentInfo(PaymentToken request) throws PaymentGatewayException {
		logger.info("> In PaymentUtilService.findPaymentInfo()");
		List<PaymentToken> paymentInfo = null;
		if (!request.getPaymentMethod().equals(com.oriental.paymentgatewaypayload.utils.BusinessConstants.PAYPAL)
				&& !request.getPaymentMethod().equals(com.oriental.paymentgatewaypayload.utils.BusinessConstants.PAYPAL_CREDIT)) {
			if (null != request.getCustomerNbr())
				paymentInfo = dbUtils.findPaymentInfoByCusNbr(request.getCustomerNbr());
			else if (null != request.getOrderNbr())
				paymentInfo = dbUtils.findPaymentInfoByOrderNbr(request.getOrderNbr());
			else if (null != request.getToken())
				paymentInfo = dbUtils.findPaymentInfoByToken(request.getToken());
			else
				logger.warn(
						"Not able to get paymentInformation. One of the fields customerNbr/OrderNbr/token are mandatory for this call");
		}
		else{
			if (null != request.getOrderNbr())
				paymentInfo = dbUtils.findPaypalPaymentInfoByOrderNbr(request.getOrderNbr());
		}
		logger.info("< In PaymentUtilService.findPaymentInfo()");
		return paymentInfo;
	}

	
	/**
	 * @author gsrinivas 01/08/2019 Breakfix INC0095773
	 * 
	 * This method retrieves order information for RECTYP = REQUEST
	 * and payment method CC / PP / PPC / GIFT_CARD to void the AUTH
	 * 
	 * @param tokenRequest
	 * @return tokenResponse
	 */
	public PaymentToken getPaymentDetails(PaymentToken tokenRequest) throws PaymentGatewayException {
		logger.info("> In PaymentUtilService.getPaymentDetails()");
		PaymentToken tokenResponse = new PaymentToken();
		tokenResponse = dbUtils.findPaymentDetails(tokenRequest);
		logger.info("< In PaymentUtilService.getPaymentDetails()");
		return tokenResponse;
	}
	
	/**
	 * Author : nsharma0, 01/27/2020, Transaction History TDD
	 * @param requestDTO
	 * @return TransactionHistoryResponse
	 * @throws PaymentGatewayException
	 */
	
	public TransactionHistoryResponse getTransactionHistory(TransactionHistoryRequestDTO requestDTO) throws PaymentGatewayException {
		logger.info("> In PaymentUtilService.getTransactionHistory()");
		TransactionHistoryResponse response = new TransactionHistoryResponse();
		if(StringUtils.isNotBlank(requestDTO.getCreditCardNumber())){
			if(isNumeric(requestDTO.getCreditCardNumber())){
				PaymentResponse paymentResponse = null;
				PaymentRequest paymentRequest = buildCCPaymentRequest(requestDTO.getCreditCardNumber(), requestDTO.getCreditCardExpDate());
				Calendar startTimeCal = Calendar.getInstance();
				try {
				paymentResponse = paymentGatewayService.processRequest(paymentRequest, startTimeCal);
				} catch (Exception ex) {
					ex.getMessage();
				}
				if (null != paymentResponse && null != paymentResponse.getCcResponse() && StringUtils.isNotBlank(paymentResponse.getCcResponse().getCcTokenNumber())) {
					requestDTO.setCreditCardNumber(paymentResponse.getCcResponse().getCcTokenNumber());
				} else {
					requestDTO.setCreditCardNumber(null);
				}
			}
		}
		response = dbUtils.getTransactionHistory(requestDTO);
		logger.info("< In PaymentUtilService.getTransactionHistory()");
		return response;
	}
	
	
	public String getReauthForOrder(TransactionHistoryRequestDTO requestDTO) throws PaymentGatewayException {
		logger.info("> In PaymentUtilService.getReauthForOrder()");
	//	TransactionHistoryResponse response = new TransactionHistoryResponse();
		if(StringUtils.isNotBlank(requestDTO.getCreditCardNumber())){
			if(isNumeric(requestDTO.getCreditCardNumber())){
				PaymentResponse paymentResponse = null;
				PaymentRequest paymentRequest = buildCCPaymentRequest(requestDTO.getCreditCardNumber(), requestDTO.getCreditCardExpDate());
				Calendar startTimeCal = Calendar.getInstance();
				try {
				paymentResponse = paymentGatewayService.processRequest(paymentRequest, startTimeCal);
				return "SUCCESS";

				} catch (Exception ex) {
					ex.getMessage();
					return "FAIL";
				}
				
				/*if (null != paymentResponse && null != paymentResponse.getCcResponse() && StringUtils.isNotBlank(paymentResponse.getCcResponse().getCcTokenNumber())) {
					requestDTO.setCreditCardNumber(paymentResponse.getCcResponse().getCcTokenNumber());
				} else {
					requestDTO.setCreditCardNumber(null);
				}*/
			}
		}
	//	response = dbUtils.getTransactionHistory(requestDTO);
		logger.info("< In PaymentUtilService.getReauthForOrder()");
		return null;
	}
	
	private boolean isNumeric(String s){
	    String pattern= "\\d+";
	    return s.matches(pattern);
	}
	
	private PaymentRequest buildCCPaymentRequest(String ccNumber, String ccDate) {
        PaymentRequest request=new PaymentRequest();
		String userId = "GUEST";
		OrderInfo orderInfo=new OrderInfo();
		orderInfo.setOrderNumber(0);
		orderInfo.setReleaseNumber(0);
        request.setOrderInfo(orderInfo);
        request.setBusinessUnit(Integer.toString(900));
        request.setPaymentAccountNumber(ccNumber);
        CCInfo ccInfo=new CCInfo();
        ccInfo.setCardDataSource("MANUAL");
        ccInfo.setExpirationDate(ccDate);
        request.setCcInfo(ccInfo);
        request.setTransactionAmount("0");
        request.setPaymentMethod(BusinessConstants.CREDIT_CARD);
        request.setRequesterApp(RequesterApps.WEB);
        request.setCurrencyCode("USD");
        request.setRequestedOperation(BusinessConstants.ZERO_DOLLAR_AUTH);
        request.setRequesterApp(RequesterApps.WEB);
		request.setUserId(userId);
        return request;
    }
	
	/**
	 * Author : nsharma0, 01/27/2020, Transaction History TDD
	 * @param requestDTO
	 * @return TransactionHistoryResponse
	 * @throws PaymentGatewayException
	 */
	public TransactionHistoryResponse getTransactionDetails(TransactionDetailRequestDTO requestDTO) throws PaymentGatewayException {
		logger.info("> In PaymentUtilService.getTransactionDetails()");
		TransactionHistoryResponse response = new TransactionHistoryResponse();
		response = dbUtils.getTransactionDetails(requestDTO);
		logger.info("< In PaymentUtilService.getTransactionDetails()");
		return response;
	}
	
	//bpandurang 4/20/2020 get void auth stauts
	public String findVoidAuthStatus(PaymentToken tokenRequest) {
		logger.info("> In PaymentUtilService.findVoidAuthStatus()");
		String result = dbUtils.findVoidAuthStatus(tokenRequest);
		logger.info("< In PaymentUtilService.findVoidAuthStatus()");
		return result;
	}
	
	//bpandurang 4/24/2020 get reauth for an order
	public String getReAuthForOrder(TransactionHistoryRequestDTO requestDTO) throws PaymentGatewayException {
		logger.info("> In PaymentUtilService.getReAuthForOrder()");
		String response = null;;
		if(StringUtils.isNotBlank(requestDTO.getCreditCardNumber())){
			if(isNumeric(requestDTO.getCreditCardNumber())){
				PaymentResponse paymentResponse = null;
				PaymentRequest paymentRequest = buildCCPaymentRequest(requestDTO.getCreditCardNumber(), requestDTO.getCreditCardExpDate());
				Calendar startTimeCal = Calendar.getInstance();
				try {
				paymentResponse = paymentGatewayService.processRequest(paymentRequest, startTimeCal);
				} catch (Exception ex) {
					ex.getMessage();
				}
				if (null != paymentResponse && null != paymentResponse.getCcResponse() && StringUtils.isNotBlank(paymentResponse.getCcResponse().getCcTokenNumber())) {
					//requestDTO.setCreditCardNumber(paymentResponse.getCcResponse().getCcTokenNumber());
					response = paymentResponse.getVendorTxnStatus();
				} 
			}
		}
		
		return response;
	}

    public CheckPaymentDTO validateCheckPayment(CheckPaymentDTO requestDTO) {
        logger.info("> In PaymentUtilsService.validateCheckPayment");
        FinanceCheckBatch checkBatch = dbUtils.validateCheck(requestDTO.getBatchNumber(), requestDTO.getCheckNumber());
        
        if (checkBatch != null) {
        	boolean checkAlreadyUsed = dbUtils.validateAlreadyUsedCheck(requestDTO.getBatchNumber(), requestDTO.getCheckNumber());
        	if(!checkAlreadyUsed) {
        		requestDTO.setCheckValue(checkBatch.getCheckAmount());
        	}
        }
        return requestDTO;
    }
	
	/**
	 * 7/7/2020
	 * @author blenagh
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public Boolean getMultiReleaseAuthStatus(PaymentToken request) throws Exception {
		logger.info("Querying transaction records for order " + request.getOrderNbr());
		List<Object[]> records = dbUtils.getOrderTransactionRecords(request);
		
		if (null != records) {
			logger.info(records.size() + " transactions returned");
			boolean orderLevelAuth = false;
			String validOrderLevelTranxId = null;
			boolean releaseLevelAuth = false;
			String validReleaseLevelTranxId = null;
			
			for (Object[] obj : records) {
				String operation = obj[0].toString().trim();
				BigDecimal release = (BigDecimal) obj[2];
				Date transactionTime = transactionTimeFormat.parse(obj[7].toString());
				String vendorStatus = obj[9].toString().trim();
				String transactionId = obj[15] == null ? "" : obj[15].toString().trim();
				String token = obj[16] == null ? "" : obj[16].toString().trim();
				String exp = obj[17] == null ? "" : obj[17].toString().trim();
				
				if (operation.equals(BusinessConstants.CAPTURE) && vendorStatus.equals(BusinessConstants.SUCCESS)
						&& release.intValue() == request.getReleaseNbr()) {
					logger.info("Release " + release + " has a successful capture, auth no longer relevant");
					return true;
				}				
				
				if (token.equals(request.getToken()) &&  exp.equals(request.getCcExpDate())) {
					if (release.intValue() == 0) {
						if (operation.equals(BusinessConstants.AUTH) && vendorStatus.equals(BusinessConstants.SUCCESS)) {
							logger.info("order level auth found with transaction ID " + transactionId);
							if (stillValidAuth(transactionTime)) {
								orderLevelAuth = true;
								validOrderLevelTranxId = transactionId;
								logger.info("order level auth from " + transactionTime + " still valid");
							} else
								logger.info("order level auth from " + transactionTime + " has EXPIRED");
						} else if (operation.equals(BusinessConstants.VOID) && vendorStatus.equals(BusinessConstants.SUCCESS)
								&& transactionId.equals(validOrderLevelTranxId)) {
							logger.info("VOID found for transaction ID " + validOrderLevelTranxId);
							orderLevelAuth = false;
							validOrderLevelTranxId = null;
						}
					} else if (release.intValue() == request.getReleaseNbr().intValue()) {
						if (operation.equals(BusinessConstants.AUTH) && vendorStatus.equals(BusinessConstants.SUCCESS)) {
							logger.info("release " + release + " auth found with transaction ID " + transactionId);
							if (stillValidAuth(transactionTime)) {
								releaseLevelAuth = true;
								validReleaseLevelTranxId = transactionId;
								logger.info("release " + release + " auth from " + transactionTime + " still valid");
							} else
								logger.info("release " + release + " auth from " + transactionTime + " has EXPIRED");
						} else if (operation.equals(BusinessConstants.VOID) && vendorStatus.equals(BusinessConstants.SUCCESS)
								&& transactionId.equals(validReleaseLevelTranxId)) {
							logger.info("VOID found for release " + release + " with transaction ID " + transactionId);
							releaseLevelAuth = false;
							validReleaseLevelTranxId = null;
						}
					}
				}
			}
			
			if (orderLevelAuth || releaseLevelAuth) {
				logger.info("Good auth found for order " + request.getOrderNbr() + ", release " + request.getReleaseNbr());
				return true;
			}
		}
		
		logger.info("No valid auth found for order " + request.getOrderNbr() + ", release " + request.getReleaseNbr());
		return false;
	}
	
	/**
	 * 7/7/2020
	 * @author blenagh
	 * @param transactionTime
	 * @return
	 */
	private boolean stillValidAuth(Date transactionTime) {
		Calendar c = Calendar.getInstance();
		c.setTime(transactionTime);
		c.add(Calendar.DAY_OF_MONTH, getAuthExpirationDays());
		return c.after(Calendar.getInstance());
	}

	/**
	 * 7/7/2020
	 * @author blenagh
	 * @return
	 */
	private int getAuthExpirationDays() {
		int days = 7;
		
		try {
			days = Integer.parseInt(PAYMENTGATEWAYController.properties.get(CommonConstants.AUTHORIZATION_EXPIRATION_DAYS_GENERAL));
		} catch (Exception e) {
			logger.error("Error parsing auth expy days, default to 7", e);
		}
		
		return days;
	}
	
	/**
	 * nsharma0, 09/25/2020, search by cc token
	 * */
	public List<Long> searchCustomerByCC(String queryBy) throws PaymentGatewayException {
		logger.info("> In PaymentUtilService.searchCustomerByCC()");
		List<Long> response = null;
		String token = null;
		if(StringUtils.isNotBlank(queryBy)){
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
		}
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
	public TransactionHistoryResponse findPGAuditInfoByOrderNbr(TransactionDetailRequestDTO requestDTO) throws PaymentGatewayException {
		logger.info("> In PaymentUtilService.findPGAuditInfoByOrderNbr()");
		TransactionHistoryResponse response = new TransactionHistoryResponse();
		response = dbUtils.findPGAuditInfoByOrderNbr(requestDTO);
		logger.info("< In PaymentUtilService.findPGAuditInfoByOrderNbr()");
		return response;
	}
}
