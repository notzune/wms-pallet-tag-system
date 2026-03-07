/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.5.2
 */
package com.tbg.wms.cli.commands;

import picocli.CommandLine.IVersionProvider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Reads CLI version from build-filtered classpath metadata.
 */
public final class BuildVersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        try (InputStream in = BuildVersionProvider.class.getResourceAsStream("/version.txt")) {
            if (in != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line = reader.readLine();
                    if (line != null && !line.isBlank()) {
                        return new String[]{line.trim()};
                    }
                }
            }
        }
        Package pkg = BuildVersionProvider.class.getPackage();
        String implementationVersion = pkg == null ? null : pkg.getImplementationVersion();
        if (implementationVersion != null && !implementationVersion.isBlank()) {
            return new String[]{implementationVersion.trim()};
        }
        return new String[]{"unknown"};
    }
}
