package com.tbg.wms.core.rail;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RailWorkflowServiceTest {

    @Test
    void prepareKeepsRailcarTotalsIndependent() {
        RailDbRepository repo = new RailDbRepository() {
            @Override
            public List<RailStopRecord> findRailStopsByTrainId(String trainId) {
                return List.of(
                        new RailStopRecord("03-04-26", "142", "0124", "CAR1", "BR", "L1",
                                List.of(new RailStopRecord.ItemQuantity("I1", 57))),
                        new RailStopRecord("03-04-26", "143", "0124", "CAR2", "BR", "L2",
                                List.of(new RailStopRecord.ItemQuantity("I1", 1)))
                );
            }

            @Override
            public Map<String, List<RailFootprintCandidate>> findRailFootprintsByShortCode(List<String> shortCodes) {
                return Map.of(
                        "I1",
                        List.of(new RailFootprintCandidate("I1", "ITEM1", "DOM", 56))
                );
            }
        };

        RailWorkflowService.RailWorkflowResult result = new RailWorkflowService(repo).prepare("0124");

        assertEquals(2, result.getCards().size());
        assertEquals(2, result.getCards().get(0).getDomPallets());
        assertEquals(1, result.getCards().get(1).getDomPallets());
    }
}
