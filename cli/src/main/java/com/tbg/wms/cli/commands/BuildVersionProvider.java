/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.5.2
 */
package com.tbg.wms.cli.commands;

import com.tbg.wms.core.update.VersionSupport;
import picocli.CommandLine.IVersionProvider;

/**
 * Reads CLI version from build-filtered classpath metadata.
 */
public final class BuildVersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        String runtimeVersion = VersionSupport.resolveRuntimeVersion(
                BuildVersionProvider.class,
                "wms.tags.version",
                "version",
                "/version.txt"
        );
        if (!runtimeVersion.isBlank()) {
            return new String[]{runtimeVersion};
        }
        return new String[]{"unknown"};
    }
}
