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

public final class InputParser {
    private InputParser() {
    }

    public static LoanInput parseFile(Path path) {
        final String content;
        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ApplicationException("Tidak dapat membaca file input: " + path, exception);
        }
        String trimmed = (content.startsWith("\uFEFF") ? content.substring(1) : content).trim();
        if (trimmed.isEmpty()) {
            throw new ApplicationException("File input kosong.");
        }
        if (trimmed.startsWith("{")) {
            return fromMap(SimpleJson.parseObject(trimmed));
        }
        return parseTextLines(trimmed);
    }

    public static LoanInput fromMap(Map<String, Object> values) {
        if (values == null) {
            throw new ApplicationException("Data input tidak tersedia.");
        }
        String vehicleType = text(value(values, "vehicleType"));
        String vehicleCondition = text(value(values, "vehicleCondition"));
        Object rawYear = value(values, "vehicleYear");
        if (rawYear == null || !rawYear.toString().trim().matches("[0-9]{4}")) {
            throw new ApplicationException("Tahun kendaraan harus berupa 4 digit.");
        }
        int vehicleYear = integer(rawYear, "vehicleYear");
        BigDecimal totalLoanAmount = decimal(value(values, "totalLoanAmount"), "totalLoanAmount");
        int loanTenure = integer(value(values, "loanTenure"), "loanTenure");
        BigDecimal downPayment = decimal(value(values, "downPayment"), "downPayment");
        return new LoanInput(
                VehicleType.parse(vehicleType),
                VehicleCondition.parse(vehicleCondition),
                vehicleYear,
                totalLoanAmount,
                loanTenure,
                downPayment);
    }

    private static LoanInput parseTextLines(String content) {
        List<String> lines = new ArrayList<>();
        Map<String, Object> keyedValues = new LinkedHashMap<>();
        boolean hasKeyValue = false;
        for (String rawLine : content.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int separator = line.indexOf('=');
            if (separator > 0) {
                hasKeyValue = true;
                String key = line.substring(0, separator).trim();
                String rawValue = line.substring(separator + 1).trim();
                if (keyedValues.keySet().stream().anyMatch(existing -> existing.equalsIgnoreCase(key))) {
                    throw new ApplicationException("Field input berulang: " + key);
                }
                keyedValues.put(key, parseScalar(rawValue));
            } else {
                lines.add(line);
            }
        }
        if (hasKeyValue) {
            if (!lines.isEmpty()) {
                throw new ApplicationException("File input tidak boleh mencampur format key=value dan baris berurutan.");
            }
            return fromMap(keyedValues);
        }
        if (lines.size() != 6) {
            throw new ApplicationException("Format baris input membutuhkan tepat 6 baris non-empty.");
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("vehicleType", lines.get(0));
        values.put("vehicleCondition", lines.get(1));
        values.put("vehicleYear", lines.get(2));
        values.put("totalLoanAmount", lines.get(3));
        values.put("loanTenure", lines.get(4));
        values.put("downPayment", lines.get(5));
        return fromMap(values);
    }

    private static Object parseScalar(String value) {
        if (value.isEmpty()) {
            return value;
        }
        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static Object value(Map<String, Object> values, String expectedKey) {
        Object result = null;
        boolean found = false;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(expectedKey)) {
                if (found) {
                    throw new ApplicationException("Field input berulang: " + expectedKey);
                }
                found = true;
                result = entry.getValue();
            }
        }
        if (found) {
            return result;
        }
        throw new ApplicationException("Field input wajib diisi: " + expectedKey);
    }

    private static String text(Object value) {
        if (!(value instanceof String string)) {
            throw new ApplicationException("Jenis dan kondisi kendaraan harus berupa teks.");
        }
        return string.trim();
    }

    private static int integer(Object value, String field) {
        try {
            if (value instanceof Number number) {
                return new BigDecimal(number.toString()).intValueExact();
            }
            if (value == null || !value.toString().trim().matches("[0-9]+")) {
                throw new NumberFormatException();
            }
            return new BigDecimal(String.valueOf(value).trim()).intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new ApplicationException(field + " harus berupa bilangan bulat.", exception);
        }
    }

    private static BigDecimal decimal(Object value, String field) {
        try {
            return value instanceof BigDecimal decimal
                    ? decimal : new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            throw new ApplicationException(field + " harus berupa angka.", exception);
        }
    }
}
