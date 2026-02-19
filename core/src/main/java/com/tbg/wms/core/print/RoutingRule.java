/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.print;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * A printer routing rule that evaluates context to select a printer.
 *
 * Rules are evaluated in order. The first rule that matches determines
 * which printer to use. If no rules match, the default printer is used.
 *
 * Currently supports simple field equality checks. Future extensions may
 * add regex matching, prefix matching, or composite conditions.
 *
 * @since 1.0.0
 */
public final class RoutingRule {

    private final String id;
    private final boolean enabled;
    private final String field;
    private final String operator;
    private final String value;
    private final String printerId;

    /**
     * Creates a new routing rule.
     *
     * @param id unique rule identifier
     * @param enabled whether this rule is active
     * @param field context field to evaluate (e.g., "stagingLocation")
     * @param operator comparison operator (e.g., "EQUALS")
     * @param value expected value for the field
     * @param printerId printer ID to route to if rule matches
     */
    public RoutingRule(String id, boolean enabled, String field,
                      String operator, String value, String printerId) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.enabled = enabled;
        this.field = Objects.requireNonNull(field, "field cannot be null");
        this.operator = Objects.requireNonNull(operator, "operator cannot be null");
        this.value = Objects.requireNonNull(value, "value cannot be null");
        this.printerId = Objects.requireNonNull(printerId, "printerId cannot be null");
    }

    /**
     * Evaluates this rule against routing context.
     *
     * @param context field values to evaluate (e.g., {"stagingLocation": "ROSSI"})
     * @return true if rule matches, false otherwise
     */
    public boolean matches(Map<String, String> context) {
        if (!enabled) {
            return false;
        }

        String contextValue = context.get(field);
        if (contextValue == null) {
            return false;
        }

        String actual = contextValue.toUpperCase(Locale.ROOT);
        String expected = value.toUpperCase(Locale.ROOT);
        switch (operator.toUpperCase(Locale.ROOT)) {
            case "EQUALS":
                return expected.equals(actual);
            case "STARTS_WITH":
                return actual.startsWith(expected);
            case "CONTAINS":
                return actual.contains(expected);
            default:
                throw new IllegalStateException("Unsupported operator: " + operator);
        }
    }

    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getField() {
        return field;
    }

    public String getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }

    public String getPrinterId() {
        return printerId;
    }

    @Override
    public String toString() {
        return "RoutingRule{" +
                "id='" + id + '\'' +
                ", field='" + field + '\'' +
                ", op='" + operator + '\'' +
                ", value='" + value + '\'' +
                ", printerId='" + printerId + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}

