package com.nusantech.creditsimulator;

import java.math.BigDecimal;

public enum VehicleType {
    MOBIL(new BigDecimal("0.08")),
    MOTOR(new BigDecimal("0.09"));

    private final BigDecimal baseRate;

    VehicleType(BigDecimal baseRate) {
        this.baseRate = baseRate;
    }

    public BigDecimal baseRate() {
        return baseRate;
    }

    public static VehicleType parse(String value) {
        if (value == null) {
            throw new ValidationException("Jenis kendaraan wajib diisi.");
        }
        return switch (value.trim().toLowerCase()) {
            case "mobil" -> MOBIL;
            case "motor" -> MOTOR;
            default -> throw new ValidationException("Jenis kendaraan harus Mobil atau Motor.");
        };
    }

    public String displayName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
