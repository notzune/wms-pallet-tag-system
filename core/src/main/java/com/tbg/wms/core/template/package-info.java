/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

/**
 * ZPL label template engine for generating shipping labels.
 *
 * This package provides template-driven label generation with placeholder
 * substitution, field validation, and ZPL special character escaping.
 *
 * Key Components:
 * <ul>
 *   <li>{@link com.tbg.wms.core.template.LabelTemplate} - Template representation
 *   <li>{@link com.tbg.wms.core.template.ZplTemplateEngine} - ZPL generation engine
 * </ul>
 *
 * Usage Example:
 * <pre>
 * // Define template with placeholders
 * LabelTemplate template = new LabelTemplate("PALLET_LABEL",
 *     "^XA\n" +
 *     "^FO10,10^A0N,25,25^FD{shipToName}^FS\n" +
 *     "^FO10,40^A0N,20,20^FD{lpnId}^FS\n" +
 *     "^BY2,3,50^BC^FD{sscc}^FS\n" +
 *     "^XZ"
 * );
 *
 * // Prepare field values
 * Map&lt;String, String&gt; fields = new HashMap&lt;&gt;();
 * fields.put("shipToName", "Acme Corp");
 * fields.put("lpnId", "LPN001");
 * fields.put("sscc", "123456789012");
 *
 * // Generate ZPL label
 * String zpl = ZplTemplateEngine.generate(template, fields);
 *
 * // Validate output
 * boolean isValid = ZplTemplateEngine.isValidZpl(zpl);
 * </pre>
 *
 * Template Syntax:
 * <ul>
 *   <li>Placeholders marked with curly braces: {fieldName}
 *   <li>Field names: alphanumeric and underscore, must start with letter
 *   <li>ZPL commands begin with caret (^)
 *   <li>Valid ZPL must have ^XA (start) and ^XZ (end)
 * </ul>
 *
 * Field Validation:
 * <ul>
 *   <li>All template placeholders must have corresponding field values
 *   <li>No field can be null or empty
 *   <li>Maximum field length: 255 characters
 *   <li>Special characters escaped for ZPL safety
 * </ul>
 *
 * Special Character Escaping:
 * <ul>
 *   <li>^ (caret) - ZPL command prefix -> ~~^</li>
 *   <li>~ (tilde) - ZPL control character -> ~~</li>
 *   <li>{ (brace) - Template marker -> {{</li>
 *   <li>} (brace) - Template marker -> }}</li>
 * </ul>
 *
 * Design Properties:
 * <ul>
 *   <li>Deterministic: Same input always produces identical output
 *   <li>Immutable: Templates cannot be modified after creation
 *   <li>Stateless: Engine methods have no side effects
 *   <li>Type-safe: Template parsing validates placeholder syntax
 * </ul>
 *
 * @author Zeyad Rashed
 * @version 1.1
 * @since 1.0.0
 */
package com.tbg.wms.core.template;
