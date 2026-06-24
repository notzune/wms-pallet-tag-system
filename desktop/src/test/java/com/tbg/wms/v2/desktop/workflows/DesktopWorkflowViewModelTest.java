package com.tbg.wms.v2.desktop.workflows;

import com.tbg.wms.v2.desktop.barcode.BarcodeViewModel;
import com.tbg.wms.v2.desktop.queue.QueuePrintViewModel;
import com.tbg.wms.v2.desktop.rail.RailLabelsViewModel;
import com.tbg.wms.v2.desktop.resume.ResumeViewModel;
import com.tbg.wms.v2.desktop.settings.SettingsViewModel;
import com.tbg.wms.v2.desktop.zplpreview.ZplPreviewViewModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DesktopWorkflowViewModelTest {
    @Test
    void queuePreviewAndPrintReportsParsedItemsAndExecutionCount() {
        QueuePrintViewModel viewModel = new QueuePrintViewModel(new FakeQueueWorkflow());

        viewModel.preview("S:800123\nC:456");
        viewModel.printToFile(Path.of("out", "queue"));

        assertEquals(2, viewModel.preview().items().size());
        assertEquals("Printed queue: 9 artifacts.", viewModel.status());
    }

    @Test
    void resumeLoadsCandidatesAndResumesSelection() {
        ResumeViewModel viewModel = new ResumeViewModel(new FakeResumeWorkflow());

        viewModel.refresh();
        viewModel.resume("job-1");

        assertEquals(1, viewModel.candidates().size());
        assertEquals("Resumed job-1 at task 5.", viewModel.status());
    }

    @Test
    void barcodeGeneratesDryRunArtifact() {
        BarcodeViewModel viewModel = new BarcodeViewModel(new FakeBarcodeWorkflow());

        viewModel.generateDryRun("HELLO-WORLD-123", Path.of("out", "barcode"));

        assertEquals("barcode-hello-world-123.zpl", viewModel.lastArtifact().getFileName().toString());
        assertEquals("Wrote barcode label barcode-hello-world-123.zpl.", viewModel.status());
    }

    @Test
    void railPreviewAndTemplateGenerationUseWorkflow() {
        RailLabelsViewModel viewModel = new RailLabelsViewModel(new FakeRailWorkflow());

        viewModel.previewTrain("tr-77");
        viewModel.generateTemplate(Path.of("out", "rail"));

        assertEquals("TR-77", viewModel.preview().trainId());
        assertEquals(2, viewModel.preview().railcarCount());
        assertEquals("Wrote rail alignment template.", viewModel.status());
    }

    @Test
    void zplPreviewRendersCurrentDocument() {
        ZplPreviewViewModel viewModel = new ZplPreviewViewModel(new FakeZplRenderer());

        viewModel.updateZpl("^XA^FDTEST^FS^XZ");
        viewModel.render();

        assertEquals("Rendered preview image.", viewModel.status());
        assertTrue(viewModel.lastPreviewDescription().contains("TEST"));
    }

    @Test
    void settingsExposeRuntimeSummaryAndSaveOperatorDefaults() {
        SettingsViewModel viewModel = new SettingsViewModel(new FakeSettingsStore());

        viewModel.load();
        viewModel.save(Path.of("out", "labels"), 21);

        assertEquals("TBG3002 / test", viewModel.runtimeSummary());
        assertEquals("Saved settings.", viewModel.status());
    }

    private static final class FakeQueueWorkflow implements QueuePrintViewModel.Workflow {
        @Override
        public QueuePrintViewModel.QueuePreview preview(String input) {
            return new QueuePrintViewModel.QueuePreview(List.of(
                    new QueuePrintViewModel.QueuePreviewItem("SHIPMENT", "800123"),
                    new QueuePrintViewModel.QueuePreviewItem("CARRIER_MOVE", "456")
            ));
        }

        @Override
        public QueuePrintViewModel.QueuePrintResult printToFile(
                QueuePrintViewModel.QueuePreview preview,
                Path outputDir
        ) {
            return new QueuePrintViewModel.QueuePrintResult(9, outputDir);
        }
    }

    private static final class FakeResumeWorkflow implements ResumeViewModel.Workflow {
        @Override
        public List<ResumeViewModel.ResumeCandidateItem> candidates() {
            return List.of(new ResumeViewModel.ResumeCandidateItem("job-1", "800123", 4, 6));
        }

        @Override
        public ResumeViewModel.ResumeResult resume(String checkpointId) {
            return new ResumeViewModel.ResumeResult(checkpointId, 5);
        }
    }

    private static final class FakeBarcodeWorkflow implements BarcodeViewModel.Workflow {
        @Override
        public Path generateDryRun(String data, Path outputDir) {
            return outputDir.resolve("barcode-hello-world-123.zpl");
        }
    }

    private static final class FakeRailWorkflow implements RailLabelsViewModel.Workflow {
        @Override
        public RailLabelsViewModel.RailPreview previewTrain(String trainId) {
            return new RailLabelsViewModel.RailPreview(trainId.trim().toUpperCase(), 2, List.of());
        }

        @Override
        public Path generateTemplate(Path outputDir) {
            return outputDir.resolve("rail-alignment-template.pdf");
        }
    }

    private static final class FakeZplRenderer implements ZplPreviewViewModel.Renderer {
        @Override
        public String render(String zpl) {
            return "preview for " + zpl;
        }
    }

    private static final class FakeSettingsStore implements SettingsViewModel.Store {
        @Override
        public SettingsViewModel.SettingsSnapshot load() {
            return new SettingsViewModel.SettingsSnapshot("TBG3002", "test", Path.of("out"), 14);
        }

        @Override
        public void save(Path defaultOutputDir, int retentionDays) {
        }
    }
}
