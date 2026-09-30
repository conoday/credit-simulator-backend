package com.nusantech.creditsimulator;

import java.util.Locale;

public enum VehicleCondition {
    BARU,
    BEKAS;

    public static VehicleCondition parse(String value) {
        if (value == null) {
            throw new ValidationException("Kondisi kendaraan wajib diisi.");
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "baru" -> BARU;
            case "bekas", "lama" -> BEKAS;
            default -> throw new ValidationException("Kondisi kendaraan harus Baru atau Bekas.");
        };
    }

    public String displayName() {
        return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
    }
}
