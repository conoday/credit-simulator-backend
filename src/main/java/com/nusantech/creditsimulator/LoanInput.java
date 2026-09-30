package com.nusantech.creditsimulator;

import java.math.BigDecimal;

public record LoanInput(
        VehicleType vehicleType,
        VehicleCondition vehicleCondition,
        int vehicleYear,
        BigDecimal totalLoanAmount,
        int loanTenureYears,
        BigDecimal downPayment) {
}
