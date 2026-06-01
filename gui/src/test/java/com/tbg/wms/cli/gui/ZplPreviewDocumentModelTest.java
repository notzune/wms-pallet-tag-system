package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ZplPreviewDocumentModelTest {

    @Test
    void loadFiltersNullDocumentsAndStartsAtFirstDocument() {
        ZplPreviewDocumentModel model = new ZplPreviewDocumentModel();

        model.load(Arrays.asList(
                new GuiZplPreviewSupport.PreviewDocument("first.zpl", "^XA^XZ"),
                null,
                new GuiZplPreviewSupport.PreviewDocument("second.zpl", "^XA^FO10^XZ")
        ));

        assertEquals(2, model.size());
        assertEquals(0, model.currentIndex());
        assertEquals("first.zpl", model.currentDocument().name());
        assertEquals("first.zpl (1/2)", model.displayLabel());
        assertTrue(model.hasNext());
        assertFalse(model.hasPrevious());
    }

    @Test
    void showAtClampsRequestedIndexToAvailableDocuments() {
        ZplPreviewDocumentModel model = new ZplPreviewDocumentModel();
        model.load(List.of(
                new GuiZplPreviewSupport.PreviewDocument("first.zpl", "first"),
                new GuiZplPreviewSupport.PreviewDocument("second.zpl", "second")
        ));

        model.showAt(99);
        assertEquals(1, model.currentIndex());
        assertEquals("second.zpl", model.currentDocument().name());

        model.showAt(-10);
        assertEquals(0, model.currentIndex());
        assertEquals("first.zpl", model.currentDocument().name());
    }

    @Test
    void persistCurrentTextUpdatesOnlyCurrentDocument() {
        ZplPreviewDocumentModel model = new ZplPreviewDocumentModel();
        model.load(List.of(
                new GuiZplPreviewSupport.PreviewDocument("first.zpl", "first"),
                new GuiZplPreviewSupport.PreviewDocument("second.zpl", "second")
        ));

        model.persistCurrentText("edited-first");
        model.showAt(1);

        assertEquals("second", model.currentDocument().zpl());

        model.showAt(0);
        assertEquals("edited-first", model.currentDocument().zpl());
    }

    @Test
    void clearResetsToSingleDocumentState() {
        ZplPreviewDocumentModel model = new ZplPreviewDocumentModel();
        model.load(List.of(new GuiZplPreviewSupport.PreviewDocument("first.zpl", "first")));

        model.clear();

        assertTrue(model.isEmpty());
        assertEquals(0, model.currentIndex());
        assertEquals(1, model.spinnerMaximum());
        assertEquals("Single document", model.displayLabel());
        assertFalse(model.hasNext());
        assertFalse(model.hasPrevious());
    }
}
