package com.tbg.wms.cli.gui;

import com.tbg.wms.core.model.CarrierMoveStopRef;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.model.ShipmentSkuFootprint;
import com.tbg.wms.core.rail.RailFootprintCandidate;
import com.tbg.wms.core.rail.RailStopRecord;
import com.tbg.wms.db.DbQueryRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CarrierMoveWorkflowSupportTest {

    private final CarrierMoveWorkflowSupport support = new CarrierMoveWorkflowSupport();

    @Test
    void prepareCarrierMoveJob_shouldTrimIdResolveShipmentsAndAssignPrintableStopPositions() throws Exception {
        RecordingRepository repo = new RecordingRepository(List.of(
                ref(2, "S200"),
                ref(1, "S101"),
                ref(1, "S100")
        ));
        RecordingShipmentResolver resolver = new RecordingShipmentResolver();

        AdvancedPrintWorkflowService.PreparedCarrierMoveJob job =
                support.prepareCarrierMoveJob(" CM1 ", repo, resolver);

        assertEquals("CM1", repo.carrierMoveId);
        assertEquals("CM1", job.getCarrierMoveId());
        assertEquals(List.of("S100", "S101", "S200"), resolver.shipmentIds);
        assertEquals(2, job.getStopGroups().size());
        assertEquals(Integer.valueOf(1), job.getStopGroups().get(0).getStopSequence());
        assertEquals(1, job.getStopGroups().get(0).getStopPosition());
        assertEquals(2, job.getStopGroups().get(0).getShipmentJobs().size());
        assertEquals(Integer.valueOf(2), job.getStopGroups().get(1).getStopSequence());
        assertEquals(2, job.getStopGroups().get(1).getStopPosition());
        assertEquals(1, job.getStopGroups().get(1).getShipmentJobs().size());
    }

    @Test
    void prepareCarrierMoveJob_shouldRejectBlankCarrierMoveId() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> support.prepareCarrierMoveJob(" ", new RecordingRepository(List.of()), new RecordingShipmentResolver())
        );

        assertEquals("Carrier Move ID is required.", ex.getMessage());
    }

    @Test
    void prepareCarrierMoveJob_shouldRejectCarrierMoveWithNoShipments() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> support.prepareCarrierMoveJob("CM1", new RecordingRepository(List.of()), new RecordingShipmentResolver())
        );

        assertEquals("Carrier Move not found or has no shipments: CM1", ex.getMessage());
    }

    private static CarrierMoveStopRef ref(Integer stopSequence, String shipmentId) {
        return new CarrierMoveStopRef("CM1", "STOP-" + stopSequence, stopSequence, stopSequence, shipmentId, "READY", null);
    }

    private static final class RecordingShipmentResolver implements CarrierMoveWorkflowSupport.ShipmentResolver {
        private final List<String> shipmentIds = new ArrayList<>();

        @Override
        public LabelWorkflowService.PreparedJob prepare(DbQueryRepository repository, String shipmentId) {
            shipmentIds.add(shipmentId);
            Lpn lpn = new Lpn("LPN-" + shipmentId, shipmentId, null, 0, 0, 0.0, null, null, null, null, null, List.of());
            return PreviewSelectionTestData.shipmentJob(shipmentId, List.of(lpn));
        }
    }

    private static final class RecordingRepository implements DbQueryRepository {
        private final List<CarrierMoveStopRef> refs;
        private String carrierMoveId;

        private RecordingRepository(List<CarrierMoveStopRef> refs) {
            this.refs = refs;
        }

        @Override
        public List<CarrierMoveStopRef> findCarrierMoveStops(String carrierMoveId) {
            this.carrierMoveId = carrierMoveId;
            return refs;
        }

        @Override
        public Shipment findShipmentWithLpnsAndLineItems(String shipmentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean shipmentExists(String shipmentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getStagingLocation(String shipmentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ShipmentSkuFootprint> findShipmentSkuFootprints(String shipmentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<RailStopRecord> findRailStopsByTrainId(String trainId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, List<RailFootprintCandidate>> findRailFootprintsByShortCode(List<String> shortCodes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
        }
    }
}
