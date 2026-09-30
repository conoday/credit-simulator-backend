package com.nusantech.creditsimulator;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SheetRepository {
    private final Path path;

    public SheetRepository(Path path) {
        this.path = path;
    }

    public LinkedHashMap<String, CreditSheet> load() {
        if (!Files.exists(path)) {
            return new LinkedHashMap<>();
        }
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            Map<String, Object> root = SimpleJson.parseObject(content);
            Object rawSheets = root.get("sheets");
            if (!(rawSheets instanceof List<?> sheets)) {
                throw new ApplicationException("Field sheets pada file penyimpanan harus berupa array.");
            }
            LinkedHashMap<String, CreditSheet> result = new LinkedHashMap<>();
            for (Object rawSheet : sheets) {
                CreditSheet sheet = parseSheet(rawSheet);
                result.put(sheet.name(), sheet);
            }
            return result;
        } catch (IOException exception) {
            throw new ApplicationException("Tidak dapat membaca file sheet: " + path, exception);
        } catch (ApplicationException exception) {
            throw new ApplicationException("File sheet tidak valid: " + exception.getMessage(), exception);
        }
    }

    public void save(Map<String, CreditSheet> sheets) {
        List<Object> serializedSheets = new ArrayList<>();
        for (CreditSheet sheet : sheets.values()) {
            serializedSheets.add(toMap(sheet));
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", 1);
        root.put("sheets", serializedSheets);
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, SimpleJson.stringify(root) + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ApplicationException("Tidak dapat menyimpan file sheet: " + path, exception);
        }
    }

    private static Map<String, Object> toMap(CreditSheet sheet) {
        LoanInput input = sheet.result().input();
        Map<String, Object> inputMap = new LinkedHashMap<>();
        inputMap.put("vehicleType", input.vehicleType().displayName());
        inputMap.put("vehicleCondition", input.vehicleCondition().displayName());
        inputMap.put("vehicleYear", input.vehicleYear());
        inputMap.put("totalLoanAmount", input.totalLoanAmount());
        inputMap.put("loanTenure", input.loanTenureYears());
        inputMap.put("downPayment", input.downPayment());

        List<Object> installments = new ArrayList<>();
        for (AnnualInstallment installment : sheet.result().installments()) {
            Map<String, Object> installmentMap = new LinkedHashMap<>();
            installmentMap.put("year", installment.year());
            installmentMap.put("rate", installment.rate());
            installmentMap.put("openingBalance", installment.openingBalance());
            installmentMap.put("totalDue", installment.totalDue());
            installmentMap.put("monthlyPayment", installment.monthlyPayment());
            installmentMap.put("closingBalance", installment.closingBalance());
            installments.add(installmentMap);
        }
        Map<String, Object> sheetMap = new LinkedHashMap<>();
        sheetMap.put("name", sheet.name());
        sheetMap.put("input", inputMap);
        sheetMap.put("installments", installments);
        return sheetMap;
    }

    private static CreditSheet parseSheet(Object rawSheet) {
        Map<String, Object> sheetMap = map(rawSheet, "sheet");
        String name = string(sheetMap.get("name"), "name");
        LoanInput input = InputParser.fromMap(map(sheetMap.get("input"), "input"));
        List<AnnualInstallment> installments = new ArrayList<>();
        Object rawInstallments = sheetMap.get("installments");
        if (!(rawInstallments instanceof List<?> list)) {
            throw new ApplicationException("installments harus berupa array.");
        }
        for (Object rawInstallment : list) {
            Map<String, Object> values = map(rawInstallment, "installment");
            installments.add(new AnnualInstallment(
                    integer(values.get("year"), "year"),
                    decimal(values.get("rate"), "rate"),
                    decimal(values.get("openingBalance"), "openingBalance"),
                    decimal(values.get("totalDue"), "totalDue"),
                    decimal(values.get("monthlyPayment"), "monthlyPayment"),
                    decimal(values.get("closingBalance"), "closingBalance")));
        }
        if (installments.size() != input.loanTenureYears()) {
            throw new ApplicationException("Jumlah hasil installment tidak sesuai tenor.");
        }
        return new CreditSheet(name, new CalculationResult(input, installments));
    }

    private static Map<String, Object> map(Object value, String field) {
        if (!(value instanceof Map<?, ?> raw)) {
            throw new ApplicationException(field + " harus berupa object.");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new ApplicationException(field + " memiliki nama field yang tidak valid.");
            }
            result.put(key, entry.getValue());
        }
        return result;
    }

    private static String string(Object value, String field) {
        if (value == null || String.valueOf(value).isBlank()) {
            throw new ApplicationException(field + " wajib diisi.");
        }
        return String.valueOf(value);
    }

    private static int integer(Object value, String field) {
        try {
            return new BigDecimal(String.valueOf(value)).intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new ApplicationException(field + " harus berupa integer.", exception);
        }
    }

    private static BigDecimal decimal(Object value, String field) {
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new ApplicationException(field + " harus berupa angka.", exception);
        }
    }
}
