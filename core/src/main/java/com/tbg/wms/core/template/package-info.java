/**
 * Template abstractions and ZPL placeholder rendering engine.
 *
 * <p><strong>Package Responsibility</strong></p>
 * <ul>
 *   <li>Define immutable label-template contracts.</li>
 *   <li>Render placeholder-backed output deterministically.</li>
 *   <li>Keep template concerns isolated from data retrieval and printer routing.</li>
 * </ul>
 *
 * <p><strong>Key Types</strong></p>
 * <ul>
 *   <li>{@link com.tbg.wms.core.template.LabelTemplate} - immutable template descriptor and content holder.</li>
 *   <li>{@link com.tbg.wms.core.template.ZplTemplateEngine} - placeholder substitution engine for ZPL templates.</li>
 * </ul>
 *
 * @since 1.5.0
 */
package com.tbg.wms.core.template;
