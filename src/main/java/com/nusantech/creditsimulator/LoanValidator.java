package com.nusantech.creditsimulator;

import java.math.BigDecimal;

public final class LoanValidator {
    private static final BigDecimal MAX_TOTAL_LOAN = new BigDecimal("1000000000");
    private static final BigDecimal NEW_MINIMUM_DP = new BigDecimal("0.35");
    private static final BigDecimal USED_MINIMUM_DP = new BigDecimal("0.25");

    private LoanValidator() {
    }

    public static void validate(LoanInput input, int currentYear) {
        if (input == null) {
            throw new ValidationException("Data pinjaman wajib diisi.");
        }
        if (input.vehicleType() == null || input.vehicleCondition() == null) {
            throw new ValidationException("Jenis dan kondisi kendaraan wajib diisi.");
        }
        if (input.vehicleYear() < 1000 || input.vehicleYear() > 9999) {
            throw new ValidationException("Tahun kendaraan harus berupa 4 digit.");
        }
        if (input.vehicleCondition() == VehicleCondition.BARU
                && input.vehicleYear() < currentYear - 1) {
            throw new ValidationException("Tahun kendaraan baru minimal " + (currentYear - 1) + ".");
        }
        if (input.totalLoanAmount() == null
                || input.totalLoanAmount().signum() <= 0
                || input.totalLoanAmount().compareTo(MAX_TOTAL_LOAN) > 0) {
            throw new ValidationException("Total pinjaman harus lebih dari 0 dan maksimal Rp. 1,000,000,000.00.");
        }
        if (input.loanTenureYears() < 1 || input.loanTenureYears() > 6) {
            throw new ValidationException("Tenor pinjaman harus antara 1 sampai 6 tahun.");
        }
        if (input.downPayment() == null || input.downPayment().signum() < 0) {
            throw new ValidationException("DP tidak boleh negatif.");
        }
        if (input.downPayment().compareTo(input.totalLoanAmount()) > 0) {
            throw new ValidationException("DP tidak boleh melebihi total pinjaman.");
        }

        BigDecimal minimumRate = input.vehicleCondition() == VehicleCondition.BARU
                ? NEW_MINIMUM_DP : USED_MINIMUM_DP;
        BigDecimal minimumDownPayment = input.totalLoanAmount().multiply(minimumRate);
        if (input.downPayment().compareTo(minimumDownPayment) < 0) {
            String percentage = input.vehicleCondition() == VehicleCondition.BARU ? "35%" : "25%";
            throw new ValidationException("DP kendaraan " + input.vehicleCondition().displayName()
                    + " minimal " + percentage + " dari total pinjaman.");
        }
    }
}
