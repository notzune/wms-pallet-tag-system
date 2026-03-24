package com.tbg.wms.cli.gui;

import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.model.ShipmentSkuFootprint;
import com.tbg.wms.core.rail.RailFootprintCandidate;
import com.tbg.wms.core.rail.RailStopRecord;
import com.tbg.wms.db.DbQueryRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabelWorkflowJobPreparationSupportTest {

    private final LabelWorkflowJobPreparationSupport support = new LabelWorkflowJobPreparationSupport();

    @Test
    void loadShipmentData_shouldRejectMissingShipment() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> support.loadShipmentData(new StubRepository(null, List.of()), "SHIP-1", new LabelWorkflowPlanningSupport())
        );

        assertEquals("Shipment not found: SHIP-1", ex.getMessage());
    }

    @Test
    void loadShipmentData_shouldBuildVirtualLabelPlanWhenShipmentHasNoPhysicalLpns() {
        Shipment shipment = new Shipment(
                "SHIP-1", "EXT-1", "ORDER-1", "3002",
                "Ship To", "123 Any St", null, null,
                "City", "ST", "12345", "USA", null,
                "CARRIER", "TL", null, null, "STAGE",
                null, "6080", null, null, 1, "CM1", null, null,
                "R", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), List.of()
        );
        ShipmentSkuFootprint footprint = new ShipmentSkuFootprint("SKU1", "Desc", 120, 60, 10, 20.0, 30.0, 40.0);

        LabelWorkflowJobPreparationSupport.LoadedShipmentData loaded = support.loadShipmentData(
                new StubRepository(shipment, List.of(footprint)),
                "SHIP-1",
                new LabelWorkflowPlanningSupport()
        );

        assertEquals("SHIP-1", loaded.shipmentId());
        assertTrue(loaded.usingVirtualLabels());
        assertEquals("STAGE", loaded.stagingLocation());
        assertTrue(loaded.lpnsForLabels().size() > 0);
        assertEquals(1, loaded.footprintBySku().size());
    }

    private static final class StubRepository implements DbQueryRepository {
        private final Shipment shipment;
        private final List<ShipmentSkuFootprint> footprints;

        private StubRepository(Shipment shipment, List<ShipmentSkuFootprint> footprints) {
            this.shipment = shipment;
            this.footprints = footprints;
        }

        @Override
        public Shipment findShipmentWithLpnsAndLineItems(String shipmentId) {
            return shipment;
        }

        @Override
        public boolean shipmentExists(String shipmentId) {
            return shipment != null;
        }

        @Override
        public String getStagingLocation(String shipmentId) {
            return shipment == null ? null : shipment.getDestinationLocation();
        }

        @Override
        public List<ShipmentSkuFootprint> findShipmentSkuFootprints(String shipmentId) {
            return footprints;
        }

        @Override
        public List<com.tbg.wms.core.model.CarrierMoveStopRef> findCarrierMoveStops(String carrierMoveId) {
            return List.of();
        }

        @Override
        public List<RailStopRecord> findRailStopsByTrainId(String trainId) {
            return List.of();
        }

        @Override
        public Map<String, List<RailFootprintCandidate>> findRailFootprintsByShortCode(List<String> shortCodes) {
            return Map.of();
        }

        @Override
        public void close() {
        }
    }
}
