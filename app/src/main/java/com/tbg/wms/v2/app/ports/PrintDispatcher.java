package com.tbg.wms.v2.app.ports;

import com.tbg.wms.v2.domain.print.PrintTask;

/**
 * Dispatches a generated print task to a live printer.
 */
public interface PrintDispatcher {
    /**
     * Dispatches one print task to the selected printer.
     *
     * @param printer the printer.
     * @param task the task.
     */
    void dispatch(PrinterRef printer, PrintTask task);
}
