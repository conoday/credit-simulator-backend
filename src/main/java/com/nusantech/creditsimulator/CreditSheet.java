package com.nusantech.creditsimulator;

public record CreditSheet(String name, CalculationResult result) {
    public CreditSheet {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Nama sheet wajib diisi.");
        }
    }
}
