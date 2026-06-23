package com.tbg.wms.v2.app.ports;

import com.tbg.wms.v2.domain.print.PrintTask;

/**
 * Dispatches a generated print task to a live printer.
 */
public interface PrintDispatcher {
    void dispatch(PrinterRef printer, PrintTask task);
}
