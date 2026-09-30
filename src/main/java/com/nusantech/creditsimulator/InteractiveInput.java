package com.nusantech.creditsimulator;

import java.math.BigDecimal;
import java.util.Scanner;

public final class InteractiveInput {
    private InteractiveInput() {
    }

    public static LoanInput readLoan(Scanner scanner) {
        String vehicleType = ask(scanner, "Jenis kendaraan (Mobil/Motor): ");
        String vehicleCondition = ask(scanner, "Kondisi kendaraan (Baru/Bekas): ");
        int vehicleYear = askInteger(scanner, "Tahun kendaraan: ");
        BigDecimal totalLoanAmount = askDecimal(scanner, "Jumlah pinjaman total: ");
        int loanTenure = askInteger(scanner, "Tenor pinjaman (1-6 tahun): ");
        BigDecimal downPayment = askDecimal(scanner, "Jumlah DP: ");
        return new LoanInput(
                VehicleType.parse(vehicleType),
                VehicleCondition.parse(vehicleCondition),
                vehicleYear,
                totalLoanAmount,
                loanTenure,
                downPayment);
    }

    private static String ask(Scanner scanner, String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new ApplicationException("Input dihentikan sebelum selesai.");
        }
        return scanner.nextLine().trim();
    }

    private static int askInteger(Scanner scanner, String prompt) {
        String value = ask(scanner, prompt);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new ApplicationException("Nilai harus berupa bilangan bulat: " + value, exception);
        }
    }

    private static BigDecimal askDecimal(Scanner scanner, String prompt) {
        String value = ask(scanner, prompt);
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new ApplicationException("Nilai harus berupa angka: " + value, exception);
        }
    }
}
