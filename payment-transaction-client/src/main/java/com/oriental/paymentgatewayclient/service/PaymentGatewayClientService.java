package com.oriental.paymentgatewayclient.service;

import java.util.List;

import com.oriental.paymentgatewaypayload.dto.web.CheckPaymentDTO;
import com.oriental.paymentgatewaypayload.dto.web.CreditCardValidationRequest;
import com.oriental.paymentgatewaypayload.dto.web.CreditCardValidationResponse;
import com.oriental.paymentgatewaypayload.dto.web.MerchandiseCertificateRequestDTO;
import com.oriental.paymentgatewaypayload.dto.web.MerchandiseCertificateResponseDTO;
import com.oriental.paymentgatewaypayload.dto.web.PaymentRequest;
import com.oriental.paymentgatewaypayload.dto.web.PaymentResponse;
import com.oriental.paymentgatewaypayload.dto.web.PaymentToken;
import com.oriental.paymentgatewaypayload.dto.web.PendingPayments;
import com.oriental.paymentgatewaypayload.dto.web.TransactionHistoryRequestDTO;
import com.oriental.paymentgatewaypayload.dto.web.TransactionHistoryResponse;
import com.oriental.paymentgatewaypayload.dto.web.VoidMerchandiseCertificateRequestDTO;
import com.oriental.paymentgatewaypayload.enums.EndPointEnum;

/**
 * @author Jaya, Santhosh
 * 
 */
public interface PaymentGatewayClientService {
	
	public CreditCardValidationResponse validateCreditCard(CreditCardValidationRequest creditCardValidationRequest) ;
	
	public PaymentResponse makePayment(PaymentRequest paymentRequest);
	
	public List<Integer> getAllBUs();
	
	public String getSwitchStatus(String businessUnit, String paymentType);
	
	public String getMasterSwitchStatus();
	
	public PaymentToken getTokenInfo(String token, String businessUnit);
	
	public PaymentToken getTokenInfo(String token);
	
	public String savePendingPayments(PendingPayments pendingPaymentsRequest); 
	
	public String saveCustomerNumberToAuditTrail(PaymentToken paymentTokenRequest);
	
	public String deletePendingPayments(PendingPayments pendingPaymentsRequest); 
	
	public String findAuthStatus(PaymentToken tokenRequest);
	public String findPayPalAuthStatus(PaymentToken tokenRequest);
		
	public List<PaymentToken> getPaymentInfoByToken(String token);
	
	public List<PaymentToken> getPaymentInfoByCusNbr(int cusNbr);
	
	public List<PaymentToken> getPaymentInfoByOrderNbr(int orderNbr);
	//sadiraju 07/11/2016 method to get order details for paypal orders
	public List<PaymentToken> getPaymentInfoByOrderNbrAndPaymentMethod(int orderNbr);
	
	public String setEndpointurl(EndPointEnum choiceEnum);
	
	public void setEndPointUrl(String url);
	
	public String getEncryptedData(String dataToBeEncrypted);
	
	public boolean isInteger(final String tokenId);
	
	//blenagh 5/5/2017 - client timeout
	public void setTimeout(int timeout);
	
	// gsrinivas - 01/08/2019: Breakfix: INC0095773
	public PaymentToken getPaymentDetailsForVoid(PaymentToken tokenRequest);

	public MerchandiseCertificateResponseDTO getMerchandiseCertificatesForCustomer(MerchandiseCertificateRequestDTO merchandiseCertificateRequestDTO);
	
	public String  voidMerchCertificates(VoidMerchandiseCertificateRequestDTO voidMerchandiseCertificateRequestDTO);
	
	//nsharma0
	public TransactionHistoryResponse searchTransactionHistory(TransactionHistoryRequestDTO requestDTO);
	
	public TransactionHistoryResponse getTransactionDetails(String transactionId);
	
	//bpandurang 04/20/2020 get void auth status
	public String findVoidAuthStatus(PaymentToken tokenRequest);
	
	public String getReAuthForOrder(TransactionHistoryRequestDTO requestDTO);
	
	public CheckPaymentDTO validateCheck(CheckPaymentDTO request);

	//blenagh 6/2/2020 - Backorder
	public List<PaymentToken> getPaymentInfo(PaymentToken request);
	public Boolean getMultiReleaseAuthStatus(PaymentToken request);
	
	//nsharma0, 09/25/2020, search by cc token
	List<Long> searchCustomerByCC(String queryBy);

	TransactionHistoryResponse findPGAuditInfoByOrderNbr(Integer orderNumber);
}
