/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ZPL label generation engine for template-driven label production.
 *
 * Takes a template and field values, performs substitution, and produces
 * deterministic ZPL output suitable for Zebra printers.
 *
 * Features:
 * - Placeholder substitution with validation
 * - Field length validation
 * - Special character escaping for ZPL
 * - Deterministic output (same input → same output)
 */
public final class ZplTemplateEngine {

    private static final Logger log = LoggerFactory.getLogger(ZplTemplateEngine.class);

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");

    /**
     * Generates a ZPL label from a template and field values.
     *
     * @param template the label template
     * @param fields map of placeholder names to values
     * @return generated ZPL label
     * @throws IllegalArgumentException if required fields are missing
     * @throws IllegalArgumentException if field values are invalid
     */
    public static String generate(LabelTemplate template, Map<String, String> fields) {
        Objects.requireNonNull(template, "template cannot be null");
        Objects.requireNonNull(fields, "fields cannot be null");

        validateRequiredFields(template, fields);
        validateFieldLengths(fields);

        String result = template.getTemplateContent();

        // Replace all placeholders
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String fieldValue = fields.get(fieldName);

            if (fieldValue == null) {
                throw new IllegalArgumentException("Missing required field: " + fieldName);
            }

            String escapedValue = escapeZpl(fieldValue);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(escapedValue));
        }
        matcher.appendTail(sb);

        result = sb.toString();

        log.debug("Generated ZPL label from template: {}", template.getName());
        return result;
    }

    /**
     * Validates that all required template fields are present in the provided map.
     *
     * @param template the template defining required fields
     * @param fields the provided field values
     * @throws IllegalArgumentException if required fields are missing
     */
    private static void validateRequiredFields(LabelTemplate template, Map<String, String> fields) {
        for (String placeholderName : template.getPlaceholders().keySet()) {
            if (!fields.containsKey(placeholderName)) {
                throw new IllegalArgumentException("Missing required field: " + placeholderName);
            }

            String value = fields.get(placeholderName);
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("Field cannot be empty: " + placeholderName);
            }
        }
    }

    /**
     * Validates field length constraints.
     *
     * Maximum ZPL field length is typically 255 characters for most commands.
     *
     * @param fields the fields to validate
     * @throws IllegalArgumentException if any field exceeds maximum length
     */
    private static void validateFieldLengths(Map<String, String> fields) {
        final int MAX_FIELD_LENGTH = 255;

        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String value = entry.getValue();
            if (value != null && value.length() > MAX_FIELD_LENGTH) {
                throw new IllegalArgumentException(
                        "Field '" + entry.getKey() + "' exceeds maximum length of " +
                                MAX_FIELD_LENGTH + " characters (length: " + value.length() + ")"
                );
            }
        }
    }

    /**
     * Escapes special characters in field values for ZPL format.
     *
     * ZPL special characters that need escaping:
     * - ^ (caret) - ZPL command prefix
     * - ~ (tilde) - ZPL control character
     * - { } (braces) - Our template markers
     *
     * @param value the raw field value
     * @return escaped value safe for ZPL
     */
    private static String escapeZpl(String value) {
        return value
                .replace("^", "~~^")  // Escape caret
                .replace("~", "~~")   // Escape tilde (must be done after caret)
                .replace("{", "{{")   // Escape opening brace
                .replace("}", "}}");  // Escape closing brace
    }

    /**
     * Validates that a ZPL label contains the expected template markers.
     *
     * @param zplContent the ZPL content to validate
     * @return true if ZPL appears valid
     */
    public static boolean isValidZpl(String zplContent) {
        Objects.requireNonNull(zplContent, "zplContent cannot be null");

        // Check for required ZPL frame
        boolean hasStart = zplContent.contains("^XA");
        boolean hasEnd = zplContent.contains("^XZ");
        boolean hasNoUnescapedPlaceholders = !PLACEHOLDER_PATTERN.matcher(zplContent).find();

        return hasStart && hasEnd && hasNoUnescapedPlaceholders;
    }

    private ZplTemplateEngine() {
        // Utility class - prevent instantiation
    }
}

