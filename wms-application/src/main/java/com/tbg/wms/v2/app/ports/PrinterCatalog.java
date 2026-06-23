package com.tbg.wms.v2.app.ports;

import java.util.List;
import java.util.Optional;

/**
 * Lists and resolves enabled printers available to application workflows.
 */
public interface PrinterCatalog {
    List<PrinterRef> listEnabled();

    Optional<PrinterRef> findEnabledById(String printerId);
}
