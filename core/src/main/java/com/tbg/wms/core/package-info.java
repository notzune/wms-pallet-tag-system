/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
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
 *   <li>{@link com.tbg.wms.core.RuntimePathResolver} - shared runtime path resolver for jar-adjacent output directories.</li>
 * </ul>
 *
 * @since 1.5.0
 */
package com.tbg.wms.core;
