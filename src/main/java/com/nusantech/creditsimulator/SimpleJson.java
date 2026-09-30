package com.nusantech.creditsimulator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A small JSON reader/writer for the fixed input and sheet formats. */
public final class SimpleJson {
    private SimpleJson() {
    }

    public static Map<String, Object> parseObject(String text) {
        Object value = new Parser(text).parse();
        if (!(value instanceof Map<?, ?> map)) {
            throw new ApplicationException("JSON harus berupa object.");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new ApplicationException("Nama field JSON harus berupa string.");
            }
            result.put(key, entry.getValue());
        }
        return result;
    }

    public static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out);
        return out.toString();
    }

    private static void write(Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String string) {
            out.append('"').append(escape(string)).append('"');
        } else if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<?, ?> entry = iterator.next();
                write(String.valueOf(entry.getKey()), out);
                out.append(':');
                write(entry.getValue(), out);
                if (iterator.hasNext()) {
                    out.append(',');
                }
            }
            out.append('}');
        } else if (value instanceof Iterable<?> iterable) {
            out.append('[');
            Iterator<?> iterator = iterable.iterator();
            while (iterator.hasNext()) {
                write(iterator.next(), out);
                if (iterator.hasNext()) {
                    out.append(',');
                }
            }
            out.append(']');
        } else {
            throw new ApplicationException("Tipe JSON tidak didukung: " + value.getClass().getName());
        }
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (character < 0x20) {
                        out.append(String.format("\\u%04x", (int) character));
                    } else {
                        out.append(character);
                    }
                }
            }
        }
        return out.toString();
    }

    private static final class Parser {
        private final String text;
        private int index;
        private int depth;

        private Parser(String text) {
            this.text = text == null ? "" : text;
        }

        private Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (index != text.length()) {
                fail("Karakter setelah JSON tidak valid");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= text.length()) {
                fail("JSON kosong");
            }
            if (++depth > 64) {
                fail("JSON terlalu dalam");
            }
            try {
                return switch (text.charAt(index)) {
                    case '{' -> parseObjectValue();
                    case '[' -> parseArrayValue();
                    case '"' -> parseString();
                    case 't' -> parseLiteral("true", Boolean.TRUE);
                    case 'f' -> parseLiteral("false", Boolean.FALSE);
                    case 'n' -> parseLiteral("null", null);
                    default -> parseNumber();
                };
            } finally {
                depth--;
            }
        }

        private Map<String, Object> parseObjectValue() {
            expect('{');
            Map<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (consume('}')) {
                return object;
            }
            while (true) {
                skipWhitespace();
                if (index >= text.length() || text.charAt(index) != '"') {
                    fail("Nama field JSON wajib berupa string");
                }
                String key = parseString();
                if (object.containsKey(key)) {
                    fail("Field JSON berulang: " + key);
                }
                skipWhitespace();
                expect(':');
                object.put(key, parseValue());
                skipWhitespace();
                if (consume('}')) {
                    return object;
                }
                expect(',');
            }
        }

        private List<Object> parseArrayValue() {
            expect('[');
            List<Object> array = new ArrayList<>();
            skipWhitespace();
            if (consume(']')) {
                return array;
            }
            while (true) {
                array.add(parseValue());
                skipWhitespace();
                if (consume(']')) {
                    return array;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (index < text.length()) {
                char character = text.charAt(index++);
                if (character == '"') {
                    return value.toString();
                }
                if (character != '\\') {
                    if (character < 0x20) {
                        fail("Karakter kontrol dalam string JSON harus di-escape");
                    }
                    value.append(character);
                    continue;
                }
                if (index >= text.length()) {
                    fail("Escape JSON tidak lengkap");
                }
                char escape = text.charAt(index++);
                switch (escape) {
                    case '"', '\\', '/' -> value.append(escape);
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> value.append(parseUnicodeEscape());
                    default -> fail("Escape JSON tidak dikenal: \\" + escape);
                }
            }
            fail("String JSON tidak ditutup");
            return "";
        }

        private char parseUnicodeEscape() {
            if (index + 4 > text.length()) {
                fail("Unicode escape JSON tidak lengkap");
            }
            String hex = text.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException exception) {
                throw new ApplicationException("Unicode escape JSON tidak valid", exception);
            }
        }

        private Object parseLiteral(String literal, Object value) {
            if (!text.startsWith(literal, index)) {
                fail("Literal JSON tidak valid");
            }
            index += literal.length();
            return value;
        }

        private Number parseNumber() {
            int start = index;
            consume('-');
            if (consume('0')) {
                if (index < text.length() && isDigit(text.charAt(index))) {
                    fail("Angka JSON tidak boleh memiliki nol di depan");
                }
            } else {
                consumeDigits();
            }
            if (consume('.')) {
                consumeDigits();
            }
            if (consume('e') || consume('E')) {
                if (!consume('+')) {
                    consume('-');
                }
                consumeDigits();
            }
            String number = text.substring(start, index);
            try {
                return new BigDecimal(number);
            } catch (NumberFormatException exception) {
                throw new ApplicationException("Angka JSON tidak valid: " + number, exception);
            }
        }

        private void consumeDigits() {
            int start = index;
            while (index < text.length() && isDigit(text.charAt(index))) {
                index++;
            }
            if (start == index) {
                fail("Angka JSON membutuhkan digit");
            }
        }

        private boolean consume(char expected) {
            if (index < text.length() && text.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!consume(expected)) {
                fail("JSON mengharapkan '" + expected + "'");
            }
        }

        private void skipWhitespace() {
            while (index < text.length() && " \t\r\n".indexOf(text.charAt(index)) >= 0) {
                index++;
            }
        }

        private static boolean isDigit(char value) {
            return value >= '0' && value <= '9';
        }

        private void fail(String message) {
            throw new ApplicationException(message + " pada posisi " + index + ".");
        }
    }
}
