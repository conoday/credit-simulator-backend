package com.nusantech.creditsimulator;

import java.util.List;

public record CalculationResult(LoanInput input, List<AnnualInstallment> installments) {
    public CalculationResult {
        installments = List.copyOf(installments);
    }
}
