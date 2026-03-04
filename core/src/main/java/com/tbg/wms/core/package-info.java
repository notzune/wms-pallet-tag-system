/**
 * Core domain and infrastructure services shared across CLI and GUI modules.
 *
 * <p><strong>Package Responsibility</strong></p>
 * <ul>
 *   <li>Provide UI-agnostic business services and data contracts.</li>
 *   <li>Define canonical configuration and behavior used by CLI and GUI modules.</li>
 *   <li>Keep business logic independent from command parsing and Swing concerns.</li>
 * </ul>
 *
 * <p><strong>Key Types</strong></p>
 * <ul>
 *   <li>{@link com.tbg.wms.core.AppConfig} - immutable runtime configuration model.</li>
 * </ul>
 *
 * @since 1.5.0
 */
package com.tbg.wms.core;
