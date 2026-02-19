/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.template;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents a ZPL label template with placeholders for dynamic field substitution.
 *
 * Templates are immutable and can be reused for multiple label generations.
 * Placeholders are marked with curly braces: {fieldName}
 *
 * Example template:
 * <pre>
 * ^XA
 * ^FO10,10^A0N,25,25^FD{shipToName}^FS
 * ^FO10,40^A0N,20,20^FD{lpnId}^FS
 * ^BY2,3,50^BC^FD{sscc}^FS
 * ^XZ
 * </pre>
 */
public final class LabelTemplate {

    private static final Pattern PLACEHOLDER_NAME_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    private final String name;
    private final String templateContent;
    private final Map<String, PlaceholderInfo> placeholders;

    /**
     * Creates a new LabelTemplate.
     *
     * @param name template name (e.g., "PALLET_LABEL", "SHIPPING_LABEL")
     * @param templateContent ZPL template with {placeholder} markers
     * @throws IllegalArgumentException if name is empty or template is empty
     * @throws IllegalArgumentException if template contains invalid placeholders
     */
    public LabelTemplate(String name, String templateContent) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.templateContent = Objects.requireNonNull(templateContent, "templateContent cannot be null");

        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("name cannot be empty");
        }
        if (templateContent.trim().isEmpty()) {
            throw new IllegalArgumentException("templateContent cannot be empty");
        }

        this.placeholders = extractPlaceholders(templateContent);
    }

    /**
     * Extracts all placeholders from the template content.
     *
     * @param content the template content to parse
     * @return map of placeholder names to their info
     * @throws IllegalArgumentException if invalid placeholder syntax found
     */
    private Map<String, PlaceholderInfo> extractPlaceholders(String content) {
        Map<String, PlaceholderInfo> result = new HashMap<>();
        int position = 0;
        int occurrenceCount = 0;

        while ((position = content.indexOf('{', position)) != -1) {
            int closePos = content.indexOf('}', position);
            if (closePos == -1) {
                throw new IllegalArgumentException("Unclosed placeholder at position " + position);
            }

            String placeholderName = content.substring(position + 1, closePos).trim();
            if (placeholderName.isEmpty()) {
                throw new IllegalArgumentException("Empty placeholder at position " + position);
            }

            if (!PLACEHOLDER_NAME_PATTERN.matcher(placeholderName).matches()) {
                throw new IllegalArgumentException("Invalid placeholder name: " + placeholderName);
            }

            occurrenceCount++;
            result.put(placeholderName, new PlaceholderInfo(placeholderName, position, occurrenceCount));
            position = closePos + 1;
        }

        return result;
    }

    /**
     * Gets the template name.
     *
     * @return template name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the original template content with placeholders.
     *
     * @return template content
     */
    public String getTemplateContent() {
        return templateContent;
    }

    /**
     * Gets all placeholder names required by this template.
     *
     * @return unmodifiable set of placeholder names
     */
    public Map<String, PlaceholderInfo> getPlaceholders() {
        return Collections.unmodifiableMap(placeholders);
    }

    /**
     * Gets the number of placeholders in the template.
     *
     * @return count of unique placeholders
     */
    public int getPlaceholderCount() {
        return placeholders.size();
    }

    /**
     * Checks if this template contains a specific placeholder.
     *
     * @param placeholderName the placeholder name to check
     * @return true if template contains this placeholder
     */
    public boolean hasPlaceholder(String placeholderName) {
        return placeholders.containsKey(placeholderName);
    }

    /**
     * Represents information about a placeholder in the template.
     */
    public static final class PlaceholderInfo {
        private final String name;
        private final int firstPosition;
        private final int occurrenceCount;

        public PlaceholderInfo(String name, int firstPosition, int occurrenceCount) {
            this.name = name;
            this.firstPosition = firstPosition;
            this.occurrenceCount = occurrenceCount;
        }

        public String getName() {
            return name;
        }

        public int getFirstPosition() {
            return firstPosition;
        }

        public int getOccurrenceCount() {
            return occurrenceCount;
        }
    }

    @Override
    public String toString() {
        return "LabelTemplate{" +
                "name='" + name + '\'' +
                ", placeholders=" + placeholders.size() +
                '}';
    }
}
