package com.tbg.wms.core.rail;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RailPalletCalculatorTest {

    @Test
    void calculateUsesCeilingAndSeparatesCanAndDomPerRailcar() {
        RailCarAggregate aggregate = new RailCarAggregate(
                "03-04-26",
                "143",
                "0124",
                "TPIX3010",
                "BR",
                Set.of("LOAD1"),
                new LinkedHashMap<>(Map.of(
                        "CAN1", 2300,
                        "DOM1", 120,
                        "DOM2", 56
                ))
        );

        Map<String, RailFamilyFootprint> footprints = Map.of(
                "CAN1", new RailFamilyFootprint("CAN1", "CAN", 56),
                "DOM1", new RailFamilyFootprint("DOM1", "DOM", 50),
                "DOM2", new RailFamilyFootprint("DOM2", "DOM", 56)
        );

        RailPalletCalculator.RailPalletResult result = new RailPalletCalculator().calculate(aggregate, footprints);

        assertEquals(42, result.getCanPallets());
        assertEquals(4, result.getDomPallets());
        assertEquals(0, result.getMissingItems().size());
    }
}
