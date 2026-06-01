package com.tbg.wms.cli.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Owns the editable document list and navigation state for the ZPL preview dialog.
 */
final class ZplPreviewDocumentModel {
    private final List<GuiZplPreviewSupport.PreviewDocument> documents = new ArrayList<>();
    private int currentIndex;

    void load(List<GuiZplPreviewSupport.PreviewDocument> previewDocuments) {
        documents.clear();
        if (previewDocuments != null) {
            documents.addAll(previewDocuments.stream()
                    .filter(Objects::nonNull)
                    .toList());
        }
        currentIndex = 0;
    }

    void clear() {
        documents.clear();
        currentIndex = 0;
    }

    void showAt(int index) {
        if (documents.isEmpty()) {
            currentIndex = 0;
            return;
        }
        currentIndex = Math.max(0, Math.min(index, documents.size() - 1));
    }

    void persistCurrentText(String zpl) {
        if (documents.isEmpty() || currentIndex < 0 || currentIndex >= documents.size()) {
            return;
        }
        GuiZplPreviewSupport.PreviewDocument document = documents.get(currentIndex);
        documents.set(currentIndex, new GuiZplPreviewSupport.PreviewDocument(document.name(), zpl));
    }

    GuiZplPreviewSupport.PreviewDocument currentDocument() {
        if (documents.isEmpty()) {
            return new GuiZplPreviewSupport.PreviewDocument("preview.zpl", "");
        }
        return documents.get(currentIndex);
    }

    int currentIndex() {
        return currentIndex;
    }

    int size() {
        return documents.size();
    }

    boolean isEmpty() {
        return documents.isEmpty();
    }

    boolean hasPrevious() {
        return documents.size() > 1 && currentIndex > 0;
    }

    boolean hasNext() {
        return documents.size() > 1 && currentIndex < documents.size() - 1;
    }

    int spinnerMaximum() {
        return Math.max(1, documents.size());
    }

    String displayLabel() {
        if (documents.isEmpty()) {
            return "Single document";
        }
        return currentDocument().name() + " (" + (currentIndex + 1) + "/" + documents.size() + ")";
    }
}
