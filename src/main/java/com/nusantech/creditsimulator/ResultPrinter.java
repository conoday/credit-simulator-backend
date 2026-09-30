package com.nusantech.creditsimulator;

public final class ResultPrinter {
    private ResultPrinter() {
    }

    public static void print(CalculationResult result, String sheetName) {
        LoanInput input = result.input();
        System.out.println();
        if (sheetName != null && !sheetName.isBlank()) {
            System.out.println("Sheet: " + sheetName);
        }
        System.out.println("Kendaraan : " + input.vehicleType().displayName()
                + " / " + input.vehicleCondition().displayName());
        System.out.println("Tahun     : " + input.vehicleYear());
        System.out.println("Total     : " + MoneyFormatter.money(input.totalLoanAmount()));
        System.out.println("DP        : " + MoneyFormatter.money(input.downPayment()));
        System.out.println("Tenor     : " + input.loanTenureYears() + " tahun");
        System.out.println("Cicilan tahunan:");
        for (AnnualInstallment installment : result.installments()) {
            System.out.printf("  Tahun %d | Rate %s | Cicilan %s/bln | Saldo akhir %s%n",
                    installment.year(),
                    MoneyFormatter.rate(installment.rate()),
                    MoneyFormatter.money(installment.monthlyPayment()),
                    MoneyFormatter.money(installment.closingBalance()));
        }
        System.out.println();
    }
}
