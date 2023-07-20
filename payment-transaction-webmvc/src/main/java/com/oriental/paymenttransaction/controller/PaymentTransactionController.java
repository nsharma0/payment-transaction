package com.oriental.paymenttransaction.controller;

import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oriental.paymenttransaction.business.constants.URLMappingConstants;
import com.oriental.paymenttransaction.dto.web.TransactionDetailRequestDTO;
import com.oriental.paymenttransaction.dto.web.TransactionHistoryRequestDTO;
import com.oriental.paymenttransaction.dto.web.TransactionHistoryResponse;
import com.oriental.paymenttransaction.utils.DatabaseUtils;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * @author nsharma0
 * 
 */
@EnableAsync
@Controller
public class PaymentTransactionController {
	
	
	private static Logger logger = LoggerFactory.getLogger(PaymentTransactionController.class);
	
	@Autowired
	private DatabaseUtils dbUtils;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	
	@SuppressWarnings("unused")
	@ApiOperation(value="Write new Order",notes="Write new Order")
	@ApiResponses({@ApiResponse(code = 201, message = "Created"),
	@ApiResponse(code = 400, message = "Bad Request"),
	@ApiResponse(code = 500, message = "Internal Server Error")})
	@RequestMapping(value = URLMappingConstants.SEARCH_TRANSACTION_HISTORY,method = RequestMethod.POST, produces="application/json", consumes="application/json")
	@ResponseBody 
	public Response searchTransactionHistory(@Valid @RequestBody TransactionHistoryRequestDTO requestDTO, BindingResult result, Errors error) {
		TransactionHistoryResponse response = null;
		if (result.hasErrors()) {
			logger.error("PaymentToken Request validation failed for saveCustomerNumberToAuditTrail");
			return Response.status(Status.BAD_REQUEST).entity(error.toString()).build();
		} else {
			try {
				response = dbUtils.getTransactionHistory(requestDTO);
			} catch (Exception exception) {
				logger.error("PaymentGatewayService Exception occured");
				exception.printStackTrace();
				return Response.serverError().entity(response).build();
			}
			logger.info("< PaymentUtilitiesController.saveCustomerNumberToAuditTrail Controller");
			return Response.ok(response).build();
		}
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
						txnHistoryRessponse = dbUtils.getTransactionDetails(requestDTO);
				} catch (Exception exception) {
					logger.error("PaymentGatewayService Exception occured");
					exception.printStackTrace();
					return Response.serverError().entity(txnHistoryRessponse).build();
				}	
				logger.info("< PaymentUtilitiesController.getTransactionDetails Controller");
				return Response.ok(txnHistoryRessponse).build();
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
					customerNumbers = dbUtils.searchCustomerByToken(queryBy);
				} catch (Exception exception) {
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
						txnHistoryRessponse = dbUtils.findPGAuditInfoByOrderNbr(requestDTO);
				} catch (Exception exception) {
					logger.error("PaymentGatewayService Exception occured");
					exception.printStackTrace();
					return Response.serverError().entity(txnHistoryRessponse).build();
				}	
				logger.info("< PaymentUtilitiesController.findPGAuditInfoByOrderNbr Controller");
				return Response.ok(txnHistoryRessponse).build();
	}
}

