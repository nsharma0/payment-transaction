package com.oriental.paymentgateway.utils;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oriental.comet.db2.customer.dto.AccountTransactionHistoryDBResponse;
import com.oriental.comet.db2.dao.PaymentService;
import com.oriental.comet.db2.payment.entity.FinanceCheckBatch;
import com.oriental.comet.db2.payment.entity.PGAuditTrialEntity;
import com.oriental.comet.db2.payment.entity.PGPendingPmtEntity;
import com.oriental.comet.db2.payment.entity.PGTSYSMIDEntity;
import com.oriental.comet.db2.payment.entity.PGTokenEntity;
import com.oriental.comet.db2.payment.entity.PGTokenEntity.Id;
import com.oriental.comet.db2.payment.entity.PGVendorCodesMapEntity;
import com.oriental.paymentgateway.exceptions.PaymentGatewayException;
import com.oriental.paymentgateway.mapper.PaypalRequestMapper;
import com.oriental.paymentgateway.mapper.TSYSRequestMapper;
import com.oriental.paymentgateway.service.CreditCardValidator;
import com.oriental.paymentgateway.service.PaymentUtilsService;
import com.oriental.paymentgatewaypayload.dto.web.AddressInfo;
import com.oriental.paymentgatewaypayload.dto.web.AltPayResponse;
import com.oriental.paymentgatewaypayload.dto.web.CCInfo;
import com.oriental.paymentgatewaypayload.dto.web.CCResponse;
import com.oriental.paymentgatewaypayload.dto.web.OrderInfo;
import com.oriental.paymentgatewaypayload.dto.web.PartialShipmentData;
import com.oriental.paymentgatewaypayload.dto.web.PaymentRequest;
import com.oriental.paymentgatewaypayload.dto.web.PaymentResponse;
import com.oriental.paymentgatewaypayload.dto.web.PaymentToken;
import com.oriental.paymentgatewaypayload.dto.web.PaypalInfo;
import com.oriental.paymentgatewaypayload.dto.web.PaypalResponse;
import com.oriental.paymentgatewaypayload.dto.web.PendingPayments;
import com.oriental.paymentgatewaypayload.dto.web.TransactionDetailRequestDTO;
import com.oriental.paymentgatewaypayload.dto.web.TransactionHistoryRecord;
import com.oriental.paymentgatewaypayload.dto.web.TransactionHistoryRequestDTO;
import com.oriental.paymentgatewaypayload.dto.web.TransactionHistoryResponse;
import com.oriental.paymentgatewaypayload.enums.AddressVerificationCodeEnum;
import com.oriental.paymentgatewaypayload.enums.CardTypeEnum;
import com.oriental.paymentgatewaypayload.utils.BusinessConstants;
import com.sun.jersey.api.client.ClientResponse.Status;

@Component
public class DatabaseUtils {

	private static Logger logger = LoggerFactory.getLogger(DatabaseUtils.class);

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private CreditCardValidator creditCardValidator;

	private ObjectMapper objMapper = new ObjectMapper();

	@Autowired
	private TSYSRequestMapper tsysRequestMapper;
	
	@Autowired
	private PaypalRequestMapper paypalRequestMapper;
	
	@Autowired
	private PaymentUtilsService paymentUtilsService;
	
	/**
	 * @since 4/28/2023
	 * @author blenagh
	 * 
	 * Resolve circular dependency
	 */
	@PostConstruct
	public void init() {
		paymentUtilsService.setDbUtils(this);
	}

	public void saveToTokenEntity(PaymentRequest paymentRequest, PaymentResponse paymentResponse) {
		logger.info("> Entered DatabaseUtils.saveToTokenEntity for saving data into Token table");
		PGTokenEntity entity = null;
		try {
			entity = getTokenEntityObject(paymentRequest, paymentResponse);
			paymentService.saveCCToken(entity);
		} catch (Exception e) {
			logger.error("Logging to token table failed for the order : " + entity.getId().getOrderNbr());
			e.printStackTrace();
			throw new PaymentGatewayException(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
					"Error while saving values into token table");
		}
		logger.info("< Saved token details to Token table successfully");
	}

	public PGTokenEntity getTokenEntityObject(PaymentRequest paymentRequest, PaymentResponse paymentResponse) {
		PGTokenEntity tokenEntity = new PGTokenEntity();
		CCInfo ccInfo = paymentRequest.getCcInfo();
		CCResponse ccResponse = paymentResponse.getCcResponse();
		Id id = tokenEntity.getId();

		id.setToken((null != ccResponse.getCcTokenNumber()) ? ccResponse.getCcTokenNumber()
				: paymentRequest.getPaymentAccountNumber());
		// We are not saving transactionId if we are writing a record into token
		// table for card Authentication
		//blenagh 5/12/2021 - Clarus, add SALE
		if (paymentRequest.getRequestedOperation().equalsIgnoreCase(BusinessConstants.AUTH)
				|| paymentRequest.getRequestedOperation().equalsIgnoreCase(BusinessConstants.RETURN)
				|| paymentRequest.getRequestedOperation().equalsIgnoreCase(BusinessConstants.SALE))
			id.setTransactionId(paymentResponse.getTransactionId());
		id.setCustomerNbr(CommonUtils.getIntfromString(paymentRequest.getCustomerNumber()));
		id.setOrderNbr(new BigDecimal(paymentRequest.getOrderInfo().getOrderNumber()));
		id.setReleaseNbr(new BigDecimal(paymentRequest.getOrderInfo().getReleaseNumber()));
		id.setBuNbr(new BigDecimal(paymentRequest.getBusinessUnit()));

		/**
		 * blenagh 6/15/2016 Adding flag values for identifying type of token
		 */
		if (paymentRequest.getRequestedOperation().equals(BusinessConstants.AUTH))
			tokenEntity.setFraudFlag(CommonConstants.AUTH_TOKEN);
		else if (paymentRequest.getRequestedOperation().equals(BusinessConstants.RETURN))
			tokenEntity.setFraudFlag(CommonConstants.RETURN_TOKEN);
		else if (paymentRequest.getRequestedOperation().equals(BusinessConstants.CAPTURE))
			tokenEntity.setFraudFlag(CommonConstants.CAPTURE_TOKEN);
		else if (paymentRequest.getRequestedOperation().equals(BusinessConstants.VOID))
			tokenEntity.setFraudFlag(CommonConstants.VOID_TOKEN);
		else if (paymentRequest.getRequestedOperation().equals(BusinessConstants.ZERO_DOLLAR_AUTH))
			tokenEntity.setFraudFlag(CommonConstants.ZERO_AUTH_TOKEN);
		//blenagh 5/12/2021 - Clarus
		else if (paymentRequest.getRequestedOperation().equals(BusinessConstants.SALE))
			tokenEntity.setFraudFlag(CommonConstants.SALE_TOKEN);
		
		//blenagh 8/27/2021 - CC Regs
		if (null != paymentResponse.getCcResponse().getHostResponseCode()) {
			PGVendorCodesMapEntity map = paymentService.getVendorCodeMapping(paymentResponse.getCcResponse().getHostResponseCode());
			if (null != map && map.getFnReturnDesc().trim().equals(CommonConstants.VENDOR_MAP_RECORD_TYPE_FRAUD)) {
				logger.info("Found fraud host response code of " + paymentResponse.getCcResponse().getHostResponseCode());
				tokenEntity.setFraudFlag(CommonConstants.YES);
			}
		}

		if (paymentResponse.getVendorTxnCode().equalsIgnoreCase(CommonConstants.ERROR_D2998)
				|| paymentResponse.getVendorTxnCode().equalsIgnoreCase(CommonConstants.ERROR_D2002))
			tokenEntity.setFraudFlag(CommonConstants.YES);
		tokenEntity.setCardType(ccResponse.getCardType().getValue());
		tokenEntity.setCcExpDate(ccInfo.getExpirationDate());
		tokenEntity.setId(id);

		return tokenEntity;
	}

	public String saveToPendingPayments(PendingPayments pendingPaymentsRequest) throws PaymentGatewayException {
		logger.info("> Entered DatabaseUtils.saveToPendingPayments for saving data into Token table");
		PGPendingPmtEntity entity = null;
		String status = BusinessConstants.FAIL;
		try {
			entity = getPendingPaymentsEntity(pendingPaymentsRequest);
			paymentService.savetoPendingPmts(entity);
		} catch (Exception e) {
			logger.error("Logging to pending payments table failed for the order : "
					+ pendingPaymentsRequest.getOrderNumber());
			e.printStackTrace();
			throw new PaymentGatewayException(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
					"Error while saving values into Pending payments table");
		}
		status = BusinessConstants.SUCCESS;
		logger.info("< Saved  details to Pending payments table successfully");
		return status;
	}

	/**
	 * @author SAdiraju 6/23/2016
	 * @param paymentTokenRequest
	 * 
	 * @return
	 * 
	 * 		Update rows in the PGWTRAADT table that have an order id matching
	 *         the orderid from the request with the customer number
	 */

	public String saveCustomerNumberToAuditTrail(PaymentToken paymentTokenRequest) throws PaymentGatewayException {
		logger.info("> Entered DatabaseUtils.saveCustomerNumberToAuditTrail for saving data into PGWTRAADT table");

		String status = BusinessConstants.FAIL;
		try {

			paymentService.saveCustomerNumberToAuditTrail(paymentTokenRequest.getCustomerNbr().intValue(),
					paymentTokenRequest.getOrderNbr().intValue());

		} catch (Exception e) {
			logger.error("Logging to payment transaction audit table failed for the order : "
					+ paymentTokenRequest.getOrderNbr());
			e.printStackTrace();
			throw new PaymentGatewayException(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
					"Error while saving values into payment transaction audit table");
		}
		status = BusinessConstants.SUCCESS;

		logger.info("< Saved  details to Payment Transaction Audit table successfully");
		return status;
	}

	public String deletePendingPayments(PendingPayments pendingPaymentsRequest) throws PaymentGatewayException {
		logger.info("> Entered DatabaseUtils.deletePendingPayments for saving data into Token table");
		PGPendingPmtEntity entity = null;
		String status = null;
		try {
			entity = getPendingPaymentsEntity(pendingPaymentsRequest);
			status = paymentService.deletePendingPmts(entity);
		} catch (Exception e) {
			logger.error("Deleting from pending payments table failed for the order : "
					+ pendingPaymentsRequest.getOrderNumber());
			e.printStackTrace();
			throw new PaymentGatewayException(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
					"Error while saving values into Pending payments table");
		}
		logger.info("< Deleted details from pending payments table successfully");
		return status;
	}

	public PGPendingPmtEntity getPendingPaymentsEntity(PendingPayments request) {
		PGPendingPmtEntity entity = new PGPendingPmtEntity();
		PGPendingPmtEntity.Id id = entity.getId();

		id.setOrderNbr(new BigDecimal(request.getOrderNumber()));
		id.setReleaseNbr(request.getReleaseNumber());
		id.setRequestedOperation(request.getRequestedOperation());
		id.setPaymentMethod(request.getPaymentMethod());

		if (null != request && null != request.getTransactionId())
			entity.setTransactionId(request.getTransactionId());

		entity.setBuNbr(new BigDecimal(request.getBusinessUnit()));
		entity.setOrderTsp(request.getOrderTimeStamp());
		entity.setCardHolderName(fitLength(request.getCardHolderName(), 30));
		entity.setCcExpDate(request.getCcExpDate());

		entity.setId(id);
		return entity;
	}

	public List<Integer> findAllBUs() {
		logger.info("> DatabaseUtils.findAllBUs");
		List<Integer> buList = paymentService.findAllBUs();
		logger.info("< Got buList. Size is " + buList.size());
		return buList;
	}

	public PaymentToken findTokenByBU(String token, BigDecimal businessUnitNbr) {
		logger.info("> DatabaseUtils.findTokenByBU");
		PGTokenEntity entity = paymentService.findTokenByBU(token, businessUnitNbr);
		logger.info("< DatabaseUtils.findTokenByBU");
		return convertTokenEntityToDTO(entity);
	}

	public PaymentToken convertTokenEntityToDTO(PGTokenEntity entity) {
		PaymentToken dto = new PaymentToken();
		Id id = null;
		if (null != entity && null != entity.getId()) {
			id = entity.getId();
			dto.setToken(id.getToken().trim());
			dto.setCardType(entity.getCardType());
			dto.setIsValid(CommonConstants.YES);
			
			// gsrinivas - 01/08/2019: Breakfix: INC0095773
			dto.setTransactionId(id.getTransactionId().trim());
			
			//blenagh 8/27/2021 - CC Regs
			if (StringUtils.isNotEmpty(entity.getFraudFlag()))
				dto.setFraudFlag(entity.getFraudFlag());
		} else {
			dto.setIsValid(CommonConstants.NO);
			dto.setCardType(CardTypeEnum.U.getValue());
		}
		return dto;
	}

	@Async
	public void updateAuthResults(Integer orderNumber, Integer releaseNumber, String sprocName, Date timestamp) {
		logger.info("> Async DatabaseUtils.updateAuthResults for orderNumber :" + orderNumber);
		
		
		paymentService.updateAuthResults(orderNumber, releaseNumber, sprocName, timestamp);
		logger.info("< Async DatabaseUtils.updateAuthResults for OrderNumber:" + orderNumber);
	}

	@Async
	public void updateVoidResults(Integer orderNumber, Integer releaseNumber, String sprocName, Date timestamp) {
		logger.info("> Async DatabaseUtils.updateVoidResults for orderNumber :" + orderNumber);
	
		paymentService.updateVoidResults(orderNumber, releaseNumber, sprocName, timestamp);
		logger.info("< Async DatabaseUtils.updateVoidResults for OrderNumber:" + orderNumber);
	}
	public String findAuthStatus(PaymentToken tokenRequest) {
		logger.info("> In DatabaseUtils.findAuthStatus()");
		String result = paymentService.findAuthStatus(tokenRequest.getToken(), tokenRequest.getOrderNbr(),
				tokenRequest.getReleaseNbr(), tokenRequest.getBuNbr(), tokenRequest.getCcExpDate());
		logger.info("< In DatabaseUtils.findAuthStatus()");
		return result;
	}
	
	public String findPayPalAuthStatus(PaymentToken tokenRequest) {
		logger.info("> In DatabaseUtils.findPaypalAuthStatus()");
		String result = paymentService.findPayPalAuthStatus( tokenRequest.getOrderNbr());
		logger.info("< In DatabaseUtils.findPaypalAuthStatus()");
		return result;
	}

	public void updateAuditTrialForRequest(PaymentRequest paymentRequest) throws PaymentGatewayException {

		String json = null;
		String paymentAcntNbr = null;
		PGAuditTrialEntity auditTrial = new PGAuditTrialEntity();
		try {

			//blenagh 7/23/2018 - masking CVV
			String cvv = null;
			if (paymentRequest.getCcInfo() != null) {
				cvv = paymentRequest.getCcInfo().getCvv();
				if (cvv != null)
					paymentRequest.getCcInfo().setCvv(null);
			}
			//blenagh 5/5/2017 - masking station ID
			String stationId = paymentRequest.getStationId();
			paymentRequest.setStationId("[stationId]");
			// Credit card number should not be logged into logs
			if (paymentRequest.getPaymentMethod()
					.equalsIgnoreCase(com.oriental.paymentgatewaypayload.utils.BusinessConstants.CREDIT_CARD)
					&& null != paymentRequest.getPaymentAccountNumber()) {
				paymentAcntNbr = paymentRequest.getPaymentAccountNumber();
				paymentRequest.setPaymentAccountNumber("XXXX-XXXX-XXXX-" + paymentRequest.getPaymentAccountNumber()
						.substring(paymentRequest.getPaymentAccountNumber().length() - 4));
				auditTrial.setPaymentAccountNbr(paymentRequest.getPaymentAccountNumber()
						.substring(paymentRequest.getPaymentAccountNumber().length() - 4));
			}

			json = objMapper.writeValueAsString(paymentRequest);
			logger.info(json);
			
			//blenagh 7/23/2018 - reset CVV
			if (cvv != null)
				paymentRequest.getCcInfo().setCvv(cvv);
			//blenagh 5/5/2017 - reset stationId
			paymentRequest.setStationId(stationId);
			if (paymentRequest.getPaymentMethod()
					.equalsIgnoreCase(com.oriental.paymentgatewaypayload.utils.BusinessConstants.CREDIT_CARD)
					&& null != paymentAcntNbr) {
				paymentRequest.setPaymentAccountNumber(paymentAcntNbr);
				//blenagh 5/12/2021 - Clarus, add Sale
				if (paymentRequest.getRequestedOperation().equalsIgnoreCase(BusinessConstants.AUTH)
						|| paymentRequest.getRequestedOperation().equalsIgnoreCase(BusinessConstants.SALE))
					paymentRequest.getCcInfo().setCardType(creditCardValidator.getCardType(paymentAcntNbr));
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			logger.error(
					"JsonProcessingException occured while converting request object to json for logging to auditTrial table.");
		}
		Date date = Calendar.getInstance().getTime();
		if (paymentRequest.getPaymentMethod().equalsIgnoreCase(com.oriental.paymentgatewaypayload.utils.BusinessConstants.CREDIT_CARD)) {
			PGTSYSMIDEntity entity = tsysRequestMapper.getMIDCfgObjectCC(paymentRequest);
			/**
			 * blenagh 6/14/2016 Retrieve config Id for originating transaction
			 * if found
			 */
			if(entity !=null){
			if (paymentRequest.getTransactionId() != null && !paymentRequest.getTransactionId().equals("")) {
				//MMuppidathy changed the logic for Digital Experience to set the correct config Id based on releases.
					
				String originReqApp = paymentService.findOriginConfigId(paymentRequest.getTransactionId(),
						paymentRequest.getOrderInfo().getOrderNumber(),BusinessConstants.AUTH);
				if(originReqApp!=null)
		 		    entity=tsysRequestMapper.getMIDCfg(paymentRequest,originReqApp); 
				 if(entity!=null)
				    paymentRequest.setOperatorId(entity.getId().getConfigId().toString());

			} else
				paymentRequest.setOperatorId(entity.getId().getConfigId().toString());
			}
		}
		


		//MMuppidathy QC-311 Added for Paypal to set the operator Id
		//SAdiraju 08/03/2016 updated to check for order info not null as GET_EXPRESS calls do not have an order number 
	if(paymentRequest.getPaymentMethod().equalsIgnoreCase(com.oriental.paymentgatewaypayload.utils.BusinessConstants.PAYPAL)
			||paymentRequest.getPaymentMethod()
			.equalsIgnoreCase(com.oriental.paymentgatewaypayload.utils.BusinessConstants.PAYPAL_CREDIT)){
			String switchStatus = paymentUtilsService.getSwitchStatus(paymentRequest.getBusinessUnit(),
					paymentRequest.getPaymentMethod());
			PGTSYSMIDEntity entity = paypalRequestMapper.getMIDCfgObject(paymentRequest);
			if(entity !=null){
			   if (paymentRequest.getTransactionId() != null && !paymentRequest.getTransactionId().equals("") 
					   && paymentRequest.getOrderInfo()!=null && switchStatus.equalsIgnoreCase(CommonConstants.ON)) {
				 //MMuppidathy changed the logic for Digital Experience to set the correct config Id based on releases.
			
				String originReqApp = paymentService.findOriginConfigId(paymentRequest.getTransactionId(),
						paymentRequest.getOrderInfo().getOrderNumber(),BusinessConstants.PAYPAL_AUTHORIZE);
				if (originReqApp != null)
				  entity=paypalRequestMapper.getMIDCfg(paymentRequest,originReqApp); 
			    if(entity!=null)
				  paymentRequest.setOperatorId(entity.getId().getConfigId().toString());
				
			} else
				paymentRequest.setOperatorId(entity.getId().getConfigId().toString());
		 
			}
				
		}
	
		AddressInfo addressInfo = CommonUtils.getAddressFromList(paymentRequest.getAddressList(),
				CommonConstants.BILLING_ADDRESS);
		CCInfo ccInfo = paymentRequest.getCcInfo();
		OrderInfo orderInfo = paymentRequest.getOrderInfo();
		PaypalInfo ppInfo = paymentRequest.getPaypalInfo();
		PartialShipmentData psData = paymentRequest.getPartialShipmentData();
		auditTrial.setRecordType(CommonConstants.REQUEST);

		auditTrial.getId().setPaymentMethod(fitLength(paymentRequest.getPaymentMethod(), 10));
		auditTrial.getId().setRequestedOperation(fitLength(paymentRequest.getRequestedOperation(), 20));
		if (null != paymentRequest.getRequesterApp())
			auditTrial.setRequesterApp(fitLength(paymentRequest.getRequesterApp().name(), 10));
		auditTrial.getId().setBuNbr(CommonUtils.getIntfromString(paymentRequest.getBusinessUnit()));
		if (null != paymentRequest.getCustomerNumber())
			auditTrial.getId().setCustomerNbr(CommonUtils.getIntfromString(paymentRequest.getCustomerNumber()));
		auditTrial.setTxnAmt(new BigDecimal(
				(null != paymentRequest.getTransactionAmount()) ? paymentRequest.getTransactionAmount() : "0"));
		if (!paymentRequest.getPaymentMethod()
				.equalsIgnoreCase(com.oriental.paymentgatewaypayload.utils.BusinessConstants.CREDIT_CARD)
				&& null != paymentRequest.getPaymentAccountNumber())
			auditTrial.setPaymentAccountNbr(fitLength(paymentRequest.getPaymentAccountNumber(), 25));
		if (null != paymentRequest.getUserId())
			auditTrial.setUserId(fitLength(paymentRequest.getUserId(), 20));
		if (null != paymentRequest.getOperatorId())
			auditTrial.setRptCfgId(new BigDecimal(fitLength(paymentRequest.getOperatorId(), 5)));
		if (null != paymentRequest.getTransactionId())
			auditTrial.setTransationId(fitLength(paymentRequest.getTransactionId(), 50));
		//sadiraju 07/11/2016 updated to display transaction type in COMET transaction history
		if (paymentRequest.getPaymentMethod().equalsIgnoreCase(com.oriental.paymentgatewaypayload.utils.BusinessConstants.PAYPAL))
			auditTrial.setCardType("P");
		if (paymentRequest.getPaymentMethod().equalsIgnoreCase(com.oriental.paymentgatewaypayload.utils.BusinessConstants.PAYPAL_CREDIT))
			auditTrial.setCardType("B");

		auditTrial.setCurrencyCode(840);

		auditTrial.getId().setTransactionTimestamp(new Timestamp(date.getTime()));
		auditTrial.setTransactionDate(date);
		auditTrial.setTransactionTime(new Time(date.getTime()));

		paymentRequest.setTransactiondate(date);
//SAdiraju 07/22/2016 updated to only save the weborderid for requests other than capture or refund
		if (null != orderInfo) {
			if (orderInfo.getOrderNumber() > 0)
				auditTrial.getId().setOrderNbr(orderInfo.getOrderNumber());
			if (orderInfo.getReleaseNumber() > 0)
				auditTrial.getId().setReleaseNbr(orderInfo.getReleaseNumber());
			if (null != orderInfo.getWebOrderId() && !paymentRequest.getRequestedOperation().equalsIgnoreCase(BusinessConstants.PAYPAL_CAPTURE) && !paymentRequest.getRequestedOperation().equalsIgnoreCase(BusinessConstants.PAYPAL_REFUND))
				auditTrial.setWebOrderId(CommonUtils.getIntfromString(orderInfo.getWebOrderId()));
		}
		if (null != ccInfo) {
			if (null != ccInfo.getExpirationDate())
				auditTrial.setCcExpDate(fitLength(ccInfo.getExpirationDate(), 6));
			if (null != ccInfo.getCardHolderBusinessName())
				auditTrial.setCardHolderBusinessName(fitLength(ccInfo.getCardHolderBusinessName(), 45));
			if (null != paymentRequest.getOperatorId())
				auditTrial.setRptCfgId(new BigDecimal(fitLength(paymentRequest.getOperatorId(), 5)));
			if (null != ccInfo.getCardHolderName())
				auditTrial.setCardHolderName(fitLength(ccInfo.getCardHolderName(), 30));
			if (null != ccInfo.getCardType())
				auditTrial.setCardType(ccInfo.getCardType().getValue());
			//blenagh 7/20/2018 - CVV
			if (null != ccInfo.getCvv())
				auditTrial.setCvvFlag(1);
		}
		if (null != addressInfo) {
			if (null != addressInfo.getFirstName())
				auditTrial.setFirstName(fitLength(addressInfo.getFirstName(), 30));
			if (null != addressInfo.getLastName())
				auditTrial.setLastName(fitLength(addressInfo.getLastName(), 25));
			if (null != addressInfo.getFullName())
				auditTrial.setFullName(fitLength(addressInfo.getFullName(), 45));
			if (null != addressInfo.getAddress1())
				auditTrial.setAddressLine1(fitLength(addressInfo.getAddress1(), 30));
			if (null != addressInfo.getPostalCode())
				auditTrial.setPostalCode(fitLength(addressInfo.getPostalCode(), 12));
		}
		if (null != ppInfo) {
			if (null != ppInfo.getEmail())
				auditTrial.setEmail(fitLength(ppInfo.getEmail(), 80));
			if (null != ppInfo.getIpAddress())
				auditTrial.setIpAddress(fitLength(ppInfo.getIpAddress(), 30));
			if (null != ppInfo.getCaptureType())
				auditTrial.setCaptureType(fitLength(ppInfo.getCaptureType(), 15));
			if (null != ppInfo.getVendorOrderId())
				auditTrial.setVendorOrderId(ppInfo.getVendorOrderId());

		}
		
		/* bpandurang 5/5/17 - update Capture Type(cptrtyp) to store 
		 * sequence number & total number of shipments....like this 1:4 
		 */
		String partialShipment = paymentRequest.getIsPartialShipment();
		if(null != partialShipment) {
			String ps = "";			
			if (partialShipment.equalsIgnoreCase("y"))
				ps = "Y";
			// bpandurang 6/6/17 Updated check for partial shipment is not null 
			if (null != psData && null != psData.getCurrentPaymentSequenceNumber() &&
					null != psData.getCurrentPaymentSequenceNumber()) {
				auditTrial.setCaptureType(psData.getCurrentPaymentSequenceNumber() + ":"
						+ psData.getTotalPaymentCount() + ":"
						+ ps);
			}	
		}
		saveAuditTrial(auditTrial, null);
	}

	private void saveAuditTrial(PGAuditTrialEntity auditTrial, PaymentResponse paymentResponse)
			throws PaymentGatewayException {
		try {
			paymentService.saveAuditTrial(auditTrial);
		} catch (Exception e) {
			logger.error("Logging to audit trial failed for the order : " + auditTrial.getId().getOrderNbr());
			e.printStackTrace();
			PaymentGatewayException pgException = new PaymentGatewayException(
					Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Error while saving Response to AuditTrial Table");
			pgException.setPaymentResponse(paymentResponse);
			throw pgException;
		}
	}

	public void updateAuditTrialForResponse(PaymentResponse paymentResponse, PaymentRequest paymentRequest)
			throws PaymentGatewayException {

		PGAuditTrialEntity auditTrial = new PGAuditTrialEntity();
		AddressInfo addressInfo = (null != paymentResponse.getAddressList())
				? CommonUtils.getAddressFromList(paymentResponse.getAddressList(), CommonConstants.BILLING_ADDRESS)
				: null;
		CCResponse ccResponse = paymentResponse.getCcResponse();
		PaypalResponse ppResponse = paymentResponse.getPpResponse();
		AltPayResponse altPayResponse = paymentResponse.getAltPayResponse();

		String json = null;
		try {
			json = objMapper.writeValueAsString(paymentResponse);
			logger.info(json);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			logger.error("Error occurred in converting response object to json for logging to AuditTrial table");
		}

		CCInfo ccInfo = paymentRequest.getCcInfo();
		OrderInfo orderInfo = paymentRequest.getOrderInfo();
		PaypalInfo ppInfo = paymentRequest.getPaypalInfo();
		PartialShipmentData psData = paymentRequest.getPartialShipmentData();
		
		auditTrial.setRecordType(CommonConstants.RESPONSE);
		auditTrial.getId().setPaymentMethod(fitLength(paymentRequest.getPaymentMethod(), 10));
		auditTrial.getId().setRequestedOperation(fitLength(paymentRequest.getRequestedOperation(), 20));

		if (null != paymentRequest.getRequesterApp())
			auditTrial.setRequesterApp(fitLength(paymentRequest.getRequesterApp().name(), 10));

		if (null != paymentRequest.getBusinessUnit())
			auditTrial.getId().setBuNbr(CommonUtils.getIntfromString(paymentRequest.getBusinessUnit()));
		if (null != paymentRequest.getCustomerNumber())
			auditTrial.getId().setCustomerNbr(CommonUtils.getIntfromString(paymentRequest.getCustomerNumber()));
		auditTrial.setTxnAmt(new BigDecimal(
				(null != paymentRequest.getTransactionAmount()) ? paymentRequest.getTransactionAmount() : "0"));
		if (!paymentRequest.getPaymentMethod().equalsIgnoreCase(com.oriental.paymentgatewaypayload.utils.BusinessConstants.CREDIT_CARD)
				&& null != paymentRequest.getPaymentAccountNumber())
			auditTrial.setPaymentAccountNbr(fitLength(paymentRequest.getPaymentAccountNumber(), 25));
		if (null != paymentRequest.getUserId())
			auditTrial.setUserId(fitLength(paymentRequest.getUserId(), 20));
		if (null != paymentRequest.getOperatorId())
			auditTrial.setRptCfgId(new BigDecimal(fitLength(paymentRequest.getOperatorId(), 5)));
		//sadiraju 07/11/2016 updated to display transaction type in COMET transaction history
		if (paymentRequest.getPaymentMethod().equalsIgnoreCase(com.oriental.paymentgatewaypayload.utils.BusinessConstants.PAYPAL))
			auditTrial.setCardType("P");
		if (paymentRequest.getPaymentMethod().equalsIgnoreCase(com.oriental.paymentgatewaypayload.utils.BusinessConstants.PAYPAL_CREDIT))
			auditTrial.setCardType("B");

		auditTrial.setCurrencyCode(840);

		if (null != orderInfo) {
			if (orderInfo.getOrderNumber() > 0)
				auditTrial.getId().setOrderNbr(orderInfo.getOrderNumber());
			if (orderInfo.getReleaseNumber() > 0)
				auditTrial.getId().setReleaseNbr(orderInfo.getReleaseNumber());
		}
		if (null != ccInfo) {
			if (null != ccInfo.getExpirationDate())
				auditTrial.setCcExpDate(fitLength(ccInfo.getExpirationDate(), 6));
			if (null != ccInfo.getCardHolderBusinessName())
				auditTrial.setCardHolderBusinessName(fitLength(ccInfo.getCardHolderBusinessName(), 45));
			if (null != paymentRequest.getOperatorId())
				auditTrial.setRptCfgId(new BigDecimal(fitLength(paymentRequest.getOperatorId(), 5)));
			if (null != ccInfo.getCardHolderName())
				auditTrial.setCardHolderName(fitLength(ccInfo.getCardHolderName(), 30));
			if (null != ccInfo.getCardType())
				auditTrial.setCardType(ccInfo.getCardType().getValue());
		}
		if (null != addressInfo) {

			if (null != addressInfo.getFirstName())
				auditTrial.setFirstName(fitLength(addressInfo.getFirstName(), 30));
			if (null != addressInfo.getLastName())
				auditTrial.setLastName(fitLength(addressInfo.getLastName(), 25));
			if (null != addressInfo.getFullName())
				auditTrial.setFullName(fitLength(addressInfo.getFullName(), 45));
			if (null != addressInfo.getAddress1())
				auditTrial.setAddressLine1(fitLength(addressInfo.getAddress1(), 30));
			if (null != addressInfo.getPostalCode())
				auditTrial.setPostalCode(fitLength(addressInfo.getPostalCode(), 12));
		}
		if (null != ppInfo) {
			if (null != ppInfo.getEmail())
				auditTrial.setEmail(fitLength(ppInfo.getEmail(), 80));
			if (null != ppInfo.getIpAddress())
				auditTrial.setIpAddress(fitLength(ppInfo.getIpAddress(), 30));
			if (null != ppInfo.getCaptureType())
				auditTrial.setCaptureType(fitLength(ppInfo.getCaptureType(), 15));
			if (null != ppInfo.getVendorOrderId())
				auditTrial.setVendorOrderId(ppInfo.getVendorOrderId());

		}
		/* bpandurang 5/5/17 - update Capture Type(cptrtyp) to store 
		 * sequence number & total number of shipments....like this 1:4 
		 */
		String partialShipment = paymentRequest.getIsPartialShipment();
		if(null != partialShipment) {
			String ps = "";			
			if (partialShipment.equalsIgnoreCase("y"))
				ps = "Y";
			// bpandurang 6/6/17 Updated check for partial shipment is not null 
			if (null != psData && null != psData.getCurrentPaymentSequenceNumber() &&
					null != psData.getCurrentPaymentSequenceNumber()) {
				auditTrial.setCaptureType(psData.getCurrentPaymentSequenceNumber() + ":"
						+ psData.getTotalPaymentCount() + ":"
						+ ps);
			}	
		}
		
//SAdiraju 07/22/2016 updated to only save the weborderid for requests other than capture or refund
		// response fields
		if (null != paymentResponse.getWebOrderId() && !paymentRequest.getRequestedOperation().equalsIgnoreCase(BusinessConstants.PAYPAL_CAPTURE) && !paymentRequest.getRequestedOperation().equalsIgnoreCase(BusinessConstants.PAYPAL_REFUND)){
			auditTrial.setWebOrderId(CommonUtils.getIntfromString(paymentResponse.getWebOrderId()));
		}
		
		if (null != paymentResponse.getTransactionId())
			auditTrial.setTransationId(fitLength(paymentResponse.getTransactionId(), 50));
		if (null != paymentResponse.getResponseStatus()) {
			auditTrial.setPymtGtwyTraStatus(paymentResponse.getResponseStatus());
			if (paymentResponse.getResponseStatus().equalsIgnoreCase(BusinessConstants.FAIL))
				auditTrial.setVendorTraStatus(BusinessConstants.FAIL);
		}
		if (null != paymentResponse.getResponseCode())
			auditTrial.setPymtGtwyTraCode(paymentResponse.getResponseCode());
		if (null != paymentResponse.getResponseDescription()) {
			auditTrial.setPymtGtwyTraStatusDesc(fitLength(paymentResponse.getResponseDescription(), 50));
		}

		if (null != paymentResponse.getVendorTxnStatus()){
				auditTrial.setVendorTraStatus(paymentResponse.getVendorTxnStatus());
			}
		if (null != paymentResponse.getVendorTxnCode())
			auditTrial.setVendorTraCode(paymentResponse.getVendorTxnCode());
		if (null != paymentResponse.getVendorTxnDescription())
			auditTrial.setVendorTraDescription(fitLength(paymentResponse.getVendorTxnDescription(), 50));

		else if (null != paymentResponse.getServiceErrorCode()
				&& !paymentResponse.getServiceErrorCode().equalsIgnoreCase("0")) {
			auditTrial.setVendorTraCode(paymentResponse.getServiceErrorCode());
			if (null != paymentResponse.getServiceErrorDesc()) {
				auditTrial.setVendorTraDescription(fitLength(paymentResponse.getServiceErrorDesc(), 50));
			}
			auditTrial.setVendorTraStatus(BusinessConstants.FAIL);
		} else {
			setVendorStatusFields(auditTrial);
		}

		Date date = paymentResponse.getTransactiondate();

		auditTrial.getId().setTransactionTimestamp(new Timestamp(date.getTime()));
		auditTrial.setTransactionDate(date);
		auditTrial.setTransactionTime(new Time(date.getTime()));

		if (null != ccResponse) {
			if (null != ccResponse.getTransactedAmount())
				auditTrial.setTxnAmt(ccResponse.getTransactedAmount());
			if (null != ccResponse.getAuthCode())
				auditTrial.setAuthCode(fitLength(ccResponse.getAuthCode(), 9));
			if (null != ccResponse.getCardType())
				auditTrial.setCardType(fitLength(ccResponse.getCardType().toString(), 1));
			if (null != ccResponse.getCardHolderVerificationCode())
				auditTrial.setCardHolderVerCode(
						fitLengthForCardHolderVeriCode(ccResponse.getCardHolderVerificationCode(), 1));
			if (null != ccResponse.getMaskedCardNumber())
				auditTrial.setPaymentAccountNbr(fitLength(ccResponse.getMaskedCardNumber(), 25));
			if (null != ccResponse.getCvvVerificationCode()) {
				auditTrial.setCvvVerificationCode(fitLength(ccResponse.getCvvVerificationCode(), 1));
				//blenagh 7/20/2018 - CVV
				auditTrial.setCvvFlag(1);
			}
			if (ccResponse.getCvvProcessingFlag() > 0)
				auditTrial.setCvvFlag(ccResponse.getCvvProcessingFlag());
			if (null != ccResponse.getAddressVerificationCode())
				auditTrial.setAdrVerificationCode(fitLength(ccResponse.getAddressVerificationCode(), 1));
			if (ccResponse.getAddressProcessingFlag() > 0)
				auditTrial.setAdrFlag(ccResponse.getAddressProcessingFlag());
		}
		if (null != ppResponse) {

			if (null != ppResponse.getVendorOrderId())
				auditTrial.setVendorOrderId(fitLength(ppResponse.getVendorOrderId(), 40));

			if (null != ppResponse.getVendorErrorNo()) {
				auditTrial.setVendorTraCode(ppResponse.getVendorErrorNo());
				if (null != ppResponse.getVendorErrorDesc()) {
					auditTrial.setVendorTraDescription(fitLength(ppResponse.getVendorErrorDesc(), 50));
				}
				auditTrial.setVendorTraStatus(BusinessConstants.FAIL);
			} else {
				//SAdiraju 10/19/2016 updated to take care that refunds and captures update the refundStatus and paymentStatus data to transaction desc 
				if(paymentResponse.getVendorTxnDescription() != null){
					auditTrial.setVendorTraStatus(BusinessConstants.SUCCESS);
					auditTrial.setVendorTraCode("" + Status.OK.getStatusCode());
					auditTrial.setVendorTraDescription(paymentResponse.getVendorTxnDescription());
				}
				else{
					setVendorStatusFields(auditTrial);
				}
			}
			if (null != ppResponse.getEmail())
				auditTrial.setEmail(fitLength(ppResponse.getEmail(), 80));
			if (null != ppResponse.getMaxThresholdAmount())
				auditTrial.setTxnAmt(ppResponse.getMaxThresholdAmount());
		}
		if (null != altPayResponse) {
			BigDecimal oldAmount = new BigDecimal((null != altPayResponse.getOldAmount() ? altPayResponse.getOldAmount() : "0.0"));
			BigDecimal newAmount = new BigDecimal((null != altPayResponse.getAmount() ? altPayResponse.getAmount() : "0.0"));
			auditTrial.setTxnAmt(
					oldAmount.subtract(newAmount));
			if (null != altPayResponse.getCardClass())
				auditTrial.setCardType(fitLength(altPayResponse.getCardClass(), 1)); // stores
																						// card
																						// class
																						// of
																						// gift
																						// card
		}
		logger.info("Saving response to audit trial for order number : " + auditTrial.getId().getOrderNbr());
		saveAuditTrial(auditTrial, paymentResponse);
	}

	private void setVendorStatusFields(PGAuditTrialEntity auditTrial) {
		auditTrial.setVendorTraStatus(BusinessConstants.SUCCESS);
		auditTrial.setVendorTraCode("" + Status.OK.getStatusCode());
		auditTrial.setVendorTraDescription(CommonConstants.SUCCESS_DESC);
	}

	public void setStatusFields(PaymentResponse paymentResponse) {
		paymentResponse.setResponseStatus(BusinessConstants.SUCCESS);
		paymentResponse.setResponseCode("" + Status.OK.getStatusCode());
		paymentResponse.setResponseDescription(CommonConstants.SUCCESS_DESC);
	}

	public String fitLength(String strValue, int length) {
		if (strValue.length() > length)
			return strValue.substring(0, length - 1);
		else
			return strValue;
	}

	public String fitLengthForCardHolderVeriCode(String strValue, int length) {
		if (strValue.length() > length)
			return "" + strValue.charAt(length);
		else
			return strValue;
	}

	public List<PaymentToken> findPaymentInfoByCusNbr(Integer cusNbr) {
		logger.info("> In DatabaseUtils.findPaymentInfoByCusNbr() for cusNbr :" + cusNbr);
		List<PaymentToken> paymentInfoList = populatePaymentInfoDto(paymentService.findPaymentInfoByCusNbr(cusNbr));
		logger.info("< In DatabaseUtils.findPaymentInfoByCusNbr() for cusNbr :" + cusNbr);
		return paymentInfoList;
	}

	public List<PaymentToken> findPaymentInfoByOrderNbr(Integer ordNbr) {
		logger.info("> In DatabaseUtils.findPaymentInfoByOrderNbr() for ordNbr :" + ordNbr);
		List<PaymentToken> paymentInfoList = populatePaymentInfoDto(paymentService.findPaymentInfoByOrderNbr(ordNbr));
		logger.info("< In DatabaseUtils.findPaymentInfoByOrderNbr() for ordNbr :" + ordNbr);
		return paymentInfoList;
	}

	public List<PaymentToken> findPaypalPaymentInfoByOrderNbr(Integer ordNbr) {
		logger.info("> In DatabaseUtils.findPaypalPaymentInfoByOrderNbr() for ordNbr :" + ordNbr);
		List<PaymentToken> paymentInfoList = populatePaypalPaymentInfoDto(
				paymentService.findPaypalPaymentInfoByOrderNbr(ordNbr));
		logger.info("< In DatabaseUtils.findPaypalPaymentInfoByOrderNbr() for ordNbr :" + ordNbr);
		return paymentInfoList;
	}

	public List<PaymentToken> findPaymentInfoByToken(String token) {
		logger.info("> In DatabaseUtils.findPaymentInfoByToken() for token :" + token);
		List<PaymentToken> paymentInfoList = populatePaymentInfoDto(paymentService.findPaymentInfoByToken(token));
		logger.info("< In DatabaseUtils.findPaymentInfoByToken() for token :" + token);
		return paymentInfoList;
	}

	private List<PaymentToken> populatePaymentInfoDto(List<Object[]> list) {
		List<PaymentToken> entityList = new ArrayList<PaymentToken>();
		PaymentToken paymentInfoDto = null;
		for (Object[] objectRow : list) {
			paymentInfoDto = new PaymentToken();

			paymentInfoDto.setOrderNbr(new Integer(objectRow[0].toString().trim()));
			paymentInfoDto.setReleaseNbr(new Integer(objectRow[1].toString().trim()));
			paymentInfoDto.setBuNbr(new Integer(objectRow[2].toString().trim()));
			paymentInfoDto.setAuthCode(objectRow[3].toString().trim());
			paymentInfoDto.setTraDate(((BigDecimal) (objectRow[4])));
			paymentInfoDto.setTraTime((BigDecimal) (objectRow[5]));
			paymentInfoDto.setToken(objectRow[6].toString().trim());
			paymentInfoDto.setTransactionId(objectRow[7].toString().trim());
			paymentInfoDto.setPaymentMethod(objectRow[8].toString().trim());
			paymentInfoDto.setRequestedOperation(objectRow[9].toString().trim());

			entityList.add(paymentInfoDto);
		}
		return entityList;
	}

	private List<PaymentToken> populatePaypalPaymentInfoDto(List<Object[]> list) {
		List<PaymentToken> entityList = new ArrayList<PaymentToken>();
		PaymentToken paymentInfoDto = null;
		for (Object[] objectRow : list) {
			paymentInfoDto = new PaymentToken();

			paymentInfoDto.setOrderNbr(new Integer(objectRow[0].toString().trim()));
			paymentInfoDto.setReleaseNbr(new Integer(objectRow[1].toString().trim()));
			paymentInfoDto.setBuNbr(new Integer(objectRow[2].toString().trim()));
			paymentInfoDto.setAuthCode(objectRow[3].toString().trim());
			paymentInfoDto.setTransactionId(objectRow[4].toString().trim());
			paymentInfoDto.setPaymentMethod(objectRow[5].toString().trim());
			paymentInfoDto.setRequestedOperation(objectRow[6].toString().trim());
			paymentInfoDto.setVendorOrderId(objectRow[7].toString().trim());
			paymentInfoDto.setRequesterApp(objectRow[8].toString().trim());
			paymentInfoDto.setUserId(objectRow[9].toString().trim());
			paymentInfoDto.setCustomerNbr(Integer.parseInt(objectRow[10].toString().trim()));

			entityList.add(paymentInfoDto);
		}
		return entityList;
	}

	public String getFNReturnCode(String vendorTxnCode) {
		logger.info("> In DatabaseUtils.getFNReturnCode()");
		String fnReturnCode = paymentService.getFNReturnCode(vendorTxnCode);
		logger.info("< In DatabaseUtils.getFNReturnCode()");
		return fnReturnCode;
	}

	public PGTokenEntity findTokenByCrdNbr(int ordnbr, String expdate, String crdNbrLst4Digits) {
		logger.info("> In DatabaseUtils.findToken with ordnbr");
		PGTokenEntity entity = paymentService.findTokenByCrdNbr(ordnbr, expdate, crdNbrLst4Digits);
		if (null != entity)
			logger.info("< In DatabaseUtils.findToken. token :" + entity.getId().getToken().trim());
		return entity;
	}

	/**
	 * @author BLenagh 6/23/2016
	 * @param transactionId
	 * @param orderNumber
	 * @return
	 * 
	 * 		Retrieve originating authCode for AMEX/Discover captures
	 */
	public String findOriginAuthCode(String transactionId, Integer orderNumber) {
		return paymentService.findOriginAuthCode(transactionId, orderNumber);
	}
	
	/**
	 * @author SAdiraju 7/12/2016
	 * @param configId
	 * 
	 * @return
	 * 
	 * 		Retrieve the config source based on the config id
	 */
	public String findConfigSource(BigDecimal configId) {
		return paymentService.findConfigSource(configId);
	}
	
	/**
	 * @author BLenagh
	 * 9/21/2016
	 * 
	 * Fix PayPal issue where
	 * full state is returned
	 * 
	 * @param zipCode
	 * @return
	 */
	public String findStateForZip(String zipCode) {
		return paymentService.findStateForZip(zipCode);
	}

	
	/**
	 * @author gsrinivas 01/08/2019: Breakfix INC0095773
	 * 
	 * @param tokenRequest
	 * @return
	 */
	public PaymentToken findPaymentDetails(PaymentToken tokenRequest) {
		// TODO Auto-generated method stub
		PaymentToken tokenResponse = new PaymentToken();
		PGAuditTrialEntity entity = paymentService.findOrderPaymentDetails(tokenRequest.getOrderNbr(), tokenRequest.getBuNbr(), tokenRequest.getRequesterApp(),
				tokenRequest.getRequestedOperation(), tokenRequest.getPaymentMethod());
		tokenResponse = convertEnitytoTokeninfo(entity);
		return tokenResponse;
	}

	
	/**
	 * @author gsrinivas 01/08/2019: Breakfix INC0095773
	 * 
	 * @param entity
	 * @return
	 */
	private PaymentToken convertEnitytoTokeninfo(PGAuditTrialEntity entity) {
		PaymentToken pmttkninfo = new PaymentToken();
		
		if (entity.getId().getPaymentMethod().trim().equalsIgnoreCase(BusinessConstants.PAYPAL) ||
				entity.getId().getPaymentMethod().trim().equalsIgnoreCase(BusinessConstants.PAYPAL_CREDIT)) {
			pmttkninfo.setTransactionId(entity.getTransationId().trim());
			pmttkninfo.setVendorOrderId(entity.getVendorOrderId().trim());
		}else {
			// This sets the GiftCard Acc#
			pmttkninfo.setToken(entity.getPaymentAccountNbr().trim());
		}
		
		return pmttkninfo;
	}
	
	/**
	 * Author : nsharma0, 01/27/2020, Transaction History TDD
	 * @param requestDTO
	 * @return TransactionHistoryResponse
	 */
	public TransactionHistoryResponse getTransactionHistory(TransactionHistoryRequestDTO requestDTO) {
		// TODO Auto-generated method stub
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
	
	//bpandurang 4/20/2020 find void auth status
	public String findVoidAuthStatus(PaymentToken tokenRequest) {
		logger.info("> In DatabaseUtils.findVoidAuthStatus()");
		String result = paymentService.findVoidAuthStatus(tokenRequest.getToken(), tokenRequest.getOrderNbr(),
				tokenRequest.getBuNbr(), tokenRequest.getCcExpDate());
		logger.info("< In DatabaseUtils.findVoidAuthStatus()");
		return result;
	}

    public FinanceCheckBatch validateCheck(String batchNumber, String checkNumber) {
        logger.info("> In DatabaseUtils.validateCheck()");
        FinanceCheckBatch result = paymentService.validateCheck(batchNumber, checkNumber);
        logger.info("< Leaving DatabaseUtils.validateCheck()");
        return result;
    }
	
	/**
	 * 7/7/2020
	 * @author blenagh
	 * @param request
	 * @return
	 */
	public List<Object[]> getOrderTransactionRecords(PaymentToken request) {
		return paymentService.getOrderTransactionRecords(request.getPaymentMethod(), request.getOrderNbr());
	}
	
	/**
	 * nsharma0, 09/25/2020, search by cc token
	 * */
	public List<Long> searchCustomerByToken(String token) {
		logger.info("> In DatabaseUtils.searchCustomerByToken with ordnbr");
		List<Long> customerNums  = paymentService.searchCustomerByToken(token);
		return customerNums;
	}

	public boolean validateAlreadyUsedCheck(String batchNumber, String checkNumber) {

        logger.info("> In DatabaseUtils.validateAlreadyUsedCheck()");
        boolean result = paymentService.validateAlreadyUsedCheck(batchNumber, checkNumber);
        logger.info("< Leaving DatabaseUtils.validateAlreadyUsedCheck()");
		
		return result;
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
