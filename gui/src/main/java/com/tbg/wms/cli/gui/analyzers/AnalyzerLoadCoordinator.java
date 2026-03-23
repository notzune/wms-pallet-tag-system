package com.tbg.wms.cli.gui.analyzers;

public final class AnalyzerLoadCoordinator {

    private long currentToken;

    public synchronized long beginLoad() {
        currentToken++;
        return currentToken;
    }

    public synchronized boolean isCurrent(long token) {
        return currentToken == token;
    }
}
