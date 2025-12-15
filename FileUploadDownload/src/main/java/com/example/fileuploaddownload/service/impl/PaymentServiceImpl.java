package com.example.fileuploaddownload.service.impl;

import com.example.fileuploaddownload.dto.OrderRequestDto;
import com.example.fileuploaddownload.pojo.PaymentEntity;
import com.example.fileuploaddownload.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    RazorpayClient razorpayClient;
    //private final PaymentRepository paymentRepository;

    @Value("${razorpay.secret}")
    private String razorpaySecret;

    @Override
    public Order createOrder(OrderRequestDto dto) throws Exception {
        long amountPaise = dto.getAmount() * 100; // rupees to paise

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountPaise);
        orderRequest.put("currency", dto.getCurrency());
        orderRequest.put("receipt", dto.getReceipt() == null ? "rcpt_" + System.currentTimeMillis() : dto.getReceipt());
        orderRequest.put("payment_capture", 1);

        Order order = razorpayClient.orders.create(orderRequest);

        // save minimal record
        PaymentEntity e = PaymentEntity.builder()
                .razorpayOrderId(order.get("id"))
                .amount(order.get("amount"))
                .currency(order.get("currency"))
                .status("created")
                .createdAt(OffsetDateTime.now())
                .build();
        //  paymentRepository.save(e);

        return order;
    }

    @Override
    public boolean verifySignature(String orderId, String paymentId, String signature) throws Exception {
        try {
            String payload = orderId + "|" + paymentId;
            return com.razorpay.Utils.verifySignature(payload, signature, razorpaySecret);
        } catch (Exception ex) {
            return false;
        }
    }
/*
    public void markPaymentCaptured(String orderId, String paymentId, String signature) {
        Optional<PaymentEntity> opt = paymentRepository.findByRazorpayOrderId(orderId);
        if (opt.isPresent()) {
            PaymentEntity e = opt.get();
            e.setRazorpayPaymentId(paymentId);
            e.setRazorpaySignature(signature);
            e.setStatus("captured");
            paymentRepository.save(e);
        }
    } */

}
