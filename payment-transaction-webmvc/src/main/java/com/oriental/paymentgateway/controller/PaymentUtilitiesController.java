package com.oriental.paymentgateway.controller;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oriental.paymentgateway.exceptions.PaymentGatewayException;
import com.oriental.paymentgateway.service.MerchandiseCertService;
import com.oriental.paymentgateway.service.PaymentUtilsService;
import com.oriental.paymentgateway.utils.CommonConstants;
import com.oriental.paymentgateway.utils.URLMappingConstants;
import com.oriental.paymentgatewaypayload.dto.web.CheckPaymentDTO;
import com.oriental.paymentgatewaypayload.dto.web.MerchandiseCertificateRequestDTO;
import com.oriental.paymentgatewaypayload.dto.web.MerchandiseCertificateResponseDTO;
import com.oriental.paymentgatewaypayload.dto.web.PaymentToken;
import com.oriental.paymentgatewaypayload.dto.web.PendingPayments;
import com.oriental.paymentgatewaypayload.dto.web.TransactionDetailRequestDTO;
import com.oriental.paymentgatewaypayload.dto.web.TransactionHistoryRequestDTO;
import com.oriental.paymentgatewaypayload.dto.web.TransactionHistoryResponse;
import com.oriental.paymentgatewaypayload.dto.web.VoidMerchandiseCertificateRequestDTO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * @author Santhosh
 * 
 */
@RestController
@RequestMapping(URLMappingConstants.PAYMENT_UTILS)
@Api(basePath = URLMappingConstants.PAYMENT_UTILS, value = "Handles database requests", description = "Handles database transactions to payment")
public class PaymentUtilitiesController {
	
	
	private static Logger logger = LoggerFactory.getLogger(PaymentUtilitiesController.class);
	
	@Autowired
	private PaymentUtilsService paymentUtilsService;
	
	@Autowired
	private MerchandiseCertService merchandiseCertService;
	

	@Autowired
	private ObjectMapper objectMapper;
	
	/**
	 * SVeeramalla
	 * This method is used for saving details into pending payments table
	 */
	@RequestMapping(value = URLMappingConstants.SAVE_PENDING_PAYMENTS, method = RequestMethod.POST)
	@ApiOperation(value = "Writes Values to Pending payments table", notes = "Writes to pending Payments table. If record is there, it updates the number of attempts")
	@ApiResponses({@ApiResponse(code = 200, message = "OK"),
		@ApiResponse(code = 400, message = "Bad Request"),
		@ApiResponse(code = 500, message = "Internal Server Error")})
	public Response savePendingPayments(@ApiParam(value = "Pending Payments Request", required = true) @Valid @RequestBody PendingPayments pendingPaymentsRequest, BindingResult result, Errors errors) {
		logger.info("> In PaymentUtilitiesController.savePendingPayments Controller");
		
		String response = null;
		if (result.hasErrors()) {
			logger.error("PendingPayments Request validation failed for savePendingpayments");
			return Response.status(Status.BAD_REQUEST).entity(errors.toString()).build();
		}else{
			try {
				response = paymentUtilsService.savePendingPayments(pendingPaymentsRequest);
			} catch (PaymentGatewayException exception) {
				logger.error("PaymentGatewayService Exception occured");
				response = exception.getMessage();
				exception.printStackTrace();
				return Response.serverError().entity(response).build();
			}	
			logger.info("< PaymentUtilitiesController.savePendingPayments Controller");
			return Response.ok(response).build();
		}
	}
	
	/**
	 * @author SAdiraju
	 * 6/23/2016
	 * 
	 * Send the request to update the paymentgateway transaction audit table with the customer number in the request
	 */
	
	@RequestMapping(value = URLMappingConstants.SAVE_CUSTOMER_NUMBER_AUDIT_TRAIL, method = RequestMethod.POST)
	@ApiOperation(value = "Writes Values to PGWTRAADT table", notes = "Writes to PGWTRAADT table")
	@ApiResponses({ @ApiResponse(code = 200, message = "OK"), @ApiResponse(code = 400, message = "Bad Request"),
			@ApiResponse(code = 500, message = "Internal Server Error") })
	public Response saveCustomerNumberToAuditTrail(
			@ApiParam(value = "Payment Token Request", required = true) @Valid @RequestBody PaymentToken paymentTokenRequest,
			BindingResult result, Errors errors) {
		logger.info("> In PaymentUtilitiesController.saveCustomerNumberToAuditTrail Controller");

		String response = null;
		if (result.hasErrors()) {
			logger.error("PaymentToken Request validation failed for saveCustomerNumberToAuditTrail");
			return Response.status(Status.BAD_REQUEST).entity(errors.toString()).build();
		} else {
			try {
				response = paymentUtilsService.saveCustomerNumberToAuditTrail(paymentTokenRequest);
			} catch (PaymentGatewayException exception) {
				logger.error("PaymentGatewayService Exception occured");
				response = exception.getMessage();
				exception.printStackTrace();
				return Response.serverError().entity(response).build();
			}
			logger.info("< PaymentUtilitiesController.saveCustomerNumberToAuditTrail Controller");
			return Response.ok(response).build();
		}
	}
	
	
	/**
	 * SVeeramalla
	 * This method is used for deleting an entry from pending payment table.
	 */
	@RequestMapping(value = URLMappingConstants.DELETE_PENDING_PAYMENTS, method = RequestMethod.DELETE)
	@ApiOperation(value = "Deletes record from Pending payments table", notes = "Deletes record from pending payments table")
	@ApiResponses({@ApiResponse(code = 200, message = "OK"),
		@ApiResponse(code = 400, message = "Bad Request"),
		@ApiResponse(code = 500, message = "Internal Server Error")})
	public Response deletePendingPayments(@ApiParam(value = "Pending Payments Request", required = true) @Valid @RequestBody PendingPayments pendingPaymentsRequest, BindingResult result, Errors errors) {
		logger.info("> In PaymentUtilitiesController.savePendingPayments Controller");
		if (result.hasErrors()) {
			logger.error("PendingPayments Request validation failed for deletePendingPayments");
			return Response.status(Status.BAD_REQUEST).entity(errors.toString()).build();
		}else{
			String status = null;
			try {
				status = paymentUtilsService.deletePendingPayments(pendingPaymentsRequest);
			} catch (PaymentGatewayException exception) {
				logger.error("PaymentGatewayService Exception occured");
				status = exception.getMessage();
				exception.printStackTrace();
				return Response.serverError().entity(status).build();
			}	
			logger.info("< PaymentUtilitiesController.deletePendingPayments Controller");
			return Response.ok(status).build();
		}
	}
	
	/**
	 * SVeeramalla
	 * This method returns Switch status for the business unit requested
	 * 
	 */
	@RequestMapping(value = URLMappingConstants.GET_SWITCH_STATUS, method = RequestMethod.GET)
	@ApiOperation(value = "Returns Switch ON/OFF status", notes = "Tells switch status for particular business Unit. Payment Type should be either PP or CC")
	@ApiResponses({@ApiResponse(code = 200, message = "OK"),
		@ApiResponse(code = 400, message = "Bad Request"),
		@ApiResponse(code = 500, message = "Internal Server Error")})
	public Response getSwitchStatus(@RequestParam(value = "Business Unit", required = false)  String businessUnit, @RequestParam(value = "Payment Type", required = false)  String paymentType) {
		logger.info("> In PaymentUtilitiesController.getSwichStatus Controller");
			String switchStatus = null;
			try {
					switchStatus = paymentUtilsService.getSwitchStatus(businessUnit, paymentType);
				} catch (PaymentGatewayException exception) {
				logger.error("PaymentGatewayService Exception occured");
				exception.printStackTrace();
				return Response.serverError().entity(exception).build();
			}	
			logger.info("< PaymentUtilitiesController.getSwichStatus Controller");
			return Response.ok(switchStatus).build();
	}
	
	/**
	 * SVeeramalla
	 * This method returns the list of all business units
	 * 
	 */
	@RequestMapping(value = URLMappingConstants.GET_ALL_BUs, method = RequestMethod.GET)
	@ApiOperation(value = "Returns BU List", notes = "Gives the list of business unit numbers configured for TSYS")
	@ApiResponses({@ApiResponse(code = 200, message = "OK"),
		@ApiResponse(code = 400, message = "Bad Request"),
		@ApiResponse(code = 500, message = "Internal Server Error")})
	public Response getAllBUs() {
		logger.info("> In PaymentUtilitiesController.getAllBUs Controller");
			List<Integer> buList = null;
			try {
					buList = paymentUtilsService.getAllBus();
				} catch (PaymentGatewayException exception) {
				logger.error("PaymentGatewayService Exception occured");
				exception.printStackTrace();
				return Response.serverError().entity(exception).build();
			}	
			logger.info("< PaymentUtilitiesController.getAllBUs Controller");
			return Response.ok(buList).build();
	}
	
	/**
	 * SVeeramalla
	 * This method return card type and token valid status
	 * 
	 */
	@RequestMapping(value = URLMappingConstants.GET_TOKEN_INFO, method = RequestMethod.GET)
	@ApiOperation(value = "Returns card Type and status", notes = "Returns card type and isValid status")
	@ApiResponses({@ApiResponse(code = 200, message = "OK"),
		@ApiResponse(code = 400, message = "Bad Request"),
		@ApiResponse(code = 500, message = "Internal Server Error")})
	public Response getTokenInfo(@RequestParam(value = "token", required = true)  String token, @RequestParam(value = "businessUnit", required = false)  BigDecimal businessUnit) {
		logger.info("> In PaymentUtilitiesController.getTokenInfo Controller");
			PaymentToken tokenInfo = null;
			if(null == token){
				logger.error("Request validation failed for getTokenInfo");
				return Response.status(Status.BAD_REQUEST).entity(CommonConstants.BAD_REQUEST_MESSAGE).build();
		} else {
			try {
				tokenInfo = paymentUtilsService.getTokenInfo(token, businessUnit);
			} catch (PaymentGatewayException exception) {
				logger.error("PaymentGatewayService Exception occured");
				exception.printStackTrace();
				return Response.serverError().entity(exception).build();
			}
			logger.info("< PaymentUtilitiesController.getTokenInfo Controller");
			return Response.ok(tokenInfo).build();
		}
	}
	
	/**
	 * SAdiraju
	 * 01/26/2017
	 * This method returns an encrypted string
	 * 
	 */
	@RequestMapping(value = URLMappingConstants.GET_ENCRYPTED_DATA, method = RequestMethod.GET)
	@ApiOperation(value = "Returns encrypted data", notes = "Returns encrypted data")
	@ApiResponses({@ApiResponse(code = 200, message = "OK"),
		@ApiResponse(code = 400, message = "Bad Request"),
		@ApiResponse(code = 500, message = "Internal Server Error")})
	public Response getEncryptedData(@RequestParam(value = "data", required = true)  String data) {
		logger.info("> In PaymentUtilitiesController.getEncryptedData Controller");
		String encryptedData="";
			if(null == data){
				logger.error("Request validation failed for getEncryptedData");
				return Response.status(Status.BAD_REQUEST).entity(CommonConstants.BAD_REQUEST_MESSAGE).build();
		} else {
			try {
				encryptedData = paymentUtilsService.getEncryptedData(data);
			} catch (PaymentGatewayException exception) {
				logger.error("PaymentGatewayService Exception occured");
				exception.printStackTrace();
				return Response.serverError().entity(exception).build();
			}
			logger.info("< PaymentUtilitiesController.getEncryptedData Controller");
			return Response.ok(encryptedData).build();
		}
	}

	/**
	 * SVeeramalla
	 * This method return payment information provided cusNbr/ordNbr/token
	 * 
	 */
	@RequestMapping(value = URLMappingConstants.GET_PAYMENT_INFO, method = RequestMethod.POST)
	@ApiOperation(value = "Returns card Type and status", notes = "Returns card type and isValid status")
	@ApiResponses({@ApiResponse(code = 200, message = "OK"),
		@ApiResponse(code = 400, message = "Bad Request"),
		@ApiResponse(code = 500, message = "Internal Server Error")})
	public Response getPaymentInfo(@ApiParam(value = "Payment Information Request", required = true) @RequestBody PaymentToken request, BindingResult result, Errors errors) {
		logger.info("> In PaymentUtilitiesController.getPaymentInfo Controller");
			List<PaymentToken> paymentInfo = null;
		if(null == request.getCustomerNbr() && null == request.getOrderNbr() && null == request.getToken() ){
				logger.error("Request validation failed for getPaymentInfo");
				return Response.status(Status.BAD_REQUEST).entity(CommonConstants.BAD_REQUEST_MESSAGE).build();
		} else {
			try {
				paymentInfo = paymentUtilsService.findPaymentInfo(request);
			} catch (PaymentGatewayException exception) {
				logger.error("PaymentGatewayService Exception occured");
				exception.printStackTrace();
				return Response.serverError().entity(exception).build();
			}
			logger.info("< PaymentUtilitiesController.getPaymentInfo Controller");
			return Response.ok(paymentInfo).build();
		}
	}

	
	/**
	 * SVeeramalla
	 * This method return Auth Status 
	 * 
	 */
	@RequestMapping(value = URLMappingConstants.GET_AUTH_STATUS, method = RequestMethod.POST)
	@ApiOperation(value = "Accepts Payment Token Request", notes = "Returns Auth Status")
	@ApiResponses({@ApiResponse(code = 200, message = "OK"),
		@ApiResponse(code = 400, message = "Bad Request"),
		@ApiResponse(code = 500, message = "Internal Server Error")})
	public Response findAuthStatus(@ApiParam(value = "PaymentToken Request", required = true) @Valid @RequestBody PaymentToken tokenRequest, BindingResult result, Errors errors) {
		logger.info("> In PaymentUtilitiesController.findAuthStatus Controller");
			
			if (result.hasErrors()) {
				logger.error("Auth Status Request validation failed for findAuthStatus");
				return Response.status(Status.BAD_REQUEST).entity(errors.toString()).build();
			}else{
				String status = null;
				try {
					status = paymentUtilsService.findAuthStatus(tokenRequest);
				} catch (PaymentGatewayException exception) {
					logger.error("PaymentGatewayService Exception occured");
					status = exception.getMessage();
					exception.printStackTrace();
					return Response.serverError().entity(status).build();
				}	
				logger.info("< PaymentUtilitiesController.findAuthStatus Controller");
				return Response.ok(status).build();
			}
	}
	
	/**
	 * mmuppidathy
	 * This method return PayPalAuth Status 
	 * 
	 */
	@RequestMapping(value = URLMappingConstants.GET_PAYPAL_AUTH_STATUS, method = RequestMethod.POST)
	@ApiOperation(value = "Accepts Payment Token Request", notes = "Returns Auth Status")
	@ApiResponses({@ApiResponse(code = 200, message = "OK"),
		@ApiResponse(code = 400, message = "Bad Request"),
		@ApiResponse(code = 500, message = "Internal Server Error")})
	public Response findPayPalAuthStatus(@ApiParam(value = "PaymentToken Request", required = true) @Valid @RequestBody PaymentToken tokenRequest, BindingResult result, Errors errors) {
		logger.info("> In PaymentUtilitiesController.findPaypalAuthStatus Controller");
			
		if(null == tokenRequest.getOrderNbr()) {
				logger.error("Auth Status Request validation failed for findPaypalAuthStatus");
				return Response.status(Status.BAD_REQUEST).entity(errors.toString()).build();
			}else{
				String status = null;
				try {
					status = paymentUtilsService.findPayPalAuthStatus(tokenRequest);
				} catch (PaymentGatewayException exception) {
					logger.error("PaymentGatewayService Exception occured");
					status = exception.getMessage();
					exception.printStackTrace();
					return Response.serverError().entity(status).build();
				}	
				logger.info("< PaymentUtilitiesController.findAuthStatus Controller");
				return Response.ok(status).build();
			}
	}
	
	
	/**
	 * @author gsrinivas 01/08/2019: Breakfix INC0095773
	 * 
	 * This method returns payment information (Transaction ID) for
	 * different payment types CC / PayPal / EGC for voiding the AUTH
	 * 
	 * @param tokenRequest
	 * @param result
	 * @param errors
	 * @return Response
	 */
	@RequestMapping(value = URLMappingConstants.GET_PAYMENT_DETAILS, method = RequestMethod.POST)
	@ApiOperation(value = "Accepts Payment Token Request", notes = "Returns Payment Token info for Payment type PayPal / EGC")
	@ApiResponses({@ApiResponse(code = 200, message = "OK"),
		@ApiResponse(code = 400, message = "Bad Request"),
		@ApiResponse(code = 500, message = "Internal Server Error")})
	public Response getPaymentDetailsForVoid(@ApiParam(value = "PaymentToken Request", required = true) @Valid @RequestBody PaymentToken tokenRequest, BindingResult result, Errors errors) {
		logger.info("> In PaymentUtilitiesController.getPaymentDetailsForVoid Controller");
		
		PaymentToken tokenResponse = new PaymentToken();
		
		if (tokenRequest == null || tokenRequest.getOrderNbr() == null) {
			logger.error("Request validation failed for getPaymentDetails");
			return Response.status(Status.BAD_REQUEST).entity(CommonConstants.BAD_REQUEST_MESSAGE).build();
		} else {
			try {
				tokenResponse = paymentUtilsService.getPaymentDetails(tokenRequest);
			} catch(PaymentGatewayException pge) {
				logger.error("PaymentGatewayService Exception occured in getPaymentDetailsForVoid()");
				pge.printStackTrace();
				return Response.serverError().entity(pge).build();
			} catch (Exception e) {
				logger.error("PaymentGatewayService Exception occured in getPaymentDetailsForVoid()");
				e.printStackTrace();
				return Response.serverError().entity(e).build();
			}
		}
		
		logger.info("< In PaymentUtilitiesController.getPaymentDetailsForVoid() Controller");
		return Response.ok(tokenResponse).build();
	}

	/**
	 * @author mmuppidath
	 * @param MerchandiseCertificateRequestDTO
	 * @return MerchandiseCertificateResponseDTO
	 * 
	 */
	@RequestMapping(value = URLMappingConstants.GET_MERCH_CERTIFICATES, method = RequestMethod.POST)
	@ApiOperation(value = "Accepts customer Number", notes = "Returns Auth Status")
	@ApiResponses({@ApiResponse(code = 200, message = "OK"),
		@ApiResponse(code = 400, message = "Bad Request"),
		@ApiResponse(code = 500, message = "Internal Server Error")})
	public Response getMerchandiseCertificatesForCustomer(@ApiParam(value = "Customer Number", required = true) @Valid @RequestBody MerchandiseCertificateRequestDTO merchandiseCertificateRequestDTO, BindingResult result, Errors errors) {
		logger.info("> In PaymentUtilitiesController.getMerchandiseCertificatesForCustomer Controller");
			
	/*	if(null == merchandiseCertificateRequestDTO.getCustomerNumber()) {
				logger.error("validation failed for getMerchandiseCertificatesForCustomer");
				return Response.status(Status.BAD_REQUEST).entity(errors.toString()).build();
			}else{*/
				MerchandiseCertificateResponseDTO merchandiseCertificateResponseDTO=null;
				try {
					if(merchandiseCertificateRequestDTO.getCustomerNumber()!=null){
					//sjain01,11/01/2020 Changes for pagination	
					merchandiseCertificateResponseDTO=	merchandiseCertService.getMerchandiseCertificatesForCustomer(merchandiseCertificateRequestDTO.getCustomerNumber(),merchandiseCertificateRequestDTO.getPageNumber());
					}else if(merchandiseCertificateRequestDTO.getCertNumber()!=null){
						merchandiseCertificateResponseDTO=	merchandiseCertService.getMerchandiseCertificatesByCertNumber(merchandiseCertificateRequestDTO.getCertNumber());
					}
					//status = paymentUtilsService.findPayPalAuthStatus(tokenRequest);
				} catch (PaymentGatewayException exception) {
					logger.error("PaymentGatewayService Exception occured");
					exception.printStackTrace();
					return Response.serverError().entity(merchandiseCertificateResponseDTO).build();
				}	
				logger.info("< PaymentUtilitiesController.getMerchandiseCertificatesForCustomer Controller");
				return Response.ok(merchandiseCertificateResponseDTO).build();
			}
//	}
	
	
	/**
	 * 
	 * @mmuppidath
	 * @param VoidMerchandiseCertificateRequestDTO
	 * @return String
	 * 
	 */
	@RequestMapping(value = URLMappingConstants.VOID_MERCH_CERTIFICATES, method = RequestMethod.POST)
	@ApiOperation(value = "Accepts Merch Certificate Numbers", notes = "Returns String")
	@ApiResponses({ @ApiResponse(code = 200, message = "OK"), @ApiResponse(code = 400, message = "Bad Request"),
			@ApiResponse(code = 500, message = "Internal Server Error") })
	public Response voidMerchCertificates(
			@ApiParam(value = "MercCertificate Number", required = true) @Valid @RequestBody VoidMerchandiseCertificateRequestDTO voidMerchandiseCertificateRequestDTO,
			BindingResult result, Errors errors) {
		logger.info("> In PaymentUtilitiesController.voidMerchCertificates Controller");
		String response = null;
		if (result.hasErrors()) {
			logger.error("PaymentReqestValidation validation failed");
			logger.error(result.toString());
			response=result.toString();
			return Response.status(Status.BAD_REQUEST).entity(response).build();
		} else {
			try {
				response = merchandiseCertService.voidMerchandiseCertificates(voidMerchandiseCertificateRequestDTO);

			} catch (PaymentGatewayException exception) {
				logger.error("PaymentGatewayService Exception occured");
				exception.printStackTrace();
				return Response.serverError().entity(response).build();
			}
			logger.info("< PaymentUtilitiesController.voidMerchCertificates Controller");
			return Response.ok(response).build();
		}

	}
	
	/**
	 * Author : nsharma0, 01/27/2020, Transaction History TDD
	 * @param requestDTO
	 * @return TransactionHistoryResponse
	 */
	@RequestMapping(value = URLMappingConstants.SEARCH_TRANSACTION_HISTORY, method = RequestMethod.POST)
	@ApiOperation(value = "Accepts customer Number", notes = "Returns Auth Status")
	@ApiResponses({@ApiResponse(code = 200, message = "OK"),
	@ApiResponse(code = 400, message = "Bad Request"),
	@ApiResponse(code = 500, message = "Internal Server Error")})
	@ResponseBody
	public Response searchTransactionHistory(@RequestBody TransactionHistoryRequestDTO requestDTO) {
		logger.info("> In PaymentUtilitiesController.searchTransactionHistory Controller");
				TransactionHistoryResponse txnHistoryRessponse = null;
				try {
						txnHistoryRessponse = paymentUtilsService.getTransactionHistory(requestDTO);
				} catch (PaymentGatewayException exception) {
					logger.error("PaymentGatewayService Exception occured");
					exception.printStackTrace();
					return Response.serverError().entity(txnHistoryRessponse).build();
				}	
				logger.info("< PaymentUtilitiesController.searchTransactionHistory Controller");
				return Response.ok(txnHistoryRessponse).build();
			}
	
	/**
	 * Author : nsharma0, 01/27/2020, Transaction History TDD
	 * @param requestDTO
	 * @return TransactionHistoryResponse
	 */
	@RequestMapping(value = URLMappingConstants.GET_TRANSACTION_DETAILS, method = RequestMethod.POST)
	@ApiOperation(value = "Accepts customer Number", notes = "Returns Auth Status")
	@ApiResponses({@ApiResponse(code = 200, message = "OK"),
	@ApiResponse(code = 400, message = "Bad Request"),
	@ApiResponse(code = 500, message = "Internal Server Error")})
	@ResponseBody
	public Response getTransactionDetails(@RequestBody TransactionDetailRequestDTO requestDTO) {
		logger.info("> In PaymentUtilitiesController.getTransactionDetails Controller");
			
				TransactionHistoryResponse txnHistoryRessponse = null;
				try {
						txnHistoryRessponse = paymentUtilsService.getTransactionDetails(requestDTO);
				} catch (PaymentGatewayException exception) {
					logger.error("PaymentGatewayService Exception occured");
					exception.printStackTrace();
					return Response.serverError().entity(txnHistoryRessponse).build();
				}	
				logger.info("< PaymentUtilitiesController.getTransactionDetails Controller");
				return Response.ok(txnHistoryRessponse).build();
			}
	
	/**
	 * bpandurang - 4/20/2020
	 * This method return Auth Status 
	 * 
	 */
	@RequestMapping(value = URLMappingConstants.GET_VOID_AUTH_STATUS, method = RequestMethod.POST)
	@ApiOperation(value = "Accepts Payment Token Request", notes = "Returns Void Auth Status")
	@ApiResponses({@ApiResponse(code = 200, message = "OK"),
		@ApiResponse(code = 400, message = "Bad Request"),
		@ApiResponse(code = 500, message = "Internal Server Error")})
	public Response findVoidAuthStatus(@ApiParam(value = "PaymentToken Request", required = true) @Valid @RequestBody PaymentToken tokenRequest, BindingResult result, Errors errors) {
		logger.info("> In PaymentUtilitiesController.findAuthStatus Controller");
			
			if (result.hasErrors()) {
				logger.error("Void Auth Status Request validation failed for findVoidAuthStatus");
				return Response.status(Status.BAD_REQUEST).entity(errors.toString()).build();
			}else{
				String status = null;
				try {
					status = paymentUtilsService.findVoidAuthStatus(tokenRequest);
				} catch (PaymentGatewayException exception) {
					logger.error("PaymentGatewayService Exception occured");
					status = exception.getMessage();
					exception.printStackTrace();
					return Response.serverError().entity(status).build();
				}	
				logger.info("< PaymentUtilitiesController.findAuthStatus Controller");
				return Response.ok(status).build();
			}
	}
	
	@RequestMapping(value = URLMappingConstants.GET_REAUTH_ORDER, method = RequestMethod.POST)
	@ApiOperation(value = "Accepts TransactionHistoryRequestDTO ", notes = "Returns Auth Status")
	@ApiResponses({@ApiResponse(code = 200, message = "OK"),
	@ApiResponse(code = 400, message = "Bad Request"),
	@ApiResponse(code = 500, message = "Internal Server Error")})
	@ResponseBody
	public Response getReAuthForOrder(@RequestBody TransactionHistoryRequestDTO requestDTO) {
		logger.info("> In PaymentUtilitiesController.getReAuthForOrder Controller");
				String authResponse = null;
				try {
					authResponse = paymentUtilsService.getReauthForOrder(requestDTO);
				} catch (PaymentGatewayException exception) {
					logger.error("PaymentGatewayService Exception occured");
					exception.printStackTrace();
					return Response.serverError().entity(authResponse).build();
				}	
				logger.info("< PaymentUtilitiesController.getReAuthForOrder Controller");
				return Response.ok(authResponse).build();
			}
	
	@RequestMapping(value = URLMappingConstants.VALIDATE_CHECK_PAYMENT, method = RequestMethod.POST)
	@ApiOperation(value = "Accepts a batch number and check number and returns the check value if it's correct")
	@ResponseBody
	public Response validateCheckPayment(@Valid @RequestBody CheckPaymentDTO requestDTO) {
	    logger.info("> In PaymentUtilitiesController.validateCheckPayment");
        CheckPaymentDTO response = null;
        try {
            response = paymentUtilsService.validateCheckPayment(requestDTO);
        } catch (PaymentGatewayException exception) {
            logger.error("PaymentGatewayService Exception occurred");
            exception.printStackTrace();
            return Response.serverError().entity(exception).build();
        }
        logger.info("< Leaving PaymentUtilitiesController.validateCheckPayment");
        return Response.ok(requestDTO).build();
	}

	@RequestMapping(value = URLMappingConstants.GET_MULTI_RELEASE_AUTH_STATUS, method = RequestMethod.POST)
	@ApiOperation(value = "Accepts Payment Token Request", notes = "Returns Auth Status")
	@ApiResponses({@ApiResponse(code = 200, message = "OK"),
		@ApiResponse(code = 400, message = "Bad Request"),
		@ApiResponse(code = 500, message = "Internal Server Error")})
	public Response getMultiReleaseAuthStatus(@ApiParam(value = "PaymentToken Request", required = true) @Valid @RequestBody 
			PaymentToken tokenRequest, BindingResult result, Errors errors) {
		Long startTime = (new Date()).getTime();
				
		try {
			logger.info(URLMappingConstants.GET_MULTI_RELEASE_AUTH_STATUS + " " + objectMapper.writeValueAsString(tokenRequest));
			
			if (result.hasErrors()) {
				logger.error("Validation error on request " + tokenRequest + " " + result.toString());
				return Response.status(Status.BAD_REQUEST).entity(errors.toString()).build();				
			}
			
			return Response.ok(paymentUtilsService.getMultiReleaseAuthStatus(tokenRequest)).build();
		} catch (JsonProcessingException e) {
			logger.error("Error parsing JSON request", e);
			return Response.serverError().entity(e.getMessage()).build();
		} catch (Exception e) {
			logger.error("Exception evaluting auth status", e);
			return Response.serverError().entity(e.getMessage()).build();
		} finally {
			logger.info("Controller response time for request " +  tokenRequest 
					+ ": " + ((new Date()).getTime() - startTime) + " ms");			
		}
	}
	
	
	/**
	 * nsharma0, 09/25/2020, search order by cc token
	 * */
	@RequestMapping(value = URLMappingConstants.SEARCH_CUSTOMER_BY_CC, method = RequestMethod.GET)
	@ApiOperation(value = "Accepts cc", notes = "Returns customer number")
	@ApiResponses({@ApiResponse(code = 200, message = "OK"),
	@ApiResponse(code = 400, message = "Bad Request"),
	@ApiResponse(code = 500, message = "Internal Server Error")})
	@ResponseBody
	public Response searchCustomerByCC(@RequestParam String queryBy) {
		logger.info("> In PaymentUtilitiesController.searchCustomerByCC Controller");
		List<Long> customerNumbers = null;
				try {
					customerNumbers = paymentUtilsService.searchCustomerByCC(queryBy);
				} catch (PaymentGatewayException exception) {
					logger.error("PaymentGatewayService Exception occured");
					exception.printStackTrace();
					return Response.serverError().entity(customerNumbers).build();
}
				logger.info("< PaymentUtilitiesController.searchCustomerByCC Controller");
				return Response.ok(customerNumbers).build();
			}
	/**
	 * Author : nbhutani
	 * @param requestDTO
	 * @return TransactionHistoryResponse
	 */
	@RequestMapping(value = URLMappingConstants.FIND_PG_AUDIT_INFO_BY_ORDNBR, method = RequestMethod.POST)
	@ApiOperation(value = "Accepts Order Number", notes = "Returns audit history")
	@ApiResponses({@ApiResponse(code = 200, message = "OK"),
	@ApiResponse(code = 400, message = "Bad Request"),
	@ApiResponse(code = 500, message = "Internal Server Error")})
	@ResponseBody
	public Response findPGAuditInfoByOrderNbr(@RequestBody TransactionDetailRequestDTO requestDTO) {
		logger.info("> In PaymentUtilitiesController.findPGAuditInfoByOrderNbr Controller");
			
				TransactionHistoryResponse txnHistoryRessponse = null;
				try {
						txnHistoryRessponse = paymentUtilsService.findPGAuditInfoByOrderNbr(requestDTO);
				} catch (PaymentGatewayException exception) {
					logger.error("PaymentGatewayService Exception occured");
					exception.printStackTrace();
					return Response.serverError().entity(txnHistoryRessponse).build();
				}	
				logger.info("< PaymentUtilitiesController.findPGAuditInfoByOrderNbr Controller");
				return Response.ok(txnHistoryRessponse).build();
			}
	
}

