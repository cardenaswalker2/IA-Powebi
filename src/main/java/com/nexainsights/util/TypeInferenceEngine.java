package com.nexainsights.util;

import com.nexainsights.model.DataType;
import org.apache.commons.lang3.math.NumberUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class TypeInferenceEngine {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy")
    );

    private static final Pattern URL_PATTERN = Pattern.compile("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public static DataType inferSingleValueType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DataType.UNKNOWN;
        }

        String trimmed = value.trim();

        // Boolean
        if (trimmed.equalsIgnoreCase("true") || trimmed.equalsIgnoreCase("false")
                || trimmed.equalsIgnoreCase("si") || trimmed.equalsIgnoreCase("sí")
                || trimmed.equalsIgnoreCase("no")) {
            return DataType.BOOLEAN;
        }

        // Email / URL
        if (URL_PATTERN.matcher(trimmed).matches()) {
            return DataType.URL;
        }
        if (EMAIL_PATTERN.matcher(trimmed).matches()) {
            return DataType.EMAIL;
        }

        // Date check
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate.parse(trimmed, formatter);
                return DataType.DATE;
            } catch (DateTimeParseException ignored) {
            }
        }

        // Number check (soporta comas o puntos de decimales y símbolos de moneda o porcentajes)
        String cleanNum = trimmed.replace("$", "").replace("€", "").replace("£", "").replace("%", "").replace(" ", "").trim();
        // Soporta formatos como 1.234,56 o 1,234.56 o 1234.56 o -45.2%
        if (cleanNum.matches("^-?\\d{1,3}(,\\d{3})*(\\.\\d+)?$") ||
            cleanNum.matches("^-?\\d{1,3}(\\.\\d{3})*(,\\d+)?$") ||
            cleanNum.matches("^-?\\d+([.,]\\d+)?$")) {
            return DataType.NUMBER;
        }

        return DataType.TEXT;
    }

    public static Object parseValueToType(String rawValue, DataType type) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }
        String val = rawValue.trim();

        try {
            switch (type) {
                case NUMBER:
                    String cleanNum = val.replace("$", "").replace("€", "").replace("%", "").replace(" ", "");
                    if (cleanNum.contains(",") && !cleanNum.contains(".")) {
                        cleanNum = cleanNum.replace(",", ".");
                    } else if (cleanNum.contains(",") && cleanNum.contains(".")) {
                        cleanNum = cleanNum.replace(",", "");
                    }
                    if (cleanNum.contains(".")) {
                        return Double.parseDouble(cleanNum);
                    } else {
                        try {
                            return Long.parseLong(cleanNum);
                        } catch (NumberFormatException e) {
                            return Double.parseDouble(cleanNum);
                        }
                    }
                case BOOLEAN:
                    return val.equalsIgnoreCase("true") || val.equalsIgnoreCase("si") || val.equalsIgnoreCase("sí");
                case DATE:
                    for (DateTimeFormatter formatter : DATE_FORMATTERS) {
                        try {
                            return LocalDate.parse(val, formatter).toString();
                        } catch (DateTimeParseException ignored) {}
                    }
                    return val;
                default:
                    return val;
            }
        } catch (Exception e) {
            return val;
        }
    }
}
