package com.mizan.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateOrderRequest {
    @NotBlank(message = "Ticker is required")
    @Size(max = 20)
    private String ticker;

    private String stockName;

    @NotNull @Min(1)
    private Integer quantity;

    @NotNull @DecimalMin("0.0001")
    private BigDecimal pricePerShare;

    @DecimalMin("0")
    private BigDecimal commission;

    @NotNull
    private LocalDate buyDate;

    @Size(max = 512)
    private String notes;
}
