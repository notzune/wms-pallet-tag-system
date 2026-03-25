package com.tbg.wms.cli.gui.analyzers;

final class AnalyzerLoadSessionState<R> {

    private AnalyzerLoadSnapshot<R> lastSuccessfulSnapshot;
    private long latestRequestId;
    private boolean loading;

    long beginLoad(long requestId) {
        loading = true;
        latestRequestId = requestId;
        return latestRequestId;
    }

    boolean isLatestRequest(long requestId) {
        return latestRequestId == requestId;
    }

    void recordSuccess(long requestId, AnalyzerLoadSnapshot<R> snapshot) {
        if (!isLatestRequest(requestId)) {
            return;
        }
        lastSuccessfulSnapshot = snapshot;
        loading = false;
    }

    void recordFailure(long requestId) {
        if (isLatestRequest(requestId)) {
            loading = false;
        }
    }

    AnalyzerLoadSnapshot<R> lastSuccessfulSnapshot() {
        return lastSuccessfulSnapshot;
    }

    boolean hasSnapshot() {
        return lastSuccessfulSnapshot != null;
    }

    boolean loading() {
        return loading;
    }
}
