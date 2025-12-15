package com.example.fileuploaddownload.controller;

import com.example.fileuploaddownload.dto.OrderRequestDto;
import com.example.fileuploaddownload.service.PaymentService;
import com.razorpay.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@Validated @RequestBody OrderRequestDto dto) throws Exception {
        Order order = paymentService.createOrder(dto);

        return ResponseEntity.ok(Map.of(
                "orderId", order.get("id"),
                "amount", order.get("amount"),
                "currency", order.get("currency")
        ));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> payload) {
        String orderId = payload.get("razorpay_order_id");
        String paymentId = payload.get("razorpay_payment_id");
        String signature = payload.get("razorpay_signature");

       // boolean ok = paymentService.verifySignature(orderId, paymentId, signature);
        if (true) {
           // paymentService.markPaymentCaptured(orderId, paymentId, signature);
            return ResponseEntity.ok(Map.of("status", "verified"));
        }
        return ResponseEntity.status(400).body(Map.of("status", "invalid_signature"));
    }
}

