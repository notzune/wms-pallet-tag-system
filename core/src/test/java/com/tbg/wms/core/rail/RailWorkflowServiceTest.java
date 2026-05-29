package com.tbg.wms.core.rail;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void prepareAllKeepsInputOrderThenCardOrder() {
        RailWorkflowService service = new RailWorkflowService(new RailDbRepository() {
            @Override
            public List<RailStopRecord> findRailStopsByTrainId(String trainId) {
                if ("TRAINB".equals(trainId)) {
                    return List.of(
                            row("TRAINB", "200", "CAR-B1", "I1", 56),
                            row("TRAINB", "201", "CAR-B2", "I1", 112)
                    );
                }
                if ("TRAINA".equals(trainId)) {
                    return List.of(row("TRAINA", "100", "CAR-A1", "I1", 56));
                }
                return List.of();
            }

            @Override
            public Map<String, List<RailFootprintCandidate>> findRailFootprintsByShortCode(List<String> shortCodes) {
                return Map.of("I1", List.of(new RailFootprintCandidate("I1", "ITEM1", "DOM", 56)));
            }
        });

        RailWorkflowService.RailWorkflowBatchResult result = service.prepareAll(List.of("TRAINB", "TRAINA"));

        assertEquals(List.of("TRAINB", "TRAINA"), result.getTrainIds());
        assertEquals(List.of("TRAINB", "TRAINB", "TRAINA"), trainIds(result.getCards()));
        assertEquals(List.of("200", "201", "100"), sequences(result.getCards()));
    }

    @Test
    void prepareAllFailsWhenAnyTrainIsMissing() {
        RailWorkflowService service = new RailWorkflowService(new RailDbRepository() {
            @Override
            public List<RailStopRecord> findRailStopsByTrainId(String trainId) {
                if ("TRAIN1".equals(trainId)) {
                    return List.of(row("TRAIN1", "100", "CAR-1", "I1", 56));
                }
                return List.of();
            }

            @Override
            public Map<String, List<RailFootprintCandidate>> findRailFootprintsByShortCode(List<String> shortCodes) {
                return Map.of("I1", List.of(new RailFootprintCandidate("I1", "ITEM1", "DOM", 56)));
            }
        });

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.prepareAll(List.of("TRAIN1", "MISSING"))
        );

        assertTrue(ex.getMessage().contains("MISSING"));
    }

    @Test
    void prepareBuildsRouteHeaderFromTrainNumberWarehouseAndLoadNumber() {
        RailWorkflowService service = new RailWorkflowService(new RailDbRepository() {
            @Override
            public List<RailStopRecord> findRailStopsByTrainId(String trainId) {
                return List.of(new RailStopRecord("05-29-26", "301", "0526", "TPIX3204", "BR", "8000618166",
                        List.of(new RailStopRecord.ItemQuantity("20548", 2200))));
            }

            @Override
            public Map<String, List<RailFootprintCandidate>> findRailFootprintsByShortCode(List<String> shortCodes) {
                return Map.of("20548", List.of(new RailFootprintCandidate("20548", "ITEM1", "DOM", 56)));
            }
        });

        RailCarCard card = service.prepare("JC05262026").getCards().get(0);

        assertEquals("8000618166", card.getLoadNumbers());
        assertEquals("0526 BR 8000618166", card.getRouteHeader());
    }

    private static RailStopRecord row(String trainId, String sequence, String vehicle, String item, int cases) {
        return new RailStopRecord("03-04-26", sequence, trainId, vehicle, "BR", "L1",
                List.of(new RailStopRecord.ItemQuantity(item, cases)));
    }

    private static List<String> trainIds(List<RailCarCard> cards) {
        List<String> trainIds = new ArrayList<>(cards.size());
        for (RailCarCard card : cards) {
            trainIds.add(card.getTrainId());
        }
        return trainIds;
    }

    private static List<String> sequences(List<RailCarCard> cards) {
        List<String> sequences = new ArrayList<>(cards.size());
        for (RailCarCard card : cards) {
            sequences.add(card.getSequence());
        }
        return sequences;
    }
}
