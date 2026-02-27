/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.3.0
 */

package com.tbg.wms.cli.gui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.label.LabelDataBuilder;
import com.tbg.wms.core.label.LabelType;
import com.tbg.wms.core.model.CarrierMoveStopRef;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.print.NetworkPrintService;
import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.template.ZplTemplateEngine;
import com.tbg.wms.db.DbConnectionPool;
import com.tbg.wms.db.DbQueryRepository;
import com.tbg.wms.db.OracleDbQueryRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Higher-level workflow for carrier move jobs, queueing, and checkpoint resume.
 */
public final class AdvancedPrintWorkflowService {

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

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final AppConfig config;
    private final LabelWorkflowService shipmentService;

    public AdvancedPrintWorkflowService(AppConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.shipmentService = new LabelWorkflowService(config);
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

            Map<Integer, List<CarrierMoveStopRef>> byStop = new TreeMap<>();
            for (CarrierMoveStopRef ref : refs) {
                int key = ref.getStopSequence() == null ? Integer.MAX_VALUE : ref.getStopSequence();
                byStop.computeIfAbsent(key, ignored -> new ArrayList<>()).add(ref);
            }

            List<PreparedStopGroup> groups = new ArrayList<>();
            int stopPosition = 1;
            for (Map.Entry<Integer, List<CarrierMoveStopRef>> entry : byStop.entrySet()) {
                List<CarrierMoveStopRef> stopRefs = new ArrayList<>(entry.getValue());
                stopRefs.sort(Comparator.comparing(CarrierMoveStopRef::getShipmentId));

                LinkedHashSet<String> uniqueShipments = new LinkedHashSet<>();
                for (CarrierMoveStopRef ref : stopRefs) {
                    if (ref.getShipmentId() != null && !ref.getShipmentId().isBlank()) {
                        uniqueShipments.add(ref.getShipmentId());
                    }
                }

                List<LabelWorkflowService.PreparedJob> jobs = new ArrayList<>();
                for (String shipId : uniqueShipments) {
                    jobs.add(shipmentService.prepareJob(repo, shipId));
                }

                if (!jobs.isEmpty()) {
                    Integer stopSequence = stopRefs.get(0).getStopSequence();
                    groups.add(new PreparedStopGroup(stopSequence, stopPosition, jobs));
                    stopPosition++;
                }
            }

            if (groups.isEmpty()) {
                throw new IllegalArgumentException("Carrier Move has no printable shipments: " + cmid);
            }

            return new PreparedCarrierMoveJob(cmid, groups);
        }
    }

    public PreparedQueueJob prepareQueue(List<QueueRequestItem> requests) throws Exception {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Queue is empty.");
        }
        List<PreparedQueueItem> resolved = new ArrayList<>();
        for (QueueRequestItem req : requests) {
            if (req == null || req.id == null || req.id.isBlank()) {
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
        Objects.requireNonNull(job, "job cannot be null");
        PrinterConfig printer = resolvePrinterForPrint(job.getRouting(), printerId, printToFile);
        Path targetDir = outputDir == null
                ? Paths.get("out", "gui-" + job.getShipmentId() + "-" + TS.format(LocalDateTime.now()))
                : outputDir;
        List<PrintTask> tasks = buildShipmentTasks(job, null, null, true);
        JobCheckpoint checkpoint = createCheckpoint("shipment-" + job.getShipmentId() + "-" + TS.format(LocalDateTime.now()),
                InputMode.SHIPMENT, job.getShipmentId(), targetDir, printToFile, printer, tasks);
        executeTasks(checkpoint, printer, 0);
        return toResult(checkpoint);
    }

    public PrintResult printCarrierMoveJob(PreparedCarrierMoveJob job, String printerId, Path outputDir, boolean printToFile) throws Exception {
        Objects.requireNonNull(job, "job cannot be null");
        PrinterConfig printer = resolvePrinterForPrint(job.stopGroups.get(0).shipmentJobs.get(0).getRouting(), printerId, printToFile);
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
        Path dir = checkpointDir();
        if (!Files.exists(dir)) {
            return List.of();
        }
        List<ResumeCandidate> items = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            List<Path> files = stream
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .collect(Collectors.toList());
            for (Path file : files) {
                JobCheckpoint checkpoint = MAPPER.readValue(file.toFile(), JobCheckpoint.class);
                if (!checkpoint.completed) {
                    int total = checkpoint.tasks == null ? 0 : checkpoint.tasks.size();
                    items.add(new ResumeCandidate(checkpoint.id, checkpoint.mode, checkpoint.sourceId, checkpoint.outputDirectory,
                            checkpoint.nextTaskIndex, total, checkpoint.updatedAt, checkpoint.lastError));
                }
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

    private List<PrintTask> buildShipmentTasks(
            LabelWorkflowService.PreparedJob job,
            Integer stopSequence,
            Integer stopPosition,
            boolean includeShipmentInfoTag
    ) {
        LabelDataBuilder builder = new LabelDataBuilder(job.getSkuMapping(), job.getSiteConfig(), job.getFootprintBySku());
        List<PrintTask> tasks = new ArrayList<>();
        for (int i = 0; i < job.getLpnsForLabels().size(); i++) {
            Lpn lpn = job.getLpnsForLabels().get(i);
            Map<String, String> data = new LinkedHashMap<>(builder.build(job.getShipment(), lpn, i, LabelType.WALMART_CANADA_GRID));
            if (stopSequence != null) {
                data.put("stopSequence", String.valueOf(stopSequence));
            }
            if (job.getShipment().getLpnCount() == 0) {
                data.put("palletSeq", String.valueOf(i + 1));
                data.put("palletTotal", String.valueOf(job.getLpnsForLabels().size()));
            }
            String zpl = ZplTemplateEngine.generate(job.getTemplate(), data);
            String fileName = String.format("%s_%s_%d_of_%d.zpl",
                    job.getShipmentId(), lpn.getLpnId(), i + 1, job.getLpnsForLabels().size());
            String payload = job.getShipmentId() + ":" + lpn.getLpnId() + (stopPosition == null ? "" : (" stop " + stopPosition));
            tasks.add(new PrintTask(TaskKind.PALLET_LABEL, fileName, zpl, payload));
        }

        if (includeShipmentInfoTag) {
            String infoFile = "info-shipment-" + safeSlug(job.getShipmentId()) + ".zpl";
            String infoZpl = buildShipmentInfoTag(job);
            tasks.add(new PrintTask(TaskKind.STOP_INFO_TAG, infoFile, infoZpl, "INFO-SHIPMENT " + job.getShipmentId()));
        }
        return tasks;
    }

    private List<PrintTask> buildCarrierMoveTasks(PreparedCarrierMoveJob job) {
        List<PrintTask> tasks = new ArrayList<>();
        int totalStops = job.stopGroups.size();
        for (PreparedStopGroup stop : job.stopGroups) {
            List<String> shipmentIds = new ArrayList<>();
            for (LabelWorkflowService.PreparedJob shipmentJob : stop.shipmentJobs) {
                shipmentIds.add(shipmentJob.getShipmentId());
                tasks.addAll(buildShipmentTasks(shipmentJob, stop.stopSequence, stop.stopPosition, false));
            }

            String stopInfoFile = String.format("info-stop-%02d-of-%02d.zpl", stop.stopPosition, totalStops);
            String stopInfo = buildStopInfoTag(job.carrierMoveId, stop.stopPosition, totalStops, stop.stopSequence, shipmentIds, stop.shipmentJobs);
            tasks.add(new PrintTask(TaskKind.STOP_INFO_TAG, stopInfoFile, stopInfo, "INFO-STOP " + stop.stopPosition));
        }

        String finalFile = "info-final-cmid-" + safeSlug(job.carrierMoveId) + ".zpl";
        String finalInfo = buildFinalInfoTag(job);
        tasks.add(new PrintTask(TaskKind.FINAL_INFO_TAG, finalFile, finalInfo, "INFO-FINAL " + job.carrierMoveId));
        return tasks;
    }

    private String buildStopInfoTag(String cmid, int stopPosition, int totalStops, Integer stopSequence, List<String> shipmentIds, List<LabelWorkflowService.PreparedJob> jobs) {
        LabelWorkflowService.PreparedJob first = jobs.isEmpty() ? null : jobs.get(0);
        String shipTo = first == null ? "-" : compact(first.getShipment().getShipToName());
        String addr = first == null ? "-" : compact(first.getShipment().getShipToAddress1() + " " + first.getShipment().getShipToCity() + ", " + first.getShipment().getShipToState() + " " + first.getShipment().getShipToZip());
        String seqText = stopSequence == null ? "-" : stopSequence.toString();
        String shipments = shipmentIds.isEmpty() ? "-" : String.join(", ", shipmentIds);
        return "^XA\n^CI28\n^PW812\n^LL1218\n^LH0,0\n"
                + "^FO16,16^GB780,1186,6^FS\n"
                + "^FO30,40^A0N,58,58^FDINFO TAG - DO NOT APPLY^FS\n"
                + "^FO30,120^A0N,32,32^FDCARRIER MOVE: " + esc(cmid) + "^FS\n"
                + "^FO30,170^A0N,32,32^FDSTOP " + stopPosition + " OF " + totalStops + " (SEQ " + esc(seqText) + ")^FS\n"
                + "^FO30,220^A0N,28,28^FB740,3,6,L,0^FDSHIPMENTS: " + esc(shipments) + "^FS\n"
                + "^FO30,330^A0N,28,28^FB740,2,6,L,0^FDSHIP TO: " + esc(shipTo) + "^FS\n"
                + "^FO30,420^A0N,24,24^FB740,3,4,L,0^FD" + esc(addr) + "^FS\n"
                // Footer docs reference intentionally disabled for now.
                // + "^FO30,1040^A0N,28,28^FDDocs: README^FS\n"
                + "^FO30,1080^A0N,34,34^FDSORT PACKET FOR STOP " + stopPosition + "^FS\n^XZ\n";
    }

    private String buildFinalInfoTag(PreparedCarrierMoveJob job) {
        StringBuilder list = new StringBuilder();
        for (PreparedStopGroup stop : job.stopGroups) {
            List<String> ids = new ArrayList<>();
            for (LabelWorkflowService.PreparedJob ship : stop.shipmentJobs) {
                ids.add(ship.getShipmentId());
            }
            if (list.length() > 0) {
                list.append("\\&");
            }
            list.append("Stop ").append(stop.stopPosition).append(": ").append(String.join(", ", ids));
        }
        return "^XA\n^CI28\n^PW812\n^LL1218\n^LH0,0\n"
                + "^FO16,16^GB780,1186,6^FS\n"
                + "^FO30,40^A0N,58,58^FDFINAL INFO TAG - DO NOT APPLY^FS\n"
                + "^FO30,120^A0N,32,32^FDCARRIER MOVE: " + esc(job.carrierMoveId) + "^FS\n"
                + "^FO30,170^A0N,32,32^FDTOTAL STOPS: " + job.stopGroups.size() + "^FS\n"
                + "^FO30,230^A0N,26,26^FB740,28,4,L,0^FD" + esc(list.toString()) + "^FS\n"
                + "^FO30,1040^A0N,28,28^FDDocs: CHANGELOG^FS\n"
                + "^FO30,1080^A0N,34,34^FDEND OF CARRIER MOVE " + esc(job.carrierMoveId) + "^FS\n^XZ\n";
    }

    private String buildShipmentInfoTag(LabelWorkflowService.PreparedJob job) {
        String shipmentId = job.getShipment().getShipmentId();
        String shipTo = compact(job.getShipment().getShipToName());
        String addr = compact(job.getShipment().getShipToAddress1() + " " + job.getShipment().getShipToCity() + ", "
                + job.getShipment().getShipToState() + " " + job.getShipment().getShipToZip());
        String carrierMove = job.getShipment().getCarrierMoveId() == null || job.getShipment().getCarrierMoveId().isBlank()
                ? "-" : job.getShipment().getCarrierMoveId();
        int labels = job.getLpnsForLabels().size();

        return "^XA\n^CI28\n^PW812\n^LL1218\n^LH0,0\n"
                + "^FO16,16^GB780,1186,6^FS\n"
                + "^FO30,40^A0N,58,58^FDINFO TAG - DO NOT APPLY^FS\n"
                + "^FO30,120^A0N,32,32^FDSHIPMENT ID: " + esc(shipmentId) + "^FS\n"
                + "^FO30,170^A0N,32,32^FDCARRIER MOVE: " + esc(carrierMove) + "^FS\n"
                + "^FO30,220^A0N,32,32^FDLABELS IN JOB: " + labels + "^FS\n"
                + "^FO30,280^A0N,28,28^FB740,2,6,L,0^FDSHIP TO: " + esc(shipTo) + "^FS\n"
                + "^FO30,360^A0N,24,24^FB740,3,4,L,0^FD" + esc(addr) + "^FS\n"
                // Footer docs reference intentionally disabled for now.
                // + "^FO30,1040^A0N,28,28^FDDocs: README^FS\n"
                + "^FO30,1080^A0N,34,34^FDSHIPMENT PACKET SUMMARY^FS\n^XZ\n";
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

    private void executeTasks(JobCheckpoint checkpoint, PrinterConfig printer, int startIndex) throws Exception {
        Path outDir = Paths.get(checkpoint.outputDirectory);
        Files.createDirectories(outDir);
        NetworkPrintService printService = new NetworkPrintService();
        int start = Math.max(0, Math.min(startIndex, checkpoint.tasks.size()));
        for (int i = start; i < checkpoint.tasks.size(); i++) {
            PrintTask task = checkpoint.tasks.get(i);
            Path outFile = outDir.resolve(task.fileName);
            try {
                Files.writeString(outFile, task.zpl);
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
        Path file = checkpointDir().resolve(id + ".json");
        if (!Files.exists(file)) {
            return null;
        }
        return MAPPER.readValue(file.toFile(), JobCheckpoint.class);
    }

    private void writeCheckpoint(JobCheckpoint checkpoint) throws Exception {
        Path dir = checkpointDir();
        Files.createDirectories(dir);
        Path file = dir.resolve(checkpoint.id + ".json");
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), checkpoint);
    }

    private Path checkpointDir() {
        return Paths.get("out", "gui-jobs");
    }

    private String compact(String value) {
        if (value == null) {
            return "-";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String safeSlug(String value) {
        return value == null ? "id" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    }

    private String esc(String value) {
        if (value == null) {
            return " ";
        }
        return value.replace("~", "~~").replace("^", "~~^").replace("{", "{{").replace("}", "}}");
    }

    public static final class PreparedCarrierMoveJob {
        private final String carrierMoveId;
        private final List<PreparedStopGroup> stopGroups;

        private PreparedCarrierMoveJob(String carrierMoveId, List<PreparedStopGroup> stopGroups) {
            this.carrierMoveId = carrierMoveId;
            this.stopGroups = List.copyOf(stopGroups);
        }

        public String getCarrierMoveId() { return carrierMoveId; }
        public List<PreparedStopGroup> getStopGroups() { return stopGroups; }
        public int getTotalStops() { return stopGroups.size(); }
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

        public Integer getStopSequence() { return stopSequence; }
        public int getStopPosition() { return stopPosition; }
        public List<LabelWorkflowService.PreparedJob> getShipmentJobs() { return shipmentJobs; }
    }

    public static final class QueueRequestItem {
        private final QueueItemType type;
        private final String id;
        public QueueRequestItem(QueueItemType type, String id) {
            this.type = Objects.requireNonNull(type, "type");
            this.id = Objects.requireNonNull(id, "id").trim();
        }
        public QueueItemType getType() { return type; }
        public String getId() { return id; }
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
        private PreparedQueueJob(List<PreparedQueueItem> items) { this.items = List.copyOf(items); }
        public List<PreparedQueueItem> getItems() { return items; }
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

        public int getLabelsPrinted() { return labelsPrinted; }
        public int getInfoTagsPrinted() { return infoTagsPrinted; }
        public Path getOutputDirectory() { return outputDirectory; }
        public String getPrinterId() { return printerId; }
        public String getPrinterEndpoint() { return printerEndpoint; }
        public boolean isPrintToFile() { return printToFile; }
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
        public List<PrintResult> getItemResults() { return itemResults; }
        public int getTotalLabelsPrinted() { return totalLabelsPrinted; }
        public int getTotalInfoTagsPrinted() { return totalInfoTagsPrinted; }
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

        public String checkpointId() { return checkpointId; }
        public InputMode mode() { return mode; }
        public String sourceId() { return sourceId; }
        public String outputDirectory() { return outputDirectory; }
        public int nextTaskIndex() { return nextTaskIndex; }
        public int totalTasks() { return totalTasks; }
        public LocalDateTime updatedAt() { return updatedAt; }
        public String lastError() { return lastError; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class JobCheckpoint {
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
    private static final class PrintTask {
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
