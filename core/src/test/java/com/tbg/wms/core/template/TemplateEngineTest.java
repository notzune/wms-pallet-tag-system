/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.template;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LabelTemplate and ZplTemplateEngine.
 *
 * Tests template creation, placeholder extraction, validation,
 * and ZPL generation with field substitution and escaping.
 */
class TemplateEngineTest {

    private static final String SIMPLE_TEMPLATE = "^XA\n^FO10,10^A0N,25,25^FD{shipToName}^FS\n^XZ";
    private static final String COMPLEX_TEMPLATE = "^XA\n" +
            "^FO10,10^A0N,25,25^FD{shipToName}^FS\n" +
            "^FO10,40^A0N,20,20^FD{lpnId}^FS\n" +
            "^FO10,70^A0N,20,20^FD{sku}^FS\n" +
            "^BY2,3,50^BC^FD{sscc}^FS\n" +
            "^XZ";

    // ===== LabelTemplate Tests =====

    @Test
    void testLabelTemplateCreation() {
        LabelTemplate template = new LabelTemplate("TEST", SIMPLE_TEMPLATE);

        assertEquals("TEST", template.getName());
        assertEquals(SIMPLE_TEMPLATE, template.getTemplateContent());
        assertTrue(template.hasPlaceholder("shipToName"));
        assertEquals(1, template.getPlaceholderCount());
    }

    @Test
    void testLabelTemplateExtractsMultiplePlaceholders() {
        LabelTemplate template = new LabelTemplate("COMPLEX", COMPLEX_TEMPLATE);

        assertEquals(4, template.getPlaceholderCount());
        assertTrue(template.hasPlaceholder("shipToName"));
        assertTrue(template.hasPlaceholder("lpnId"));
        assertTrue(template.hasPlaceholder("sku"));
        assertTrue(template.hasPlaceholder("sscc"));
    }

    @Test
    void testLabelTemplateRejectsNullName() {
        assertThrows(NullPointerException.class,
                () -> new LabelTemplate(null, SIMPLE_TEMPLATE));
    }

    @Test
    void testLabelTemplateRejectsEmptyName() {
        assertThrows(IllegalArgumentException.class,
                () -> new LabelTemplate("", SIMPLE_TEMPLATE));

        assertThrows(IllegalArgumentException.class,
                () -> new LabelTemplate("   ", SIMPLE_TEMPLATE));
    }

    @Test
    void testLabelTemplateRejectsNullContent() {
        assertThrows(NullPointerException.class,
                () -> new LabelTemplate("TEST", null));
    }

    @Test
    void testLabelTemplateRejectsEmptyContent() {
        assertThrows(IllegalArgumentException.class,
                () -> new LabelTemplate("TEST", ""));

        assertThrows(IllegalArgumentException.class,
                () -> new LabelTemplate("TEST", "   "));
    }

    @Test
    void testLabelTemplateRejectsUnclosedPlaceholder() {
        String badTemplate = "^XA^FD{shipToName^FS^XZ";
        assertThrows(IllegalArgumentException.class,
                () -> new LabelTemplate("BAD", badTemplate),
                "Should reject unclosed placeholder");
    }

    @Test
    void testLabelTemplateRejectsEmptyPlaceholder() {
        String badTemplate = "^XA^FD{}^FS^XZ";
        assertThrows(IllegalArgumentException.class,
                () -> new LabelTemplate("BAD", badTemplate),
                "Should reject empty placeholder");
    }

    @Test
    void testLabelTemplateRejectsInvalidPlaceholderName() {
        String badTemplate = "^XA^FD{123invalid}^FS^XZ";
        assertThrows(IllegalArgumentException.class,
                () -> new LabelTemplate("BAD", badTemplate),
                "Should reject placeholder starting with number");
    }

    // ===== ZplTemplateEngine Tests =====

    @Test
    void testGenerateSimpleLabel() {
        LabelTemplate template = new LabelTemplate("SIMPLE", SIMPLE_TEMPLATE);
        Map<String, String> fields = new HashMap<>();
        fields.put("shipToName", "Acme Corp");

        String result = ZplTemplateEngine.generate(template, fields);

        assertTrue(result.contains("Acme Corp"));
        assertTrue(result.contains("^XA"));
        assertTrue(result.contains("^XZ"));
        assertFalse(result.contains("{shipToName}"));
    }

    @Test
    void testGenerateComplexLabel() {
        LabelTemplate template = new LabelTemplate("COMPLEX", COMPLEX_TEMPLATE);
        Map<String, String> fields = new HashMap<>();
        fields.put("shipToName", "Acme Corp");
        fields.put("lpnId", "LPN001");
        fields.put("sku", "SKU123");
        fields.put("sscc", "123456789012");

        String result = ZplTemplateEngine.generate(template, fields);

        assertTrue(result.contains("Acme Corp"));
        assertTrue(result.contains("LPN001"));
        assertTrue(result.contains("SKU123"));
        assertTrue(result.contains("123456789012"));
        assertFalse(result.contains("{"));
        assertFalse(result.contains("}"));
    }

    @Test
    void testGenerateIsDeterministic() {
        LabelTemplate template = new LabelTemplate("SIMPLE", SIMPLE_TEMPLATE);
        Map<String, String> fields = new HashMap<>();
        fields.put("shipToName", "Test Name");

        String result1 = ZplTemplateEngine.generate(template, fields);
        String result2 = ZplTemplateEngine.generate(template, fields);

        assertEquals(result1, result2, "Same input should produce identical output");
    }

    @Test
    void testGenerateRequiresAllFields() {
        LabelTemplate template = new LabelTemplate("TEST", COMPLEX_TEMPLATE);
        Map<String, String> fields = new HashMap<>();
        fields.put("shipToName", "Acme");
        // Missing other required fields

        assertThrows(IllegalArgumentException.class,
                () -> ZplTemplateEngine.generate(template, fields));
    }

    @Test
    void testGenerateRejectsNullTemplate() {
        Map<String, String> fields = new HashMap<>();
        assertThrows(NullPointerException.class,
                () -> ZplTemplateEngine.generate(null, fields));
    }

    @Test
    void testGenerateRejectsNullFields() {
        LabelTemplate template = new LabelTemplate("TEST", SIMPLE_TEMPLATE);
        assertThrows(NullPointerException.class,
                () -> ZplTemplateEngine.generate(template, null));
    }

    @Test
    void testGenerateAllowsEmptyFieldValue() {
        LabelTemplate template = new LabelTemplate("TEST", SIMPLE_TEMPLATE);
        Map<String, String> fields = new HashMap<>();
        fields.put("shipToName", "");

        String zpl = ZplTemplateEngine.generate(template, fields);
        assertNotNull(zpl);
    }

    @Test
    void testGenerateRejectsNullFieldValue() {
        LabelTemplate template = new LabelTemplate("TEST", SIMPLE_TEMPLATE);
        Map<String, String> fields = new HashMap<>();
        fields.put("shipToName", null);

        assertThrows(IllegalArgumentException.class,
                () -> ZplTemplateEngine.generate(template, fields));
    }

    @Test
    void testGenerateValidatesFieldLength() {
        LabelTemplate template = new LabelTemplate("TEST", SIMPLE_TEMPLATE);
        Map<String, String> fields = new HashMap<>();
        // Create field value longer than 255 characters
        fields.put("shipToName", "A".repeat(256));

        assertThrows(IllegalArgumentException.class,
                () -> ZplTemplateEngine.generate(template, fields),
                "Should reject fields exceeding 255 characters");
    }

    @Test
    void testEscapesCaretCharacter() {
        LabelTemplate template = new LabelTemplate("TEST", "^XA^FD{value}^FS^XZ");
        Map<String, String> fields = new HashMap<>();
        fields.put("value", "Test^Value");

        String result = ZplTemplateEngine.generate(template, fields);

        assertTrue(result.contains("Test~~^Value"), "Caret should be escaped");
    }

    @Test
    void testEscapesTildeCharacter() {
        LabelTemplate template = new LabelTemplate("TEST", "^XA^FD{value}^FS^XZ");
        Map<String, String> fields = new HashMap<>();
        fields.put("value", "Test~Value");

        String result = ZplTemplateEngine.generate(template, fields);

        assertTrue(result.contains("Test~~Value"), "Tilde should be escaped");
    }

    @Test
    void testEscapesBraceCharacters() {
        LabelTemplate template = new LabelTemplate("TEST", "^XA^FD{value}^FS^XZ");
        Map<String, String> fields = new HashMap<>();
        fields.put("value", "Test{Value}");

        String result = ZplTemplateEngine.generate(template, fields);

        assertTrue(result.contains("Test{{Value}}"), "Braces should be escaped");
    }

    @Test
    void testValidZplDetectsValidTemplate() {
        String validZpl = "^XA\n^FO10,10^A0N,25,25^FDHello^FS\n^XZ";
        assertTrue(ZplTemplateEngine.isValidZpl(validZpl));
    }

    @Test
    void testValidZplRejectsMissingStart() {
        String invalidZpl = "^FO10,10^A0N,25,25^FDHello^FS\n^XZ";
        assertFalse(ZplTemplateEngine.isValidZpl(invalidZpl));
    }

    @Test
    void testValidZplRejectsMissingEnd() {
        String invalidZpl = "^XA\n^FO10,10^A0N,25,25^FDHello^FS";
        assertFalse(ZplTemplateEngine.isValidZpl(invalidZpl));
    }

    @Test
    void testValidZplRejectsUnresolvedPlaceholders() {
        String invalidZpl = "^XA\n^FO10,10^A0N,25,25^FD{name}^FS\n^XZ";
        assertFalse(ZplTemplateEngine.isValidZpl(invalidZpl));
    }

    @Test
    void testValidZplRejectsNull() {
        assertThrows(NullPointerException.class,
                () -> ZplTemplateEngine.isValidZpl(null));
    }
}

