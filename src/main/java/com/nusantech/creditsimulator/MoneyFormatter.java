package com.nusantech.creditsimulator;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class MoneyFormatter {
    private static final DecimalFormat MONEY;
    private static final DecimalFormat RATE;

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        MONEY = new DecimalFormat("'Rp. '#,##0.00", symbols);
        RATE = new DecimalFormat("0.##%", symbols);
    }

    private MoneyFormatter() {
    }

    public static String money(BigDecimal amount) {
        synchronized (MONEY) {
            return MONEY.format(amount);
        }
    }

    public static String rate(BigDecimal rate) {
        synchronized (RATE) {
            return RATE.format(rate);
        }
    }
}
