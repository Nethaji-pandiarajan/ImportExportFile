package com.example.fileuploaddownload.pojo;

import lombok.*;

import java.time.OffsetDateTime;

//@Entity
//@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEntity {
    //@Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    //private Long id;

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    private Long amount; // in paise
    private String currency;
    private String status; // created/captured/failed
    private OffsetDateTime createdAt;
}

