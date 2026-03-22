/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.RuntimePathResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Locates and launches packaged install-maintenance scripts.
 *
 * <p>This service keeps script discovery and detached process launch behavior out of the GUI shell
 * so install/uninstall support remains testable and easier to adjust for packaging changes.</p>
 */
public final class InstallMaintenanceService {
    public Optional<Path> findInstallScript(Class<?> anchorType) {
        Path appHome = RuntimePathResolver.resolveAppHome(anchorType);
        List<Path> candidates = List.of(
                appHome.resolve("scripts").resolve("install-wms-installer.ps1"),
                appHome.resolve("install-wms-installer.ps1"),
                Path.of("dist", "install-wms-installer.ps1"),
                Path.of("scripts", "install-wms-installer.ps1")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                return Optional.of(candidate.toAbsolutePath().normalize());
            }
        }
        return Optional.empty();
    }

    public Optional<Path> findUninstallScript(Class<?> anchorType) {
        Path appHome = RuntimePathResolver.resolveAppHome(anchorType);
        List<Path> candidates = List.of(
                appHome.resolve("scripts").resolve("uninstall-wms-tags.ps1"),
                appHome.resolve("uninstall-wms-tags.ps1"),
                Path.of("dist", "uninstall-wms-tags.ps1"),
                Path.of("scripts", "uninstall-wms-tags.ps1")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                return Optional.of(candidate.toAbsolutePath().normalize());
            }
        }
        return Optional.empty();
    }

    public void launchUninstall(Path scriptPath, boolean wipeInstallRoot) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("powershell");
        command.add("-ExecutionPolicy");
        command.add("Bypass");
        command.add("-File");
        command.add(scriptPath.toString());
        if (wipeInstallRoot) {
            command.add("-WipeInstallRoot");
            command.add("-RemoveRuntimeSettings");
        }
        new ProcessBuilder(command)
                .directory(scriptPath.getParent().toFile())
                .start();
    }

    public void launchInstaller(Path scriptPath, Path installerPath) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("powershell");
        command.add("-ExecutionPolicy");
        command.add("Bypass");
        command.add("-File");
        command.add(scriptPath.toString());
        command.add("-InstallerPath");
        command.add(installerPath.toString());
        new ProcessBuilder(command)
                .directory(scriptPath.getParent().toFile())
                .start();
    }
}
