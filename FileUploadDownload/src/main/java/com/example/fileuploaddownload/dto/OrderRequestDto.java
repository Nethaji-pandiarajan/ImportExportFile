package com.example.fileuploaddownload.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRequestDto {
    @Min(value = 1, message = "Amount must be >= 1")
    private Long amount; // in INR rupees

    @NotBlank
    private String currency = "INR";

    private String receipt;
}
