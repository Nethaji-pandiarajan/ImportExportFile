package com.example.fileuploaddownload.service;

import com.example.fileuploaddownload.dto.OrderRequestDto;
import com.razorpay.Order;

public interface PaymentService {

    public Order createOrder(OrderRequestDto dto) throws Exception;

    public boolean verifySignature(String orderId, String paymentId, String signature) throws Exception;

    // public void markPaymentCaptured(String orderId, String paymentId, String signature);
}
