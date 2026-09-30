package com.nusantech.creditsimulator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public final class InteractiveInput {
    private InteractiveInput() {
    }

    public static LoanInput readLoan(Scanner scanner) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("vehicleType", ask(scanner, "Jenis kendaraan (Mobil/Motor): "));
        values.put("vehicleCondition", ask(scanner, "Kondisi kendaraan (Baru/Bekas): "));
        values.put("vehicleYear", ask(scanner, "Tahun kendaraan: "));
        values.put("totalLoanAmount", ask(scanner, "Jumlah pinjaman total: "));
        values.put("loanTenure", ask(scanner, "Tenor pinjaman (1-6 tahun): "));
        values.put("downPayment", ask(scanner, "Jumlah DP: "));
        return InputParser.fromMap(values);
    }

    private static String ask(Scanner scanner, String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new ApplicationException("Input dihentikan sebelum selesai.");
        }
        return scanner.nextLine().trim();
    }

}
