/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.3.0
 */

package com.tbg.wms.cli.gui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.CarrierMoveStopRef;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.db.DbConnectionPool;
import com.tbg.wms.db.DbQueryRepository;
import com.tbg.wms.db.OracleDbQueryRepository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Higher-level workflow for carrier move jobs, queueing, and checkpoint resume.
 */
public final class AdvancedPrintWorkflowService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int MAX_QUEUE_ITEMS = 500;
    private static final int MAX_TASKS_PER_JOB = 50_000;
    private static final int MAX_CHECKPOINT_FILES_SCANNED = 5_000;
    private final AppConfig config;
    private final LabelWorkflowService shipmentService;
    private final PrintCheckpointSupport checkpointSupport;
    private final CarrierMovePreparationSupport carrierMovePreparationSupport = new CarrierMovePreparationSupport();
    private final AdvancedPrintResultSupport resultSupport = new AdvancedPrintResultSupport();

    public AdvancedPrintWorkflowService(AppConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.shipmentService = new LabelWorkflowService(config);
        this.checkpointSupport = new PrintCheckpointSupport(
                new JobCheckpointStore(),
                shipmentService,
                MAX_TASKS_PER_JOB,
                MAX_CHECKPOINT_FILES_SCANNED
        );
    }

    public LabelWorkflowService.PreparedJob prepareShipmentJob(String shipmentId) throws Exception {
        return shipmentService.prepareJob(shipmentId);
    }

    public void clearCaches() {
        shipmentService.clearCaches();
    }

    public PreparedCarrierMoveJob prepareCarrierMoveJob(String carrierMoveId) throws Exception {
        if (carrierMoveId == null || carrierMoveId.isBlank()) {
            throw new IllegalArgumentException("Carrier Move ID is required.");
        }
        String cmid = carrierMoveId.trim();

        try (DbConnectionPool pool = new DbConnectionPool(config)) {
            DbQueryRepository repo = new OracleDbQueryRepository(pool.getDataSource());
            List<CarrierMoveStopRef> refs = repo.findCarrierMoveStops(cmid);
            if (refs.isEmpty()) {
                throw new IllegalArgumentException("Carrier Move not found or has no shipments: " + cmid);
            }
            List<PreparedStopGroup> groups = buildPreparedStopGroups(repo, refs);

            if (groups.isEmpty()) {
                throw new IllegalArgumentException("Carrier Move has no printable shipments: " + cmid);
            }

            return new PreparedCarrierMoveJob(cmid, groups);
        }
    }

    private List<PreparedStopGroup> buildPreparedStopGroups(DbQueryRepository repo, List<CarrierMoveStopRef> refs) throws Exception {
        List<CarrierMovePreparationSupport.StopShipmentPlan> plans = carrierMovePreparationSupport.buildStopShipmentPlans(refs);
        List<PreparedStopGroup> groups = new ArrayList<>(plans.size());
        int stopPosition = 1;
        for (CarrierMovePreparationSupport.StopShipmentPlan plan : plans) {
            List<LabelWorkflowService.PreparedJob> jobs = resolvePreparedJobsForStop(repo, plan.shipmentIds());
            if (!jobs.isEmpty()) {
                groups.add(new PreparedStopGroup(plan.stopSequence(), stopPosition, jobs));
                stopPosition++;
            }
        }
        return groups;
    }

    private List<LabelWorkflowService.PreparedJob> resolvePreparedJobsForStop(DbQueryRepository repo, List<String> shipmentIds) throws Exception {
        List<LabelWorkflowService.PreparedJob> jobs = new ArrayList<>(shipmentIds.size());
        for (String shipId : shipmentIds) {
            jobs.add(shipmentService.prepareJob(repo, shipId));
        }
        return jobs;
    }

    public PreparedQueueJob prepareQueue(List<QueueRequestItem> requests) throws Exception {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Queue is empty.");
        }
        if (requests.size() > MAX_QUEUE_ITEMS) {
            throw new IllegalArgumentException("Queue exceeds max size of " + MAX_QUEUE_ITEMS + " items.");
        }
        List<PreparedQueueItem> resolved = new ArrayList<>();
        for (QueueRequestItem req : requests) {
            if (req == null || req.id.isBlank()) {
                continue;
            }
            if (req.type == QueueItemType.CARRIER_MOVE) {
                resolved.add(PreparedQueueItem.forCarrier(req.id, prepareCarrierMoveJob(req.id)));
            } else {
                resolved.add(PreparedQueueItem.forShipment(req.id, prepareShipmentJob(req.id)));
            }
        }
        if (resolved.isEmpty()) {
            throw new IllegalArgumentException("Queue is empty after parsing.");
        }
        return new PreparedQueueJob(resolved);
    }

    public QueuePrintResult printQueue(PreparedQueueJob queue, String printerId, boolean printToFile) throws Exception {
        List<PrintResult> results = new ArrayList<>();
        int labels = 0;
        int infoTags = 0;
        for (PreparedQueueItem item : queue.items) {
            PrintResult result = item.type == QueueItemType.CARRIER_MOVE
                    ? printCarrierMoveJob(item.carrierMoveJob, printerId, null, printToFile)
                    : printShipmentJob(item.shipmentJob, printerId, null, printToFile);
            results.add(result);
            labels += result.labelsPrinted;
            infoTags += result.infoTagsPrinted;
        }
        return new QueuePrintResult(results, labels, infoTags);
    }

    public PrintResult printShipmentJob(LabelWorkflowService.PreparedJob job, String printerId, Path outputDir, boolean printToFile) throws Exception {
        return printShipmentJob(job, job.getLpnsForLabels(), printerId, outputDir, printToFile, true);
    }

    public PrintResult printShipmentJob(LabelWorkflowService.PreparedJob job, List<Lpn> selectedLpns, String printerId, Path outputDir, boolean printToFile) throws Exception {
        return printShipmentJob(job, selectedLpns, printerId, outputDir, printToFile, true);
    }

    public PrintResult printShipmentJob(
            LabelWorkflowService.PreparedJob job,
            List<Lpn> selectedLpns,
            String printerId,
            Path outputDir,
            boolean printToFile,
            boolean includeInfoTags
    ) throws Exception {
        Objects.requireNonNull(job, "job cannot be null");
        PrinterConfig printer = resultSupport.resolvePrinterForPrint(job.getRouting(), printerId, printToFile);
        Path targetDir = outputDir == null
                ? Paths.get("out", "gui-" + job.getShipmentId() + "-" + TS.format(LocalDateTime.now()))
                : outputDir;
        List<Lpn> lpnsToPrint = PrintTaskPlanner.filterLpnsForPrint(job.getLpnsForLabels(), selectedLpns);
        PrintTaskPlanner.ShipmentPrintBatch shipmentBatch =
                PrintTaskPlanner.ShipmentPrintBatch.forShipment(job, lpnsToPrint, includeInfoTags);
        List<PrintTask> tasks = PrintTaskPlanner.buildShipmentTasks(shipmentBatch);
        JobCheckpoint checkpoint = checkpointSupport.createCheckpoint("shipment-" + job.getShipmentId() + "-" + TS.format(LocalDateTime.now()),
                InputMode.SHIPMENT, job.getShipmentId(), targetDir, printToFile, printer, tasks);
        checkpointSupport.executeTasks(checkpoint, printer, 0);
        return resultSupport.toResult(checkpoint);
    }

    public PrintResult printCarrierMoveJob(PreparedCarrierMoveJob job, String printerId, Path outputDir, boolean printToFile) throws Exception {
        return printCarrierMoveJob(job, PrintTaskPlanner.collectAllCarrierMoveLabelSelections(job), printerId, outputDir, printToFile, true);
    }

    public PrintResult printCarrierMoveJob(
            PreparedCarrierMoveJob job,
            List<LabelSelectionRef> selectedLabels,
            String printerId,
            Path outputDir,
            boolean printToFile
    ) throws Exception {
        return printCarrierMoveJob(job, selectedLabels, printerId, outputDir, printToFile, true);
    }

    public PrintResult printCarrierMoveJob(
            PreparedCarrierMoveJob job,
            List<LabelSelectionRef> selectedLabels,
            String printerId,
            Path outputDir,
            boolean printToFile,
            boolean includeInfoTags
    ) throws Exception {
        Objects.requireNonNull(job, "job cannot be null");
        LabelWorkflowService.PreparedJob firstShipment = job.firstShipmentJob();
        PrinterConfig printer = resultSupport.resolvePrinterForPrint(firstShipment.getRouting(), printerId, printToFile);
        Path targetDir = outputDir == null
                ? Paths.get("out", "gui-cmid-" + job.carrierMoveId + "-" + TS.format(LocalDateTime.now()))
                : outputDir;
        List<PrintTask> tasks = PrintTaskPlanner.buildCarrierMoveTasks(job, selectedLabels, includeInfoTags);
        JobCheckpoint checkpoint = checkpointSupport.createCheckpoint("carrier-" + job.carrierMoveId + "-" + TS.format(LocalDateTime.now()),
                InputMode.CARRIER_MOVE, job.carrierMoveId, targetDir, printToFile, printer, tasks);
        checkpointSupport.executeTasks(checkpoint, printer, 0);
        return resultSupport.toResult(checkpoint);
    }

    public List<ResumeCandidate> listIncompleteJobs() throws Exception {
        return checkpointSupport.listIncompleteJobs();
    }

    /**
     * Resume from last successful task (safe mode): reprint the most recent completed task, then continue.
     */
    public PrintResult resumeJob(String checkpointId) throws Exception {
        return resultSupport.toResult(checkpointSupport.resumeJob(checkpointId));
    }

    public enum InputMode {
        CARRIER_MOVE,
        SHIPMENT
    }

    public enum QueueItemType {
        CARRIER_MOVE,
        SHIPMENT
    }

    public enum TaskKind {
        PALLET_LABEL,
        STOP_INFO_TAG,
        FINAL_INFO_TAG
    }

    public static final class PreparedCarrierMoveJob {
        private final String carrierMoveId;
        private final List<PreparedStopGroup> stopGroups;

        private PreparedCarrierMoveJob(String carrierMoveId, List<PreparedStopGroup> stopGroups) {
            this.carrierMoveId = carrierMoveId;
            this.stopGroups = List.copyOf(stopGroups);
        }

        public String getCarrierMoveId() {
            return carrierMoveId;
        }

        public List<PreparedStopGroup> getStopGroups() {
            return stopGroups;
        }

        public int getTotalStops() {
            return stopGroups.size();
        }

        public LabelWorkflowService.PreparedJob firstShipmentJob() {
            for (PreparedStopGroup stop : stopGroups) {
                for (LabelWorkflowService.PreparedJob shipmentJob : stop.shipmentJobs) {
                    if (shipmentJob != null) {
                        return shipmentJob;
                    }
                }
            }
            throw new IllegalArgumentException("Carrier move has no printable shipments.");
        }
    }

    public static final class PreparedStopGroup {
        private final Integer stopSequence;
        private final int stopPosition;
        private final List<LabelWorkflowService.PreparedJob> shipmentJobs;

        private PreparedStopGroup(Integer stopSequence, int stopPosition, List<LabelWorkflowService.PreparedJob> shipmentJobs) {
            this.stopSequence = stopSequence;
            this.stopPosition = stopPosition;
            this.shipmentJobs = List.copyOf(shipmentJobs);
        }

        public Integer getStopSequence() {
            return stopSequence;
        }

        public int getStopPosition() {
            return stopPosition;
        }

        public List<LabelWorkflowService.PreparedJob> getShipmentJobs() {
            return shipmentJobs;
        }
    }

    public static final class QueueRequestItem {
        private final QueueItemType type;
        private final String id;

        public QueueRequestItem(QueueItemType type, String id) {
            this.type = Objects.requireNonNull(type, "type");
            this.id = Objects.requireNonNull(id, "id").trim();
        }

        public QueueItemType getType() {
            return type;
        }

        public String getId() {
            return id;
        }
    }

    public static final class PreparedQueueItem {
        private final QueueItemType type;
        private final String sourceId;
        private final LabelWorkflowService.PreparedJob shipmentJob;
        private final PreparedCarrierMoveJob carrierMoveJob;

        private PreparedQueueItem(QueueItemType type, String sourceId, LabelWorkflowService.PreparedJob shipmentJob, PreparedCarrierMoveJob carrierMoveJob) {
            this.type = type;
            this.sourceId = sourceId;
            this.shipmentJob = shipmentJob;
            this.carrierMoveJob = carrierMoveJob;
        }

        private static PreparedQueueItem forShipment(String id, LabelWorkflowService.PreparedJob job) {
            return new PreparedQueueItem(QueueItemType.SHIPMENT, id, job, null);
        }

        private static PreparedQueueItem forCarrier(String id, PreparedCarrierMoveJob job) {
            return new PreparedQueueItem(QueueItemType.CARRIER_MOVE, id, null, job);
        }

        public QueueItemType getType() {
            return type;
        }

        public String getSourceId() {
            return sourceId;
        }

        public LabelWorkflowService.PreparedJob getShipmentJob() {
            return shipmentJob;
        }

        public PreparedCarrierMoveJob getCarrierMoveJob() {
            return carrierMoveJob;
        }
    }

    public static final class PreparedQueueJob {
        private final List<PreparedQueueItem> items;

        private PreparedQueueJob(List<PreparedQueueItem> items) {
            this.items = List.copyOf(items);
        }

        public List<PreparedQueueItem> getItems() {
            return items;
        }
    }

    public static final class PrintResult {
        private final int labelsPrinted;
        private final int infoTagsPrinted;
        private final Path outputDirectory;
        private final String printerId;
        private final String printerEndpoint;
        private final boolean printToFile;

        PrintResult(int labelsPrinted, int infoTagsPrinted, Path outputDirectory, String printerId, String printerEndpoint, boolean printToFile) {
            this.labelsPrinted = labelsPrinted;
            this.infoTagsPrinted = infoTagsPrinted;
            this.outputDirectory = outputDirectory;
            this.printerId = printerId;
            this.printerEndpoint = printerEndpoint;
            this.printToFile = printToFile;
        }

        public int getLabelsPrinted() {
            return labelsPrinted;
        }

        public int getInfoTagsPrinted() {
            return infoTagsPrinted;
        }

        public Path getOutputDirectory() {
            return outputDirectory;
        }

        public String getPrinterId() {
            return printerId;
        }

        public String getPrinterEndpoint() {
            return printerEndpoint;
        }

        public boolean isPrintToFile() {
            return printToFile;
        }
    }

    public static final class QueuePrintResult {
        private final List<PrintResult> itemResults;
        private final int totalLabelsPrinted;
        private final int totalInfoTagsPrinted;

        private QueuePrintResult(List<PrintResult> itemResults, int totalLabelsPrinted, int totalInfoTagsPrinted) {
            this.itemResults = List.copyOf(itemResults);
            this.totalLabelsPrinted = totalLabelsPrinted;
            this.totalInfoTagsPrinted = totalInfoTagsPrinted;
        }

        public List<PrintResult> getItemResults() {
            return itemResults;
        }

        public int getTotalLabelsPrinted() {
            return totalLabelsPrinted;
        }

        public int getTotalInfoTagsPrinted() {
            return totalInfoTagsPrinted;
        }
    }

    public static final class ResumeCandidate {
        private final String checkpointId;
        private final InputMode mode;
        private final String sourceId;
        private final String outputDirectory;
        private final int nextTaskIndex;
        private final int totalTasks;
        private final LocalDateTime updatedAt;
        private final String lastError;

        ResumeCandidate(String checkpointId, InputMode mode, String sourceId, String outputDirectory, int nextTaskIndex, int totalTasks, LocalDateTime updatedAt, String lastError) {
            this.checkpointId = checkpointId;
            this.mode = mode;
            this.sourceId = sourceId;
            this.outputDirectory = outputDirectory;
            this.nextTaskIndex = nextTaskIndex;
            this.totalTasks = totalTasks;
            this.updatedAt = updatedAt;
            this.lastError = lastError;
        }

        public String checkpointId() {
            return checkpointId;
        }

        public InputMode mode() {
            return mode;
        }

        public String sourceId() {
            return sourceId;
        }

        public String outputDirectory() {
            return outputDirectory;
        }

        public int nextTaskIndex() {
            return nextTaskIndex;
        }

        public int totalTasks() {
            return totalTasks;
        }

        public LocalDateTime updatedAt() {
            return updatedAt;
        }

        public String lastError() {
            return lastError;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class JobCheckpoint {
        public String id;
        public InputMode mode;
        public String sourceId;
        public String outputDirectory;
        public boolean printToFile;
        public String printerId;
        public String printerEndpoint;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
        public boolean completed;
        public int nextTaskIndex;
        public List<PrintTask> tasks = List.of();
        public String lastError;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class PrintTask {
        public TaskKind kind;
        public String fileName;
        public String zpl;
        public String payloadId;

        public PrintTask() {
        }

        PrintTask(TaskKind kind, String fileName, String zpl, String payloadId) {
            this.kind = kind;
            this.fileName = fileName;
            this.zpl = zpl;
            this.payloadId = payloadId;
        }
    }
}
