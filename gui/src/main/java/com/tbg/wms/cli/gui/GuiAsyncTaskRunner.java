package com.tbg.wms.cli.gui;

import javax.swing.SwingWorker;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

final class GuiAsyncTaskRunner {

    <T> void run(Callable<T> task, TaskSuccess<T> onSuccess, TaskFailure onFailure) {
        Objects.requireNonNull(task, "task cannot be null");
        Objects.requireNonNull(onSuccess, "onSuccess cannot be null");
        Objects.requireNonNull(onFailure, "onFailure cannot be null");

        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (Exception ex) {
                    onFailure.accept(unwrap(ex));
                }
            }
        };
        worker.execute();
    }

    private static Exception unwrap(Exception ex) {
        if (ex instanceof ExecutionException executionException && executionException.getCause() instanceof Exception cause) {
            return cause;
        }
        return ex;
    }

    @FunctionalInterface
    interface TaskSuccess<T> {
        void accept(T value) throws Exception;
    }

    @FunctionalInterface
    interface TaskFailure {
        void accept(Exception ex);
    }
}
