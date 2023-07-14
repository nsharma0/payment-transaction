package com.oriental.paymentgatewayclient.service.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oriental.customer.payment.business.constants.URLMappingConstants;
import com.oriental.paymentgatewayclient.exceptions.PaymentGatewayClientException;
import com.oriental.paymentgatewayclient.service.PaymentGatewayClientService;
import com.oriental.paymentgatewayclient.utils.CommonConstants;
import com.oriental.paymentgatewaypayload.dto.web.CheckPaymentDTO;
import com.oriental.paymentgatewaypayload.dto.web.CreditCardValidationRequest;
import com.oriental.paymentgatewaypayload.dto.web.CreditCardValidationResponse;
import com.oriental.paymentgatewaypayload.dto.web.MerchandiseCertificateRequestDTO;
import com.oriental.paymentgatewaypayload.dto.web.MerchandiseCertificateResponseDTO;
import com.oriental.paymentgatewaypayload.dto.web.OrderInfo;
import com.oriental.paymentgatewaypayload.dto.web.PaymentRequest;
import com.oriental.paymentgatewaypayload.dto.web.PaymentResponse;
import com.oriental.paymentgatewaypayload.dto.web.PaymentToken;
import com.oriental.paymentgatewaypayload.dto.web.PendingPayments;
import com.oriental.paymentgatewaypayload.dto.web.TransactionDetailRequestDTO;
import com.oriental.paymentgatewaypayload.dto.web.TransactionHistoryRequestDTO;
import com.oriental.paymentgatewaypayload.dto.web.TransactionHistoryResponse;
import com.oriental.paymentgatewaypayload.dto.web.VoidMerchandiseCertificateRequestDTO;
import com.oriental.paymentgatewaypayload.enums.CVVVerificationCodesEnum;
import com.oriental.paymentgatewaypayload.enums.EndPointEnum;
import com.oriental.paymentgatewaypayload.utils.BusinessConstants;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * @author Jaya
 * 
 * sveeramalla : added utilies methods to deal with payment releated tables
 * 
 */
@Service("paymentGatewayClientService")
public class PaymentGatewayClientServiceImpl implements PaymentGatewayClientService {
	private static Logger logger = Logger.getLogger(PaymentGatewayClientServiceImpl.class);
	private  ObjectMapper objectMapper = new ObjectMapper();
	//blenagh 5/5/2017 - default client timeout
	private static Integer MAX_CONNECTION_TIME_OUT =  45000;
	private String endpointurl;
	
	private String getEndpointurl() {
		return endpointurl;
	}
	
	public PaymentGatewayClientServiceImpl() {
		super();
	}

	public void setEndPointUrl(String url){
		this.endpointurl = url;
	}

	public PaymentGatewayClientService getInstance(EndPointEnum endpoint){
		return new PaymentGatewayClientServiceImpl(endpoint);
	}
	
	public String setEndpointurl(EndPointEnum choiceEnum) {
		switch (choiceEnum) {
		case DEV:
			endpointurl = CommonConstants.DEV_URL;
			break;
		case SECUREDEV:
			endpointurl = CommonConstants.SECUREDEV_URL;
			break;
		case TEST:
			endpointurl = CommonConstants.TEST_URL;
			break;
		case SECURETEST:
			endpointurl = CommonConstants.SECURETEST_URL;
			break;
		case STAGE:
			endpointurl = CommonConstants.STAGE_URL;
			break;
		case SECURESTAGE:
			endpointurl = CommonConstants.SECURESTAGE_URL;
			break;
		case PROD:
			endpointurl = CommonConstants.PROD_URL;
			break;			
		case LOCAL:
			endpointurl = CommonConstants.LOCAL_URL;
			break;
		case SECUREQA4:
			endpointurl = CommonConstants.SECUREQA4_URL;
			break;			
		case SECUREQA5:
			endpointurl = CommonConstants.SECUREQA5_URL;
			break;
		case SECUREQA6:
			endpointurl = CommonConstants.SECUREQA6_URL;
			break;			
		default:
			logger.info("Enter the environment value for Credit Card Validation");
			break;
		}
		return endpointurl;
	}
	
	/*
	 * This method hits the paymentgateway service with
	 * CreditCardValidationRequest object and returns
	 * CreditCardValidationResponse object
	 * 
	 * @param : CreditCardValidationRequest object
	 * 
	 * @return CreditCardValidationResponse object with all the fields
	 * populated, {@code null} if null CreditCardValidationResponse object
	 * output
	 */

	@Override
	public CreditCardValidationResponse validateCreditCard(CreditCardValidationRequest creditCardValidationRequest) {
		String url =getEndpointurl() + "/general/validateCreditCard";
		logger.info("In PaymentGatewayClientServiceImpl.validateCreditCardNumber for call: "+url);
		String json = "";
		CreditCardValidationResponse creditCardValidationResponse = new CreditCardValidationResponse();
		try {
			json = objectMapper.writeValueAsString(creditCardValidationRequest);
			Client client = Client.create();
			//blenagh 5/5/2017 - set client timeout
			client.setConnectTimeout(MAX_CONNECTION_TIME_OUT);
			client.setReadTimeout(MAX_CONNECTION_TIME_OUT);
			WebResource webResource = client.resource(url);
			ClientResponse response = webResource.type("application/json").accept("application/json")
					.post(ClientResponse.class, json);
			if (response.getStatus() != 200) {
				throw new PaymentGatewayClientException("Exception occured  in PaymentGatewayService: "
						+ response.getStatus() + "" + response.getClientResponseStatus().getReasonPhrase());
			}
			String jsonReply = getJsonReply(response);
			creditCardValidationResponse = objectMapper.readValue(jsonReply, CreditCardValidationResponse.class);
		} catch (JsonProcessingException e) {
			logger.error("JsonProcessingException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (IOException e) {
			logger.error("IOException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (Exception e) {
			logger.error("Exception Exception occured", e);
			throw new PaymentGatewayClientException(e);
		}
		logger.info("Credit card validation completed");
		return  creditCardValidationResponse;
	}

	/*
	 * This method hits the paymentgateway service with PaymentRequest object
	 * and returns PaymentResponse object
	 * 
	 * @param : PaymentRequest object
	 * 
	 * @return PaymentResponse object with all the fields populated, {@code
	 * null} if null PaymentResponse object output
	 */
	@Override
	public PaymentResponse makePayment(PaymentRequest paymentRequest) {
		String url = getEndpointurl() + "/general/payment";
		logger.info("In PaymentGatewayClientServiceImpl.makePayment for call: " + paymentRequest.getPaymentMethod()
				+ " " + paymentRequest.getRequestedOperation()+"to the url: "+url);
		String json = "";
		PaymentResponse paymentresponse = new PaymentResponse();
		try {
			json = objectMapper.writeValueAsString(paymentRequest);
			Client client = Client.create();
			//blenagh 5/5/2017 - set client timeout
			client.setConnectTimeout(MAX_CONNECTION_TIME_OUT);
			client.setReadTimeout(MAX_CONNECTION_TIME_OUT);
			WebResource webResource = client.resource(url);
			ClientResponse response = webResource.type("application/json").accept("application/json")
					.post(ClientResponse.class, json);
			if (response.getStatus() != 200) {
				throw new PaymentGatewayClientException("Exception occured  in PaymentGatewayService: "
						+ response.getStatus() + "" + response.getClientResponseStatus().getReasonPhrase());
			}
			String jsonReply = getJsonReply(response);
			paymentresponse = objectMapper.readValue(jsonReply, PaymentResponse.class);
			
			//blenagh 8/30/2018 - CVV, void auth if CVV fails
			if (paymentRequest.getPaymentMethod().equalsIgnoreCase(BusinessConstants.CREDIT_CARD)
					&& paymentRequest.getRequestedOperation().equalsIgnoreCase(BusinessConstants.AUTH)
					&& paymentRequest.getCcInfo() != null
					&& paymentRequest.getCcInfo().getCvv() != null
					&& paymentresponse.getResponseStatus().equalsIgnoreCase(BusinessConstants.SUCCESS)
					&& paymentresponse.getVendorTxnStatus().equalsIgnoreCase(BusinessConstants.SUCCESS)
					&& paymentresponse.getCcResponse() != null
					&& paymentresponse.getCcResponse().getCvvVerificationCode() != null
					&& paymentresponse.getCcResponse().getCvvVerificationCode()
					.equalsIgnoreCase(CVVVerificationCodesEnum.N.getValue())) {
				PaymentRequest voidRequest = new PaymentRequest();
				voidRequest.setBusinessUnit(paymentRequest.getBusinessUnit());
				voidRequest.setTransactionAmount(paymentRequest.getTransactionAmount());
				voidRequest.setCurrencyCode(paymentRequest.getCurrencyCode());
				voidRequest.setCustomerNumber(paymentRequest.getCustomerNumber());
				voidRequest.setPaymentMethod(paymentRequest.getPaymentMethod());
				voidRequest.setUserId(paymentRequest.getUserId());
				voidRequest.setRequestedOperation(BusinessConstants.VOID);
				voidRequest.setRequesterApp(paymentRequest.getRequesterApp());
				voidRequest.setTransactionId(paymentresponse.getTransactionId());
				voidRequest.setStationId(paymentRequest.getStationId());
				if (paymentRequest.getOrderInfo() != null) {
					voidRequest.setOrderInfo(new OrderInfo());		
					voidRequest.getOrderInfo().setOrderNumber(paymentRequest.getOrderInfo().getOrderNumber());
					voidRequest.getOrderInfo().setReleaseNumber(paymentRequest.getOrderInfo().getReleaseNumber());
				}
				
				PaymentResponse voidResponse = null;
				try {
					voidResponse = this.makePayment(voidRequest);
				} catch (PaymentGatewayClientException e) {
					//if VOID fails for some reason, allow the AUTH response to flow through
					logger.error("VOID call failed on CVV mismatch", e);					
				}
				
				if (voidResponse != null && voidResponse.getResponseStatus().equalsIgnoreCase(BusinessConstants.SUCCESS)
						&& voidResponse.getVendorTxnStatus().equalsIgnoreCase(BusinessConstants.SUCCESS)) {
					//if void succeeds, change response back to consumer to a vendor fail for the auth
					paymentresponse.setVendorTxnStatus(BusinessConstants.FAIL);
					paymentresponse.setVendorTxnCode(BusinessConstants.D2020);
					paymentresponse.setVendorTxnDescription(BusinessConstants.CVVFailed);
					paymentresponse.getCcResponse().setCardHolderVerificationCode("2");
				}
			}
		} catch (JsonProcessingException e) {
			logger.error("JsonProcessingException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (IOException e) {
			logger.error("IOException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (Exception e) {
			logger.error("Exception Exception occured", e);
			throw new PaymentGatewayClientException(e);
		}
		logger.info(paymentRequest.getPaymentMethod() + " " + paymentRequest.getRequestedOperation()
				+ " is completed with transactionId : " + paymentresponse.getTransactionId());
		
		return paymentresponse;

	}
	
	/*
	 * This method hits the paymentgateway service to get the list of Business units
	 * 
	 * 
	 * @return ArrayList object with all the business units
	 * populated, {@code null} 
	 */

	@SuppressWarnings("unchecked")
	@Override
	public List<Integer> getAllBUs() {
		String url = getEndpointurl() + "/utils/getAllBUs";
		logger.info("> In PaymentGatewayClientServiceImpl.getAllBUs for call: " + url);
		List<Integer> buList = null;
		try {
			Client client = Client.create();
			//blenagh 5/5/2017 - set client timeout
			client.setConnectTimeout(MAX_CONNECTION_TIME_OUT);
			client.setReadTimeout(MAX_CONNECTION_TIME_OUT);
			WebResource webResource = client.resource(url);
			ClientResponse response = webResource.type("application/json").accept("application/json")
					.get(ClientResponse.class);
			if (response.getStatus() != 200) {
				throw new PaymentGatewayClientException("Exception occured  in PaymentGatewayService: "
						+ response.getStatus() + "" + response.getClientResponseStatus().getReasonPhrase());
			}
			String jsonReply = getJsonReply(response);
			buList = objectMapper.readValue(jsonReply, List.class);
		} catch (JsonProcessingException e) {
			logger.error("JsonProcessingException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (IOException e) {
			logger.error("IOException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (Exception e) {
			logger.error("Exception Exception occured", e);
			throw new PaymentGatewayClientException(e);
		}
		logger.info("< PaymentGatewayClientServiceImpl.getAllBUs completed");
		return  buList;
	}
	
	/*
	 * This method hits the paymentgateway service for getting switch status
	 * 
	 * Accepts businessUnit and paymentType in the request url
	 * 
	 * @return String as switch status
	 */

	@Override
	public String getSwitchStatus(String businessUnit, String paymentType) {
		String url = getEndpointurl() + "/utils/getSwitchStatus";
		if(null != businessUnit && null != paymentType)
			url += "?Business%20Unit=" + businessUnit + "&Payment%20Type="+ paymentType;
		logger.info("In PaymentGatewayClientServiceImpl.getSwitchStatus for call: "+url);
		String status = null;
		try {
			Client client = Client.create();
			//blenagh 5/5/2017 - set client timeout
			client.setConnectTimeout(MAX_CONNECTION_TIME_OUT);
			client.setReadTimeout(MAX_CONNECTION_TIME_OUT);
			WebResource webResource = client.resource(url);
			ClientResponse response = webResource.type("application/json").accept("application/json")
					.get(ClientResponse.class);
			if (response.getStatus() != 200) {
				throw new PaymentGatewayClientException("Exception occured  in PaymentGatewayService: "
						+ response.getStatus() + "" + response.getClientResponseStatus().getReasonPhrase());
			}
			String jsonReply = getJsonReply(response);
			status = objectMapper.readValue(jsonReply, String.class);
		} catch (JsonProcessingException e) {
			logger.error("JsonProcessingException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (IOException e) {
			logger.error("IOException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (Exception e) {
			logger.error("Exception Exception occured", e);
			throw new PaymentGatewayClientException(e);
		}
		logger.info("GetSwitchStatus completed for businessUnit " + businessUnit + " and paymentType : " + paymentType );
		return  status;
	}
	
	
	public String getMasterSwitchStatus(){
		return getSwitchStatus(null,null);
	}
	
	
	/*
	 * This method hits the paymentgateway service for getting token Info
	 * 
	 * Accepts businessUnit and token in the request url
	 * 
	 * @return PaymentToken information
	 */

	@Override
	public PaymentToken getTokenInfo(String token, String businessUnit) {
		
		StringBuffer url = new StringBuffer(getEndpointurl() + "/utils/getTokenInfo");
		url.append("?token=").append(token);
		if(null != businessUnit)
			url.append("&businessUnit=").append(businessUnit);
		logger.info("In PaymentGatewayClientServiceImpl.getTokenInfo for call: "+url);
		PaymentToken tokenInfo = null;
		try {
			Client client = Client.create();
			//blenagh 5/5/2017 - set client timeout
			client.setConnectTimeout(MAX_CONNECTION_TIME_OUT);
			client.setReadTimeout(MAX_CONNECTION_TIME_OUT);
			WebResource webResource = client.resource(url.toString());
			ClientResponse response = webResource.type("application/json").accept("application/json")
					.get(ClientResponse.class);
			if (response.getStatus() != 200) {
				throw new PaymentGatewayClientException("Exception occured  in PaymentGatewayService: "
						+ response.getStatus() + "" + response.getClientResponseStatus().getReasonPhrase());
			}
			String jsonReply = getJsonReply(response);
			tokenInfo = objectMapper.readValue(jsonReply, PaymentToken.class);
		} catch (JsonProcessingException e) {
			logger.error("JsonProcessingException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (IOException e) {
			logger.error("IOException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (Exception e) {
			logger.error("Exception Exception occured", e);
			throw new PaymentGatewayClientException(e);
		}
		logger.info("getTokenInfo completed for businessUnit " + businessUnit);
		return  tokenInfo;
	}
	
	/*
	 * This method hits the paymentgateway service for getting payment Info
	 * 
	 * Accepts customerNbr/orderNbr/token in the request body.
	 * Not exposing this method out side so that no one will mess up with the actual purpose. 
	 * @return PaymentToken information
	 */

	public List<PaymentToken> getPaymentInfo(PaymentToken request) {
		
		StringBuffer url = new StringBuffer(getEndpointurl() + "/utils/getPaymentInfo");
		String json = "";
		logger.info("In PaymentGatewayClientServiceImpl.getPaymentInfo for call: "+url);
		List<PaymentToken> paymentInfoList = new ArrayList<PaymentToken>();
		try {
			json = objectMapper.writeValueAsString(request);
			Client client = Client.create();
			//blenagh 5/5/2017 - set client timeout
			client.setConnectTimeout(MAX_CONNECTION_TIME_OUT);
			client.setReadTimeout(MAX_CONNECTION_TIME_OUT);
			WebResource webResource = client.resource(url.toString());
			ClientResponse response = webResource.type("application/json").accept("application/json")
					.post(ClientResponse.class, json);
			if (response.getStatus() != 200) {
				throw new PaymentGatewayClientException("Exception occured  in PaymentGatewayService: "
						+ response.getStatus() + "" + response.getClientResponseStatus().getReasonPhrase());
			}
			String jsonReply = getJsonReply(response);
			paymentInfoList = objectMapper.readValue(jsonReply, new TypeReference<List<PaymentToken>>(){});
		} catch (JsonProcessingException e) {
			logger.error("JsonProcessingException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (IOException e) {
			logger.error("IOException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (Exception e) {
			logger.error("Exception Exception occured", e);
			throw new PaymentGatewayClientException(e);
		}
		logger.info("getPaymentInfo completed with " + paymentInfoList.size() + " records");
		return  paymentInfoList;
	}
	
	@Override
	public List<PaymentToken> getPaymentInfoByToken(String token) {
		logger.info("In PaymentGatewayClientServiceImpl.getPaymentInfoByToken");
		PaymentToken request = new PaymentToken();
		request.setToken(token);
		return getPaymentInfo(request);
	}
	
	@Override
	public List<PaymentToken> getPaymentInfoByOrderNbr(int orderNbr) {
		logger.info("In PaymentGatewayClientServiceImpl.getPaymentInfoByOrderNbr");
		PaymentToken request = new PaymentToken();
		request.setOrderNbr(orderNbr);
		return getPaymentInfo(request);
	}
	//sadiraju 07/11/2016 method to get order details for paypal orders.
	@Override
	public List<PaymentToken> getPaymentInfoByOrderNbrAndPaymentMethod(int orderNbr) {
		logger.info("In PaymentGatewayClientServiceImpl.getPaymentInfoByOrderNbrAndPaymentMethod");
		PaymentToken request = new PaymentToken();
		request.setOrderNbr(orderNbr);
		request.setPaymentMethod("PP");
		return getPaymentInfo(request);
	}
	@Override
	public List<PaymentToken> getPaymentInfoByCusNbr(int cusNbr) {
		logger.info("In PaymentGatewayClientServiceImpl.getPaymentInfoByCusNbr");
		PaymentToken request = new PaymentToken();
		request.setCustomerNbr(cusNbr);
		return getPaymentInfo(request);
	}
	
	/*
	 * This method hits the paymentgateway service for getting Auth status
	 * 
	 * Accepts  tokenRequest
	 * 
	 * @return Status string
	 */

	 @Override
	public String findAuthStatus(PaymentToken tokenRequest) {
		String url = getEndpointurl() + "/utils/getAuthStatus";
		logger.info("In PaymentGatewayClientServiceImpl.findAuthStatus for call: " + url);
		String json = "";
		String status = null;
		try {
			json = objectMapper.writeValueAsString(tokenRequest);
			Client client = Client.create();
			//blenagh 5/5/2017 - set client timeout
			client.setConnectTimeout(MAX_CONNECTION_TIME_OUT);
			client.setReadTimeout(MAX_CONNECTION_TIME_OUT);
			WebResource webResource = client.resource(url);
			ClientResponse response = webResource.type("application/json").accept("application/json")
					.post(ClientResponse.class, json);
			if (response.getStatus() != 200) {
				throw new PaymentGatewayClientException("Exception occured  in PaymentGatewayService: "
						+ response.getStatus() + "" + response.getClientResponseStatus().getReasonPhrase());
			}
			String jsonReply = getJsonReply(response);
			status = objectMapper.readValue(jsonReply, String.class);
		} catch (JsonProcessingException e) {
			logger.error("JsonProcessingException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (IOException e) {
			logger.error("IOException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (Exception e) {
			logger.error("Exception Exception occured", e);
			throw new PaymentGatewayClientException(e);
		}
		logger.info("findAuthStatus call completed");
		return status;
	}

	 
	 /*
		 * This method hits the paymentgateway service for getting PaypalAuth status
		 * 
		 * Accepts  orderNbr
		 * 
		 * @return Status string
		 */

		 @Override
		public String findPayPalAuthStatus(PaymentToken tokenRequest) {
			 String url = getEndpointurl() + "/utils/getPayPalAuthStatus";
			logger.info("In PaymentGatewayClientServiceImpl.findPaypalAuthStatus for call: " + url);
			String json = "";
			String status = null;
			try {
				json = objectMapper.writeValueAsString(tokenRequest);
				Client client = Client.create();
				//blenagh 5/5/2017 - set client timeout
				client.setConnectTimeout(MAX_CONNECTION_TIME_OUT);
				client.setReadTimeout(MAX_CONNECTION_TIME_OUT);
				WebResource webResource = client.resource(url);
				ClientResponse response = webResource.type("application/json").accept("application/json")
						.post(ClientResponse.class, json);
				if (response.getStatus() != 200) {
					throw new PaymentGatewayClientException("Exception occured  in PaymentGatewayService: "
							+ response.getStatus() + "" + response.getClientResponseStatus().getReasonPhrase());
				}
				String jsonReply = getJsonReply(response);
				status = objectMapper.readValue(jsonReply, String.class);
			} catch (JsonProcessingException e) {
				logger.error("JsonProcessingException Exception occured", e);
				throw new PaymentGatewayClientException(e);
			} catch (IOException e) {
				logger.error("IOException Exception occured", e);
				throw new PaymentGatewayClientException(e);
			} catch (Exception e) {
				logger.error("Exception Exception occured", e);
				throw new PaymentGatewayClientException(e);
			}
			logger.info("findAuthStatus call completed");
			return status;
		}

	@Override
	public PaymentToken getTokenInfo(String token) {
		return getTokenInfo(token, null);
	}
	
	
	/**
	 * This method hits the paymentgateway service with PendingPayments object
	 * for saves a record in pending payments table
	 * 
	 * @param : PendingPayments object
	 * 
	 * @return String object
	 */

    @Override
	public String savePendingPayments(PendingPayments pendingPaymentsRequest) {
		String url =getEndpointurl() + "/utils/savePendingPayments";
		logger.info("In PaymentGatewayClientServiceImpl.savePendingPayments for call: "+url);
		String json = "";
		String status = null;
		try {
			json = objectMapper.writeValueAsString(pendingPaymentsRequest);
			Client client = Client.create();
			//blenagh 5/5/2017 - set client timeout
			client.setConnectTimeout(MAX_CONNECTION_TIME_OUT);
			client.setReadTimeout(MAX_CONNECTION_TIME_OUT);
			WebResource webResource = client.resource(url);
			ClientResponse response = webResource.type("application/json").accept("application/json")
					.post(ClientResponse.class, json);
			if (response.getStatus() != 200) {
				throw new PaymentGatewayClientException("Exception occured  in PaymentGatewayService: "
						+ response.getStatus() + "" + response.getClientResponseStatus().getReasonPhrase());
			}
			String jsonReply = getJsonReply(response);
			status = objectMapper.readValue(jsonReply, String.class);
		} catch (JsonProcessingException e) {
			logger.error("JsonProcessingException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (IOException e) {
			logger.error("IOException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (Exception e) {
			logger.error("Exception Exception occured", e);
			throw new PaymentGatewayClientException(e);
		}
		logger.info("saving to pendingpayments completed");
		return  status;
	}
	/**
	 * @author SAdiraju
	 * 6/23/2016
	 * 
	 * Update the payment gateway transaction audit table with the customer number from COMET in the paymenttokenrequest
	 */
    @Override
   	public String saveCustomerNumberToAuditTrail(PaymentToken paymentTokenRequest) {
   		String url =getEndpointurl() + "/utils/saveCustomerNumberToAuditTrail";
   		logger.info("In PaymentGatewayClientServiceImpl.saveCustomerNumberToAuditTrail for call: "+url);
   		String json = "";
   		String status = null;
   		try {
   			json = objectMapper.writeValueAsString(paymentTokenRequest);
   			Client client = Client.create();
   			//blenagh 5/5/2017 - set client timeout
   			client.setConnectTimeout(MAX_CONNECTION_TIME_OUT);
   			client.setReadTimeout(MAX_CONNECTION_TIME_OUT);
   			WebResource webResource = client.resource(url);
   			ClientResponse response = webResource.type("application/json").accept("application/json")
   					.post(ClientResponse.class, json);
   			if (response.getStatus() != 200) {
   				throw new PaymentGatewayClientException("Exception occured  in PaymentGatewayService: "
   						+ response.getStatus() + "" + response.getClientResponseStatus().getReasonPhrase());
   			}
   			String jsonReply = getJsonReply(response);
   			status = objectMapper.readValue(jsonReply, String.class);
   		} catch (JsonProcessingException e) {
   			logger.error("JsonProcessingException Exception occured", e);
   			throw new PaymentGatewayClientException(e);
   		} catch (IOException e) {
   			logger.error("IOException Exception occured", e);
   			throw new PaymentGatewayClientException(e);
   		} catch (Exception e) {
   			logger.error("Exception Exception occured", e);
   			throw new PaymentGatewayClientException(e);
   		}
   		logger.info("saving to saveCustomerNumberToAuditTrail completed");
   		return  status;
   	}
    /**
	 * This method hits the paymentgateway service with
	 * PendingPayments object for deleting a record in pending payments table
	 * 
	 * @param : PendingPayments object
	 * 
	 * @return String object
	 */

    @Override
	public String deletePendingPayments(PendingPayments pendingPaymentsRequest) {
		String url =getEndpointurl() + "/utils/deletePendingPayments";
		logger.info("In PaymentGatewayClientServiceImpl.deletePendingPayments for call: "+url);
		String json = "";
		String status = null;
		try {
			json = objectMapper.writeValueAsString(pendingPaymentsRequest);
			Client client = Client.create();
			//blenagh 5/5/2017 - set client timeout
			client.setConnectTimeout(MAX_CONNECTION_TIME_OUT);
			client.setReadTimeout(MAX_CONNECTION_TIME_OUT);
			WebResource webResource = client.resource(url);
			ClientResponse response = webResource.type("application/json").accept("application/json")
					.delete(ClientResponse.class, json);
			if (response.getStatus() != 200) {
				throw new PaymentGatewayClientException("Exception occured  in PaymentGatewayService: "
						+ response.getStatus() + "" + response.getClientResponseStatus().getReasonPhrase());
			}
			String jsonReply = getJsonReply(response);
			status = objectMapper.readValue(jsonReply, String.class);
		} catch (JsonProcessingException e) {
			logger.error("JsonProcessingException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (IOException e) {
			logger.error("IOException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (Exception e) {
			logger.error("Exception Exception occured", e);
			throw new PaymentGatewayClientException(e);
		}
		logger.info("deleting from deletePendingPayments completed");
		return  status;
	}
    
    //sadiraju 01/26/2017
	//return an encrypted string for the credit card 
	//The web could pass in any string and we would end up encrypting it
	public String getEncryptedData(String dataToBeEncrypted) {
		String encryptedString="";	
		StringBuffer url = new StringBuffer(getEndpointurl() + "/utils/getEncryptedData");
		logger.info("In PaymentGatewayClientServiceImpl.getEncryptedData");
		try {
			url.append("?data=").append(URLEncoder.encode(dataToBeEncrypted, "UTF-8"));			
			
			Client client = Client.create();
			//blenagh 5/5/2017 - set client timeout
			client.setConnectTimeout(MAX_CONNECTION_TIME_OUT);
			client.setReadTimeout(MAX_CONNECTION_TIME_OUT);
			WebResource webResource = client.resource(url.toString());
			ClientResponse response = webResource.type("application/json").accept("application/json")
					.get(ClientResponse.class);
			if (response.getStatus() != 200) {
				throw new PaymentGatewayClientException("Exception occured  in PaymentGatewayService: "
						+ response.getStatus() + "" + response.getClientResponseStatus().getReasonPhrase());
			}
			String jsonReply = getJsonReply(response);
			encryptedString = objectMapper.readValue(jsonReply, String.class);
		} catch (JsonProcessingException e) {
			logger.error("JsonProcessingException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (UnsupportedEncodingException e) {
			logger.error("UnsupportedEncodingException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (IOException e) {
			logger.error("IOException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (Exception e) {
			logger.error("Exception Exception occured", e);
			throw new PaymentGatewayClientException(e);
		}
		logger.info("getEncryptedData completed");
		return encryptedString;
		
	}
	
	/**
	 * @author gsrinivas 01/08/2019
	 * Breakfix: INC0095773
	 * 
	 * Client method retreives the PGWTRAADT info of PayPal / EGC payments
	 * 
	 * @param request
	 * @return
	 */
	@Override
	public PaymentToken getPaymentDetailsForVoid(PaymentToken request) {
		StringBuffer url = new StringBuffer(getEndpointurl() + "/utils/getPaymentDetailsForVoid");
		String json = "";
		PaymentToken tokenInfo =  new PaymentToken();
		logger.info("In PaymentGatewayClientServiceImpl.getPaymentDetailsForVoid() for call: "+url);
		try {
			json = objectMapper.writeValueAsString(request);
			Client client = Client.create();
			client.setConnectTimeout(MAX_CONNECTION_TIME_OUT);
			client.setReadTimeout(MAX_CONNECTION_TIME_OUT);
			WebResource webResource = client.resource(url.toString());
			ClientResponse response = webResource.type("application/json").accept("application/json")
					.post(ClientResponse.class, json);
			if (response.getStatus() != 200) {
				throw new PaymentGatewayClientException("Exception occured  in PaymentGatewayService in getPaymentDetailsForVoid(): "
						+ response.getStatus() + "" + response.getClientResponseStatus().getReasonPhrase());
			}
			String jsonReply = getJsonReply(response);
			tokenInfo = objectMapper.readValue(jsonReply, PaymentToken.class);
		} catch (JsonProcessingException e) {
			logger.error("JsonProcessingException Exception occured in getPaymentDetailsForVoid()", e);
			throw new PaymentGatewayClientException(e);
		} catch (IOException e) {
			logger.error("IOException Exception occured in getPaymentDetailsForVoid()", e);
			throw new PaymentGatewayClientException(e);
		} catch (Exception e) {
			logger.error("Exception Exception occured in getPaymentDetailsForVoid()", e);
			throw new PaymentGatewayClientException(e);
		}
		
		logger.info("getPaymentDetailsForVoid() is completed");
		return tokenInfo;
	}

	//sadiraju 01/30/2017
	//returns true if the input contains all digits
	//used by the web team to check for credit card numbers
	public boolean isInteger(final String tokenId) {
		return (StringUtils.isNotEmpty(tokenId) && tokenId.matches("\\d+")) ;
	} 
	
	public PaymentGatewayClientServiceImpl(EndPointEnum choiceEnum) {
		super();
		this.endpointurl = setEndpointurl(choiceEnum);
	}

	/*This method takes the object in the String format and convert that into JSON object
	 * @param : Response in String format
	 * @return  Response in Json format,
     * {@code null} if null String input 
	 */
	private String getJsonReply(ClientResponse response){
		String reply = response.getEntity(String.class);
		int start = StringUtils.indexOf(reply, "entity");
		int endIndex = StringUtils.indexOf(reply, "entityType");
		String jsonReply = StringUtils.substring(reply, start + 8, endIndex - 2);
		return jsonReply;
		
	}

	/**
	 * 5/4/2017
	 * blenagh
	 * Adding client timeout
	 */
	@Override
	public void setTimeout(int timeout) {
		MAX_CONNECTION_TIME_OUT = timeout;
	}

	@Override
	public MerchandiseCertificateResponseDTO getMerchandiseCertificatesForCustomer(
			MerchandiseCertificateRequestDTO merchandiseCertificateRequestDTO) {
		String url = getEndpointurl() + "/utils/getMerchandiseCertificatesForCustomer";
		logger.info("In PaymentGatewayClientServiceImpl.getMerchandiseCertificatesForCustomer for call: " + url);
		String json = "";
		MerchandiseCertificateResponseDTO merchandiseCertificateResponseDTO = null;
		try {
			json = objectMapper.writeValueAsString(merchandiseCertificateRequestDTO);
			Client client = Client.create();
			client.setConnectTimeout(MAX_CONNECTION_TIME_OUT);
			client.setReadTimeout(MAX_CONNECTION_TIME_OUT);
			WebResource webResource = client.resource(url);
			ClientResponse response = webResource.type("application/json").accept("application/json")
					.post(ClientResponse.class, json);
			if (response.getStatus() != 200) {
				throw new PaymentGatewayClientException("Exception occured  in PaymentGatewayService: "
						+ response.getStatus() + "" + response.getClientResponseStatus().getReasonPhrase());
			}
			String jsonReply = getJsonReply(response);
			merchandiseCertificateResponseDTO = objectMapper.readValue(jsonReply, MerchandiseCertificateResponseDTO.class);
		} catch (JsonProcessingException e) {
			logger.error("JsonProcessingException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (IOException e) {
			logger.error("IOException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (Exception e) {
			logger.error("Exception Exception occured", e);
			throw new PaymentGatewayClientException(e);
		}
		logger.info("getMerchandiseCertificatesForCustomer call completed");
		return merchandiseCertificateResponseDTO;
	}

	@Override
	public String voidMerchCertificates(VoidMerchandiseCertificateRequestDTO voidMerchandiseCertificateRequestDTO) {
		String url = getEndpointurl() + "/utils/voidMerchandiseCertificates";
		logger.info("In PaymentGatewayClientServiceImpl.voidMerchandiseCertificates for call: " + url);
		String json = "";
		String status = null;
		try {
			json = objectMapper.writeValueAsString(voidMerchandiseCertificateRequestDTO);
			Client client = Client.create();
			client.setConnectTimeout(MAX_CONNECTION_TIME_OUT);
			client.setReadTimeout(MAX_CONNECTION_TIME_OUT);
			WebResource webResource = client.resource(url);
			ClientResponse response = webResource.type("application/json").accept("application/json")
					.post(ClientResponse.class, json);
			if (response.getStatus() != 200) {
				throw new PaymentGatewayClientException("Exception occured  in PaymentGatewayService: "
						+ response.getStatus() + "" + response.getClientResponseStatus().getReasonPhrase());
			}
			String jsonReply = getJsonReply(response);
			status = objectMapper.readValue(jsonReply, String.class);
		} catch (JsonProcessingException e) {
			logger.error("JsonProcessingException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (IOException e) {
			logger.error("IOException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (Exception e) {
			logger.error("Exception Exception occured", e);
			throw new PaymentGatewayClientException(e);
		}
		logger.info("voidMerchandiseCertificates call completed");
		return status;
	}
	
	/**
     * Author : nsharma0, 01/27/2020, Transaction History TDD
     * @param requestDTO
     * @return TransactionHistoryResponse
     */
	@Override
	public TransactionHistoryResponse searchTransactionHistory(TransactionHistoryRequestDTO requestDTO){
		String url = getEndpointurl() + URLMappingConstants.UTILS + URLMappingConstants.SEARCH_TRANSACTION_HISTORY;
		logger.info("In PaymentGatewayClientServiceImpl.searchTransactionHistory for call: " + url);
		TransactionHistoryResponse responseList = null;
		try {
			HttpEntity<TransactionHistoryRequestDTO> entity = new HttpEntity<TransactionHistoryRequestDTO>(requestDTO);
			RestTemplate client = new RestTemplate();
			String response = client.postForObject(url, entity, String.class);
			String  jsonReply = getJsonReply(response); 
			responseList = objectMapper.readValue(jsonReply, TransactionHistoryResponse.class);
		} catch (Exception e) {
			logger.error("Exception Exception occured", e);
			throw new PaymentGatewayClientException(e);
		}
		logger.info("searchTransactionHistory call completed");
		return responseList;
	}
	
	 public String getJsonReply(String reply) {
	        int start = StringUtils.indexOf(reply, "entity");
	        int endIndex = StringUtils.indexOf(reply, "entityType");
	        String jsonReply = StringUtils.substring(reply, start + 8, endIndex - 2);
	        return jsonReply;
	    }
	
	/**
     * Author : nsharma0, 01/27/2020, Transaction History TDD
     * @param transactionId
     * @return TransactionHistoryResponse
     */
	@Override
	public TransactionHistoryResponse getTransactionDetails(String transactionId){
		String url = getEndpointurl() + URLMappingConstants.UTILS + URLMappingConstants.GET_TRANSACTION_DETAILS;
		logger.info("In PaymentGatewayClientServiceImpl.getTransactionDetails for call: " + url);
		String json = "";
		TransactionHistoryResponse txnHistoryResponse = null;
		TransactionDetailRequestDTO requestDTO = new TransactionDetailRequestDTO();
		requestDTO.setTimeStamp(transactionId);
		try {
			HttpEntity<TransactionDetailRequestDTO> entity = new HttpEntity<TransactionDetailRequestDTO>(requestDTO);
			RestTemplate client = new RestTemplate();
			String response = client.postForObject(url, entity, String.class);
			String  jsonReply = getJsonReply(response); 
			txnHistoryResponse = objectMapper.readValue(jsonReply, TransactionHistoryResponse.class);
		} catch (JsonProcessingException e) {
			logger.error("JsonProcessingException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (IOException e) {
			logger.error("IOException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (Exception e) {
			logger.error("Exception Exception occured", e);
			throw new PaymentGatewayClientException(e);
		}
		logger.info("getTransactionDetails call completed");
		return txnHistoryResponse;
	}
	
	 @Override
	public String findVoidAuthStatus(PaymentToken tokenRequest) {
			String url = getEndpointurl() + URLMappingConstants.UTILS + URLMappingConstants.GET_VOID_AUTH_STATUS;
			logger.info("In PaymentGatewayClientServiceImpl.findVoidAuthStatus for call: " + url);
			String status = null;
			try {
				HttpEntity<PaymentToken> token = new HttpEntity<PaymentToken>(tokenRequest);
				RestTemplate client = new RestTemplate();
				String response = client.postForObject(url, token, String.class);
				String  jsonReply = getJsonReply(response); 
				status = objectMapper.readValue(jsonReply, String.class);
			} catch (JsonProcessingException e) {
				logger.error("JsonProcessingException Exception occured", e);
				throw new PaymentGatewayClientException(e);
			} catch (IOException e) {
				logger.error("IOException Exception occured", e);
				throw new PaymentGatewayClientException(e);
			} catch (Exception e) {
				logger.error("Exception Exception occured", e);
				throw new PaymentGatewayClientException(e);
			}
			logger.info("findVoidAuthStatus call completed");
			return status;
		}
	 
	 //bpandurang 04/24/2020 get re auth
	 @Override
		public String getReAuthForOrder(TransactionHistoryRequestDTO requestDTO){
			String url = getEndpointurl() + URLMappingConstants.UTILS + URLMappingConstants.GET_REAUTH_ORDER;
			logger.info("In PaymentGatewayClientServiceImpl.getReAuthForOrder for call: " + url);
			String responseList = null;
			try {
				HttpEntity<TransactionHistoryRequestDTO> entity = new HttpEntity<TransactionHistoryRequestDTO>(requestDTO);
				RestTemplate client = new RestTemplate();
				String response = client.postForObject(url, entity, String.class);
				String  jsonReply = getJsonReply(response); 
				responseList = objectMapper.readValue(jsonReply, String.class);
			} catch (Exception e) {
				logger.error("Exception Exception occured", e);
				throw new PaymentGatewayClientException(e);
			}
			logger.info("getReAuthForOrder call completed");
			return responseList;
		}

    @Override
    public CheckPaymentDTO validateCheck(CheckPaymentDTO request) {
        String url = getEndpointurl() + URLMappingConstants.UTILS + URLMappingConstants.VALIDATE_CHECK;
        logger.info("In PaymentGatewayClientServiceImpl.validateCheck for call: " + url);
        CheckPaymentDTO responseDTO = null;
        try {
            HttpEntity<CheckPaymentDTO> entity = new HttpEntity<CheckPaymentDTO>(request);
            RestTemplate client = new RestTemplate();
            String response = client.postForObject(url, entity, String.class);
            String jsonReply = getJsonReply(response);
            responseDTO = objectMapper.readValue(jsonReply, CheckPaymentDTO.class);
        } catch (Exception e) {
            logger.error("Exception occurred", e);
            throw new PaymentGatewayClientException(e);
        }
        logger.info("validateCheck call completed");
        return responseDTO;
    }
		
	/**
	 * 7/8/2020
	 * @author blenagh
	 */
	@Override
	public Boolean getMultiReleaseAuthStatus(PaymentToken request) {
		StringBuffer url = new StringBuffer(getEndpointurl() + "/utils/getMultiReleaseAuthStatus");
		
		Boolean authStatus = null;
		logger.info("In PaymentGatewayClientServiceImpl.getMultiReleaseAuthStatus() for call: "+url);
		try {
			String json = objectMapper.writeValueAsString(request);
			Client client = Client.create();
			client.setConnectTimeout(MAX_CONNECTION_TIME_OUT);
			client.setReadTimeout(MAX_CONNECTION_TIME_OUT);
			WebResource webResource = client.resource(url.toString());
			ClientResponse response = webResource.type("application/json").accept("application/json")
					.post(ClientResponse.class, json);
			if (response.getStatus() != 200) {
				throw new PaymentGatewayClientException("Exception occured  in PaymentGatewayService in getMultiReleaseAuthStatus(): "
						+ response.getStatus() + "" + response.getClientResponseStatus().getReasonPhrase());
			}
			String jsonReply = getJsonReply(response);
			authStatus = objectMapper.readValue(jsonReply, Boolean.class);
		} catch (JsonProcessingException e) {
			logger.error("JsonProcessingException Exception occured in getMultiReleaseAuthStatus()", e);
			throw new PaymentGatewayClientException(e);
		} catch (IOException e) {
			logger.error("IOException Exception occured in getMultiReleaseAuthStatus()", e);
			throw new PaymentGatewayClientException(e);
		} catch (Exception e) {
			logger.error("Exception Exception occured in getMultiReleaseAuthStatus()", e);
			throw new PaymentGatewayClientException(e);
		}
		
		logger.info("getMultiReleaseAuthStatus() is completed");
		return authStatus;
	}

	/**
	 * nsharma0, 09/25/2020, search order by cc token
	 * */
	@Override
	public List<Long> searchCustomerByCC(String queryBy) {
		String url = getEndpointurl() + URLMappingConstants.UTILS + URLMappingConstants.SEARCH_CUSTOMER_BY_CC;
		logger.info("In PaymentGatewayClientServiceImpl.searchCustomerByCC for call: " + url);
		List<Long> customerNumber = null;
		try {
			Client client = Client.create();
			client.setConnectTimeout(MAX_CONNECTION_TIME_OUT);
			client.setReadTimeout(MAX_CONNECTION_TIME_OUT);
			WebResource webResource = client.resource(url.toString()+"?queryBy="+queryBy);
			ClientResponse response = webResource.type("application/json").accept("application/json")
					.get(ClientResponse.class);
			if (response.getStatus() != 200) {
				throw new PaymentGatewayClientException("Exception occured  in PaymentGatewayService in searchCustomerByCC(): "
						+ response.getStatus() + "" + response.getClientResponseStatus().getReasonPhrase());
			}
			String jsonReply = getJsonReply(response);
			customerNumber = objectMapper.readValue(jsonReply, new TypeReference<List<Long>>(){});
		} catch (Exception e) {
			logger.error("Exception Exception occured", e);
			throw new PaymentGatewayClientException(e);
		}
		logger.info("searchCustomerByCC call completed");
		return customerNumber;
	}
	/**
     * Author : nbhutani,payment audit history
     * @param orderNumber
     * @return TransactionHistoryResponse
     */
	@Override
	public TransactionHistoryResponse findPGAuditInfoByOrderNbr(Integer orderNumber){
		String url = getEndpointurl() + URLMappingConstants.UTILS + URLMappingConstants.FIND_PG_AUDIT_INFO_BY_ORDNBR;
		logger.info("In PaymentGatewayClientServiceImpl.findPGAuditInfoByOrderNbr for call: " + url);
		TransactionHistoryResponse txnHistoryResponse = null;
		TransactionDetailRequestDTO requestDTO = new TransactionDetailRequestDTO();
		requestDTO.setOrderNumber(orderNumber);
		try {
			HttpEntity<TransactionDetailRequestDTO> entity = new HttpEntity<TransactionDetailRequestDTO>(requestDTO);
			RestTemplate client = new RestTemplate();
			String response = client.postForObject(url, entity, String.class);
			String  jsonReply = getJsonReply(response); 
			txnHistoryResponse = objectMapper.readValue(jsonReply, TransactionHistoryResponse.class);
		} catch (JsonProcessingException e) {
			logger.error("JsonProcessingException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (IOException e) {
			logger.error("IOException Exception occured", e);
			throw new PaymentGatewayClientException(e);
		} catch (Exception e) {
			logger.error("Exception Exception occured", e);
			throw new PaymentGatewayClientException(e);
		}
		logger.info("findPGAuditInfoByOrderNbr call completed");
		return txnHistoryResponse;
	}
}
