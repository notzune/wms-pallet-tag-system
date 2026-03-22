package com.tbg.wms.cli.gui;

import com.tbg.wms.core.exception.WmsException;

import javax.swing.Icon;
import java.awt.Color;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps DB connectivity state to footer-ready GUI text, tooltip, and LED color.
 */
final class GuiDbStatusSupport {
    private static final Pattern ORACLE_CODE_PATTERN = Pattern.compile("(ORA-\\d{5})", Pattern.CASE_INSENSITIVE);
    private static final int LED_SIZE = 10;
    private static final Color GREEN = new Color(40, 167, 69);
    private static final Color AMBER = new Color(214, 152, 36);
    private static final Color RED = new Color(200, 36, 36);

    StatusState checking(String oracleService) {
        String service = safeService(oracleService);
        return new StatusState(
                new StatusLedIcon(LED_SIZE, AMBER),
                "Checking - " + service,
                html("Oracle connectivity check in progress for " + service + ".")
        );
    }

    StatusState connected(String oracleService) {
        String service = safeService(oracleService);
        return new StatusState(
                new StatusLedIcon(LED_SIZE, GREEN),
                "Connected - " + service,
                html("Oracle connectivity verified for " + service + ".")
        );
    }

    Optional<StatusState> failure(String oracleService, Throwable throwable) {
        if (throwable == null) {
            return Optional.empty();
        }
        String code = extractOracleCode(throwable);
        boolean connectivityRelated = code != null || containsConnectivityException(throwable);
        if (!connectivityRelated) {
            return Optional.empty();
        }
        String shortText = code == null ? "Not connected" : "Not connected - " + code.toUpperCase();
        return Optional.of(new StatusState(
                new StatusLedIcon(LED_SIZE, RED),
                shortText,
                buildFailureTooltip(oracleService, throwable)
        ));
    }

    private boolean containsConnectivityException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof com.tbg.wms.core.exception.WmsDbConnectivityException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String extractOracleCode(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                Matcher matcher = ORACLE_CODE_PATTERN.matcher(message);
                if (matcher.find()) {
                    return matcher.group(1).toUpperCase();
                }
            }
            current = current.getCause();
        }
        return null;
    }

    private String buildFailureTooltip(String oracleService, Throwable throwable) {
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("Oracle connectivity failed for ").append(safeService(oracleService)).append('.');
        String message = GuiExceptionMessageSupport.rootMessage(throwable);
        if (!message.isBlank()) {
            tooltip.append("\n\n").append(message);
        }
        String remediation = remediationHint(throwable);
        if (remediation != null && !remediation.isBlank()) {
            tooltip.append("\n\nRemediation: ").append(remediation);
        }
        return html(tooltip.toString());
    }

    private String remediationHint(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof WmsException wmsException) {
                return wmsException.getRemediationHint();
            }
            current = current.getCause();
        }
        return null;
    }

    private String safeService(String oracleService) {
        return oracleService == null || oracleService.isBlank() ? "Oracle" : oracleService.trim();
    }

    private String html(String text) {
        return "<html>" + escape(text).replace("\n", "<br/>") + "</html>";
    }

    private String escape(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    record StatusState(
            Icon icon,
            String text,
            String tooltip
    ) {
    }
}
