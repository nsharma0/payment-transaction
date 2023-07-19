package com.oriental.paymenttransaction.controller;

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
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oriental.paymenttransaction.business.constants.URLMappingConstants;
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
	public Response writeOrder(@Valid @RequestBody TransactionHistoryRequestDTO requestDTO, BindingResult result, Errors error) {
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
}

