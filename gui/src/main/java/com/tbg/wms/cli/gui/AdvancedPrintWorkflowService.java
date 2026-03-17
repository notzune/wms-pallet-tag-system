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
import com.tbg.wms.core.label.LabelDataBuilder;
import com.tbg.wms.core.label.LabelType;
import com.tbg.wms.core.model.CarrierMoveStopRef;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.print.NetworkPrintService;
import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.template.ZplTemplateEngine;
import com.tbg.wms.db.DbConnectionPool;
import com.tbg.wms.db.DbQueryRepository;
import com.tbg.wms.db.OracleDbQueryRepository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Higher-level workflow for carrier move jobs, queueing, and checkpoint resume.
 */
public final class AdvancedPrintWorkflowService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int MAX_QUEUE_ITEMS = 500;
    private static final int MAX_LABELS_PER_JOB = 10_000;
    private static final int MAX_TASKS_PER_JOB = 50_000;
    private static final int MAX_CHECKPOINT_FILES_SCANNED = 5_000;
    private static final Pattern NON_ALNUM_PATTERN = Pattern.compile("[^a-z0-9]+");
    private final AppConfig config;
    private final LabelWorkflowService shipmentService;
    private final JobCheckpointStore checkpointStore;

    public AdvancedPrintWorkflowService(AppConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.shipmentService = new LabelWorkflowService(config);
        this.checkpointStore = new JobCheckpointStore();
    }

    public LabelWorkflowService.PreparedJob prepareShipmentJob(String shipmentId) throws Exception {
        return shipmentService.prepareJob(shipmentId);
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
        Map<Integer, List<CarrierMoveStopRef>> byStop = groupCarrierMoveRefsByStop(refs);
        List<PreparedStopGroup> groups = new ArrayList<>(byStop.size());
        int stopPosition = 1;
        for (List<CarrierMoveStopRef> stopRefs : byStop.values()) {
            if (stopRefs == null || stopRefs.isEmpty()) {
                continue;
            }
            List<LabelWorkflowService.PreparedJob> jobs = resolvePreparedJobsForStop(repo, stopRefs);
            if (!jobs.isEmpty()) {
                Integer stopSequence = null;
                for (CarrierMoveStopRef stopRef : stopRefs) {
                    if (stopRef != null) {
                        stopSequence = stopRef.getStopSequence();
                        break;
                    }
                }
                groups.add(new PreparedStopGroup(stopSequence, stopPosition, jobs));
                stopPosition++;
            }
        }
        return groups;
    }

    private Map<Integer, List<CarrierMoveStopRef>> groupCarrierMoveRefsByStop(List<CarrierMoveStopRef> refs) {
        Map<Integer, List<CarrierMoveStopRef>> byStop = new TreeMap<>();
        for (CarrierMoveStopRef ref : refs) {
            int key = ref.getStopSequence() == null ? Integer.MAX_VALUE : ref.getStopSequence();
            byStop.computeIfAbsent(key, ignored -> new ArrayList<>()).add(ref);
        }
        return byStop;
    }

    private List<LabelWorkflowService.PreparedJob> resolvePreparedJobsForStop(DbQueryRepository repo, List<CarrierMoveStopRef> stopRefs) throws Exception {
        List<CarrierMoveStopRef> orderedRefs = new ArrayList<>(stopRefs.size());
        for (CarrierMoveStopRef stopRef : stopRefs) {
            if (stopRef != null) {
                orderedRefs.add(stopRef);
            }
        }
        orderedRefs.sort(Comparator.comparing(
                CarrierMoveStopRef::getShipmentId,
                Comparator.nullsLast(String::compareTo)
        ));

        LinkedHashSet<String> uniqueShipments = new LinkedHashSet<>(orderedRefs.size());
        for (CarrierMoveStopRef ref : orderedRefs) {
            if (ref.getShipmentId() != null && !ref.getShipmentId().isBlank()) {
                uniqueShipments.add(ref.getShipmentId());
            }
        }

        List<LabelWorkflowService.PreparedJob> jobs = new ArrayList<>(uniqueShipments.size());
        for (String shipId : uniqueShipments) {
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
        return printShipmentJob(job, job.getLpnsForLabels(), printerId, outputDir, printToFile);
    }

    public PrintResult printShipmentJob(LabelWorkflowService.PreparedJob job, List<Lpn> selectedLpns, String printerId, Path outputDir, boolean printToFile) throws Exception {
        Objects.requireNonNull(job, "job cannot be null");
        PrinterConfig printer = resolvePrinterForPrint(job.getRouting(), printerId, printToFile);
        Path targetDir = outputDir == null
                ? Paths.get("out", "gui-" + job.getShipmentId() + "-" + TS.format(LocalDateTime.now()))
                : outputDir;
        List<Lpn> lpnsToPrint = filterLpnsForPrint(job.getLpnsForLabels(), selectedLpns);
        List<PrintTask> tasks = buildShipmentTasks(job, lpnsToPrint, null, null, true);
        JobCheckpoint checkpoint = createCheckpoint("shipment-" + job.getShipmentId() + "-" + TS.format(LocalDateTime.now()),
                InputMode.SHIPMENT, job.getShipmentId(), targetDir, printToFile, printer, tasks);
        executeTasks(checkpoint, printer, 0);
        return toResult(checkpoint);
    }

    public PrintResult printCarrierMoveJob(PreparedCarrierMoveJob job, String printerId, Path outputDir, boolean printToFile) throws Exception {
        Objects.requireNonNull(job, "job cannot be null");
        LabelWorkflowService.PreparedJob firstShipment = firstCarrierShipment(job);
        PrinterConfig printer = resolvePrinterForPrint(firstShipment.getRouting(), printerId, printToFile);
        Path targetDir = outputDir == null
                ? Paths.get("out", "gui-cmid-" + job.carrierMoveId + "-" + TS.format(LocalDateTime.now()))
                : outputDir;
        List<PrintTask> tasks = buildCarrierMoveTasks(job);
        JobCheckpoint checkpoint = createCheckpoint("carrier-" + job.carrierMoveId + "-" + TS.format(LocalDateTime.now()),
                InputMode.CARRIER_MOVE, job.carrierMoveId, targetDir, printToFile, printer, tasks);
        executeTasks(checkpoint, printer, 0);
        return toResult(checkpoint);
    }

    public List<ResumeCandidate> listIncompleteJobs() throws Exception {
        List<ResumeCandidate> items = new ArrayList<>();
        for (Path file : checkpointStore.listCheckpointFiles(MAX_CHECKPOINT_FILES_SCANNED)) {
            try {
                JobCheckpoint checkpoint = checkpointStore.read(stripJsonExtension(file.getFileName().toString()));
                if (checkpoint != null && !checkpoint.completed) {
                    int total = checkpoint.tasks == null ? 0 : checkpoint.tasks.size();
                    items.add(new ResumeCandidate(checkpoint.id, checkpoint.mode, checkpoint.sourceId, checkpoint.outputDirectory,
                            checkpoint.nextTaskIndex, total, checkpoint.updatedAt, checkpoint.lastError));
                }
            } catch (Exception ignored) {
                // Skip malformed checkpoint files and continue scanning.
            }
        }
        items.sort(Comparator.comparing(ResumeCandidate::updatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed());
        return items;
    }

    /**
     * Resume from last successful task (safe mode): reprint the most recent completed task, then continue.
     */
    public PrintResult resumeJob(String checkpointId) throws Exception {
        if (checkpointId == null || checkpointId.isBlank()) {
            throw new IllegalArgumentException("Checkpoint ID is required.");
        }
        JobCheckpoint checkpoint = readCheckpoint(checkpointId.trim());
        if (checkpoint == null) {
            throw new IllegalArgumentException("Checkpoint not found: " + checkpointId);
        }
        if (checkpoint.completed) {
            throw new IllegalStateException("Checkpoint is already completed.");
        }
        PrinterConfig printer = checkpoint.printToFile ? null : shipmentService.resolvePrinter(checkpoint.printerId);
        int resumeIndex = checkpoint.nextTaskIndex <= 0 ? 0 : checkpoint.nextTaskIndex - 1;
        executeTasks(checkpoint, printer, resumeIndex);
        return toResult(checkpoint);
    }

    private PrinterConfig resolvePrinterForPrint(com.tbg.wms.core.print.PrinterRoutingService routing, String printerId, boolean printToFile) {
        if (printToFile) {
            return null;
        }
        String id = printerId == null ? "" : printerId.trim();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Printer is required.");
        }
        return routing.findPrinter(id)
                .orElseThrow(() -> new IllegalArgumentException("Printer not found or disabled: " + id));
    }

    static List<Lpn> filterLpnsForPrint(List<Lpn> availableLpns, List<Lpn> selectedLpns) {
        Objects.requireNonNull(availableLpns, "availableLpns cannot be null");
        Objects.requireNonNull(selectedLpns, "selectedLpns cannot be null");

        if (selectedLpns.isEmpty()) {
            throw new IllegalArgumentException("Select at least one label to print.");
        }

        Set<String> selectedIds = new LinkedHashSet<>();
        for (Lpn lpn : selectedLpns) {
            if (lpn != null && lpn.getLpnId() != null) {
                selectedIds.add(lpn.getLpnId());
            }
        }
        if (selectedIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one label to print.");
        }

        List<Lpn> filtered = new ArrayList<>(selectedIds.size());
        for (Lpn lpn : availableLpns) {
            if (lpn != null && selectedIds.contains(lpn.getLpnId())) {
                filtered.add(lpn);
            }
        }
        if (filtered.isEmpty()) {
            throw new IllegalArgumentException("Selected labels are no longer available for printing.");
        }
        return filtered;
    }

    private List<PrintTask> buildShipmentTasks(
            LabelWorkflowService.PreparedJob job,
            List<Lpn> lpnsToPrint,
            Integer stopSequence,
            Integer stopPosition,
            boolean includeShipmentInfoTag
    ) {
        Objects.requireNonNull(job, "job cannot be null");
        Objects.requireNonNull(lpnsToPrint, "lpnsToPrint cannot be null");
        LabelDataBuilder builder = new LabelDataBuilder(job.getSkuMapping(), job.getSiteConfig(), job.getFootprintBySku());
        List<PrintTask> tasks = new ArrayList<>();
        Shipment shipmentForLabels = buildShipmentForLabeling(job.getShipment(), lpnsToPrint);
        int labelCount = lpnsToPrint.size();
        if (labelCount > MAX_LABELS_PER_JOB) {
            throw new IllegalArgumentException("Label count exceeds max limit: " + MAX_LABELS_PER_JOB);
        }
        for (int i = 0; i < labelCount; i++) {
            Lpn lpn = lpnsToPrint.get(i);
            Map<String, String> data = new LinkedHashMap<>(builder.build(shipmentForLabels, lpn, i, LabelType.WALMART_CANADA_GRID));
            if (stopSequence != null) {
                data.put("stopSequence", String.valueOf(stopSequence));
            }
            if (job.isUsingVirtualLabels()) {
                data.put("palletSeq", String.valueOf(i + 1));
                data.put("palletTotal", String.valueOf(labelCount));
            }
            String zpl = ZplTemplateEngine.generate(job.getTemplate(), data);
            String fileName = String.format("%s_%s_%d_of_%d.zpl",
                    job.getShipmentId(), lpn.getLpnId(), i + 1, labelCount);
            String payload = job.getShipmentId() + ":" + lpn.getLpnId() + (stopPosition == null ? "" : (" stop " + stopPosition));
            tasks.add(new PrintTask(TaskKind.PALLET_LABEL, fileName, zpl, payload));
        }

        if (includeShipmentInfoTag) {
            String infoFile = "info-shipment-" + safeSlug(job.getShipmentId()) + ".zpl";
            String infoZpl = InfoTagZplBuilder.buildShipmentInfoTag(job);
            tasks.add(new PrintTask(TaskKind.STOP_INFO_TAG, infoFile, infoZpl, "INFO-SHIPMENT " + job.getShipmentId()));
        }
        return tasks;
    }

    private Shipment buildShipmentForLabeling(Shipment shipment, List<Lpn> lpnsForLabels) {
        if (shipment == null) {
            throw new IllegalArgumentException("shipment cannot be null");
        }
        if (lpnsForLabels == null) {
            throw new IllegalArgumentException("lpnsForLabels cannot be null");
        }
        if (shipment.getLpnCount() == lpnsForLabels.size()) {
            return shipment;
        }
        return new Shipment(
                shipment.getShipmentId(),
                shipment.getExternalId(),
                shipment.getOrderId(),
                shipment.getWarehouseId(),
                shipment.getShipToName(),
                shipment.getShipToAddress1(),
                shipment.getShipToAddress2(),
                shipment.getShipToAddress3(),
                shipment.getShipToCity(),
                shipment.getShipToState(),
                shipment.getShipToZip(),
                shipment.getShipToCountry(),
                shipment.getShipToPhone(),
                shipment.getCarrierCode(),
                shipment.getServiceLevel(),
                shipment.getDocumentNumber(),
                shipment.getTrackingNumber(),
                shipment.getDestinationLocation(),
                shipment.getCustomerPo(),
                shipment.getLocationNumber(),
                shipment.getDepartmentNumber(),
                shipment.getStopId(),
                shipment.getStopSequence(),
                shipment.getCarrierMoveId(),
                shipment.getProNumber(),
                shipment.getBolNumber(),
                shipment.getStatus(),
                shipment.getShipDate(),
                shipment.getDeliveryDate(),
                shipment.getCreatedDate(),
                lpnsForLabels
        );
    }

    private LabelWorkflowService.PreparedJob firstCarrierShipment(PreparedCarrierMoveJob job) {
        for (PreparedStopGroup stop : job.stopGroups) {
            for (LabelWorkflowService.PreparedJob shipmentJob : stop.shipmentJobs) {
                if (shipmentJob != null) {
                    return shipmentJob;
                }
            }
        }
        throw new IllegalArgumentException("Carrier move has no printable shipments.");
    }

    private List<PrintTask> buildCarrierMoveTasks(PreparedCarrierMoveJob job) {
        int totalShipmentJobs = 0;
        for (PreparedStopGroup stop : job.stopGroups) {
            totalShipmentJobs += stop.shipmentJobs.size();
        }
        // One label-task block per shipment plus one stop info tag per stop and one final info tag.
        List<PrintTask> tasks = new ArrayList<>(totalShipmentJobs + job.stopGroups.size() + 1);
        int totalStops = job.stopGroups.size();
        for (PreparedStopGroup stop : job.stopGroups) {
            List<String> shipmentIds = new ArrayList<>(stop.shipmentJobs.size());
            for (LabelWorkflowService.PreparedJob shipmentJob : stop.shipmentJobs) {
                shipmentIds.add(shipmentJob.getShipmentId());
                tasks.addAll(buildShipmentTasks(shipmentJob, shipmentJob.getLpnsForLabels(), stop.stopSequence, stop.stopPosition, false));
            }

            String stopInfoFile = String.format("info-stop-%02d-of-%02d.zpl", stop.stopPosition, totalStops);
            String stopInfo = InfoTagZplBuilder.buildStopInfoTag(
                    job.carrierMoveId,
                    stop.stopPosition,
                    totalStops,
                    stop.stopSequence,
                    shipmentIds,
                    stop.shipmentJobs
            );
            tasks.add(new PrintTask(TaskKind.STOP_INFO_TAG, stopInfoFile, stopInfo, "INFO-STOP " + stop.stopPosition));
        }

        String finalFile = "info-final-cmid-" + safeSlug(job.carrierMoveId) + ".zpl";
        String finalInfo = InfoTagZplBuilder.buildFinalInfoTag(job);
        tasks.add(new PrintTask(TaskKind.FINAL_INFO_TAG, finalFile, finalInfo, "INFO-FINAL " + job.carrierMoveId));
        return tasks;
    }

    private JobCheckpoint createCheckpoint(String id, InputMode mode, String sourceId, Path outputDir, boolean printToFile, PrinterConfig printer, List<PrintTask> tasks) throws Exception {
        JobCheckpoint c = new JobCheckpoint();
        c.id = id;
        c.mode = mode;
        c.sourceId = sourceId;
        c.outputDirectory = outputDir.toAbsolutePath().toString();
        c.printToFile = printToFile;
        c.printerId = printToFile ? "FILE" : printer.getId();
        c.printerEndpoint = printToFile ? "FILE" : printer.getEndpoint();
        c.createdAt = LocalDateTime.now();
        c.updatedAt = c.createdAt;
        c.nextTaskIndex = 0;
        c.completed = false;
        c.tasks = tasks;
        writeCheckpoint(c);
        return c;
    }

    /**
     * Executes print tasks in deterministic order while persisting checkpoint progress after
     * each successful task. This makes resume semantics explicit and crash-safe.
     */
    private void executeTasks(JobCheckpoint checkpoint, PrinterConfig printer, int startIndex) throws Exception {
        Objects.requireNonNull(checkpoint, "checkpoint cannot be null");
        if (checkpoint.tasks == null) {
            throw new IllegalArgumentException("Checkpoint tasks cannot be null.");
        }
        if (checkpoint.tasks.size() > MAX_TASKS_PER_JOB) {
            throw new IllegalArgumentException("Task count exceeds max limit: " + MAX_TASKS_PER_JOB);
        }
        Path outDir = Paths.get(checkpoint.outputDirectory);
        java.nio.file.Files.createDirectories(outDir);
        NetworkPrintService printService = new NetworkPrintService();
        int start = Math.max(0, Math.min(startIndex, checkpoint.tasks.size()));
        for (int i = start; i < checkpoint.tasks.size(); i++) {
            PrintTask task = checkpoint.tasks.get(i);
            Path outFile = outDir.resolve(task.fileName);
            try {
                java.nio.file.Files.writeString(outFile, task.zpl);
                if (!checkpoint.printToFile) {
                    if (printer == null) {
                        throw new IllegalStateException("Printer is required.");
                    }
                    printService.print(printer, task.zpl, task.payloadId);
                }
                checkpoint.nextTaskIndex = i + 1;
                checkpoint.updatedAt = LocalDateTime.now();
                checkpoint.lastError = null;
                writeCheckpoint(checkpoint);
            } catch (Exception ex) {
                checkpoint.completed = false;
                checkpoint.updatedAt = LocalDateTime.now();
                checkpoint.lastError = ex.getMessage();
                writeCheckpoint(checkpoint);
                throw ex;
            }
        }
        checkpoint.completed = true;
        checkpoint.updatedAt = LocalDateTime.now();
        checkpoint.lastError = null;
        writeCheckpoint(checkpoint);
    }

    private PrintResult toResult(JobCheckpoint checkpoint) {
        int labels = 0;
        int info = 0;
        for (PrintTask task : checkpoint.tasks) {
            if (task.kind == TaskKind.PALLET_LABEL) {
                labels++;
            } else {
                info++;
            }
        }
        return new PrintResult(labels, info, Paths.get(checkpoint.outputDirectory), checkpoint.printerId, checkpoint.printerEndpoint, checkpoint.printToFile);
    }

    private JobCheckpoint readCheckpoint(String id) throws Exception {
        return checkpointStore.read(id);
    }

    private void writeCheckpoint(JobCheckpoint checkpoint) throws Exception {
        checkpointStore.write(checkpoint);
    }

    private String stripJsonExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        return fileName.toLowerCase(Locale.ROOT).endsWith(".json")
                ? fileName.substring(0, fileName.length() - 5)
                : fileName;
    }

    private String safeSlug(String value) {
        if (value == null) {
            return "id";
        }
        String slug = NON_ALNUM_PATTERN.matcher(value.toLowerCase(Locale.ROOT)).replaceAll("-");
        return slug.isBlank() ? "id" : slug;
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

        private PrintResult(int labelsPrinted, int infoTagsPrinted, Path outputDirectory, String printerId, String printerEndpoint, boolean printToFile) {
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

        private ResumeCandidate(String checkpointId, InputMode mode, String sourceId, String outputDirectory, int nextTaskIndex, int totalTasks, LocalDateTime updatedAt, String lastError) {
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

        private PrintTask(TaskKind kind, String fileName, String zpl, String payloadId) {
            this.kind = kind;
            this.fileName = fileName;
            this.zpl = zpl;
            this.payloadId = payloadId;
        }
    }
}
