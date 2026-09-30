package com.nusantech.creditsimulator;

import java.math.BigDecimal;

public record AnnualInstallment(
        int year,
        BigDecimal rate,
        BigDecimal openingBalance,
        BigDecimal totalDue,
        BigDecimal monthlyPayment,
        BigDecimal closingBalance) {
}
