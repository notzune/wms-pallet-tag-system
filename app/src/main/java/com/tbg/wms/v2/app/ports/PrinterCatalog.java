package com.tbg.wms.v2.app.ports;

import java.util.List;
import java.util.Optional;

/**
 * Defines the contract for printer catalog behavior in WMS 2.0 workflows.
 */
public interface PrinterCatalog {
    /**
     * Lists enabled printers.
     *
     * @return the matching values.
     */
    List<PrinterRef> listEnabled();

    /**
     * Finds an enabled printer by identifier.
     *
     * @param printerId the printer id.
     * @return the matching value, when present.
     */
    Optional<PrinterRef> findEnabledById(String printerId);
}
