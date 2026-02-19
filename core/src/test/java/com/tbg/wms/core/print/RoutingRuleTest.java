/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.print;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link RoutingRule}.
 */
class RoutingRuleTest {

    @Test
    void matches_shouldReturnTrue_whenEqualsOperatorMatches() {
        // Arrange
        RoutingRule rule = new RoutingRule("test-rule", true,
                "stagingLocation", "EQUALS", "ROSSI", "DISPATCH");
        Map<String, String> context = Map.of("stagingLocation", "ROSSI");

        // Act
        boolean matches = rule.matches(context);

        // Assert
        assertTrue(matches);
    }

    @Test
    void matches_shouldBeCaseInsensitive_forEqualsOperator() {
        // Arrange
        RoutingRule rule = new RoutingRule("test-rule", true,
                "stagingLocation", "EQUALS", "ROSSI", "DISPATCH");
        Map<String, String> context = Map.of("stagingLocation", "rossi");

        // Act
        boolean matches = rule.matches(context);

        // Assert
        assertTrue(matches);
    }

    @Test
    void matches_shouldReturnFalse_whenEqualsOperatorDoesNotMatch() {
        // Arrange
        RoutingRule rule = new RoutingRule("test-rule", true,
                "stagingLocation", "EQUALS", "ROSSI", "DISPATCH");
        Map<String, String> context = Map.of("stagingLocation", "OFFICE");

        // Act
        boolean matches = rule.matches(context);

        // Assert
        assertFalse(matches);
    }

    @Test
    void matches_shouldReturnFalse_whenFieldNotInContext() {
        // Arrange
        RoutingRule rule = new RoutingRule("test-rule", true,
                "stagingLocation", "EQUALS", "ROSSI", "DISPATCH");
        Map<String, String> context = Map.of("otherField", "value");

        // Act
        boolean matches = rule.matches(context);

        // Assert
        assertFalse(matches);
    }

    @Test
    void matches_shouldReturnFalse_whenRuleDisabled() {
        // Arrange
        RoutingRule rule = new RoutingRule("test-rule", false,
                "stagingLocation", "EQUALS", "ROSSI", "DISPATCH");
        Map<String, String> context = Map.of("stagingLocation", "ROSSI");

        // Act
        boolean matches = rule.matches(context);

        // Assert
        assertFalse(matches);
    }

    @Test
    void matches_shouldSupportStartsWithOperator() {
        // Arrange
        RoutingRule rule = new RoutingRule("test-rule", true,
                "location", "STARTS_WITH", "DOCK", "FLOOR");
        Map<String, String> context = Map.of("location", "DOCK_A1");

        // Act
        boolean matches = rule.matches(context);

        // Assert
        assertTrue(matches);
    }

    @Test
    void matches_shouldSupportContainsOperator() {
        // Arrange
        RoutingRule rule = new RoutingRule("test-rule", true,
                "location", "CONTAINS", "TEMP", "OFFICE");
        Map<String, String> context = Map.of("location", "DOCK_TEMP_01");

        // Act
        boolean matches = rule.matches(context);

        // Assert
        assertTrue(matches);
    }

    @Test
    void matches_shouldThrowException_forUnsupportedOperator() {
        // Arrange
        RoutingRule rule = new RoutingRule("test-rule", true,
                "location", "REGEX", ".*", "OFFICE");
        Map<String, String> context = Map.of("location", "TEST");

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> rule.matches(context));
    }

    @Test
    void constructor_shouldRequireNonNullParameters() {
        assertThrows(NullPointerException.class,
                () -> new RoutingRule(null, true, "field", "op", "value", "printer"));
        assertThrows(NullPointerException.class,
                () -> new RoutingRule("id", true, null, "op", "value", "printer"));
        assertThrows(NullPointerException.class,
                () -> new RoutingRule("id", true, "field", null, "value", "printer"));
        assertThrows(NullPointerException.class,
                () -> new RoutingRule("id", true, "field", "op", null, "printer"));
        assertThrows(NullPointerException.class,
                () -> new RoutingRule("id", true, "field", "op", "value", null));
    }
}

