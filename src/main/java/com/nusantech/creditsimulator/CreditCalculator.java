package com.nusantech.creditsimulator;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class CreditCalculator {
    private static final BigDecimal YEARLY_INCREASE = new BigDecimal("0.001");
    private static final BigDecimal TWO_YEAR_INCREASE = new BigDecimal("0.004");
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final MathContext CALCULATION_CONTEXT = new MathContext(28, RoundingMode.HALF_UP);

    public CalculationResult calculate(LoanInput input, int currentYear) {
        LoanValidator.validate(input, currentYear);

        BigDecimal balance = input.totalLoanAmount().subtract(input.downPayment(), CALCULATION_CONTEXT);
        List<AnnualInstallment> installments = new ArrayList<>();

        for (int year = 1; year <= input.loanTenureYears(); year++) {
            BigDecimal rate = rateFor(input.vehicleType(), year);
            BigDecimal totalDue = balance.multiply(BigDecimal.ONE.add(rate), CALCULATION_CONTEXT);
            int remainingYears = input.loanTenureYears() - year + 1;
            BigDecimal monthlyPayment = totalDue.divide(
                    TWELVE.multiply(BigDecimal.valueOf(remainingYears)), 12, RoundingMode.HALF_UP);
            BigDecimal closingBalance = totalDue.subtract(
                    monthlyPayment.multiply(TWELVE, CALCULATION_CONTEXT), CALCULATION_CONTEXT);

            installments.add(new AnnualInstallment(
                    year,
                    rate,
                    scaleForMoney(balance),
                    scaleForMoney(totalDue),
                    scaleForMoney(monthlyPayment),
                    scaleForMoney(closingBalance)));
            balance = closingBalance;
        }

        return new CalculationResult(input, installments);
    }

    public BigDecimal rateFor(VehicleType vehicleType, int year) {
        if (vehicleType == null || year < 1 || year > 6) {
            throw new ValidationException("Rate hanya tersedia untuk tahun 1 sampai 6.");
        }
        BigDecimal yearOffset = BigDecimal.valueOf(year - 1L);
        BigDecimal twoYearBlocks = BigDecimal.valueOf((year - 1L) / 2L);
        return vehicleType.baseRate()
                .add(YEARLY_INCREASE.multiply(yearOffset))
                .add(TWO_YEAR_INCREASE.multiply(twoYearBlocks));
    }

    private static BigDecimal scaleForMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
