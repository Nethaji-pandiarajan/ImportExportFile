package com.example.fileuploaddownload.controller;

import com.example.fileuploaddownload.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WebHookController {

    PaymentService paymentService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${razorpay.webhook_secret}")
    private String webhookSecret;


    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestHeader(name = "X-Razorpay-Signature", required = false) String signature,
                                                @RequestBody String payload) {
        try {
            boolean valid = com.razorpay.Utils.verifyWebhookSignature(payload, signature, webhookSecret);
            if (!valid) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid signature");
            }

            // parse event
            Map<String, Object> json = mapper.readValue(payload, Map.class);
            String event = (String) json.get("event");

            if ("payment.captured".equals(event)) {
                Map<String, Object> payloadObj = (Map<String, Object>) json.get("payload");
                Map<String, Object> payment = (Map<String, Object>) ((Map<String, Object>) payloadObj.get("payment")).get("entity");
                String orderId = (String) payment.get("order_id");
                String paymentId = (String) payment.get("id");
                // we don't have signature here — webhook proven valid already
                //paymentService.markPaymentCaptured(orderId, paymentId, "webhook_verified");
            }

            return ResponseEntity.ok("ok");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error");
        }
    }
}

