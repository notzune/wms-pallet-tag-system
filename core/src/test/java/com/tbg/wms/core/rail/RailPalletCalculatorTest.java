package com.tbg.wms.core.rail;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class RailPalletCalculatorTest {

    @Test
    void calculateUsesCeilingAndSeparatesCanDomAndKevPerRailcar() {
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
                        "DOM2", 56,
                        "KEV1", 21
                ))
        );

        Map<String, RailFamilyFootprint> footprints = Map.of(
                "CAN1", new RailFamilyFootprint("CAN1", "CAN", 56),
                "DOM1", new RailFamilyFootprint("DOM1", "DOM", 50),
                "DOM2", new RailFamilyFootprint("DOM2", "DOM", 56),
                "KEV1", new RailFamilyFootprint("KEV1", "KEV", 20)
        );

        RailPalletCalculator.RailPalletResult result = new RailPalletCalculator().calculate(aggregate, footprints);

        assertEquals(42, result.getCanPallets());
        assertEquals(4, result.getDomPallets());
        assertEquals(2, result.getKevPallets());
        assertEquals(0, result.getMissingItems().size());
    }

    @Test
    void calculateUsesOverflowSafeCeilingDivision() {
        RailCarAggregate aggregate = new RailCarAggregate(
                "03-04-26",
                "143",
                "0124",
                "TPIX3010",
                "BR",
                Set.of("LOAD1"),
                new LinkedHashMap<>(Map.of("CAN1", Integer.MAX_VALUE))
        );
        Map<String, RailFamilyFootprint> footprints = Map.of(
                "CAN1", new RailFamilyFootprint("CAN1", "CAN", Integer.MAX_VALUE)
        );

        RailPalletCalculator.RailPalletResult result = new RailPalletCalculator().calculate(aggregate, footprints);
        assertEquals(1, result.getCanPallets());
    }

    @Test
    void calculateThrowsWhenBucketTotalOverflowsIntRange() {
        RailCarAggregate aggregate = new RailCarAggregate(
                "03-04-26",
                "143",
                "0124",
                "TPIX3010",
                "BR",
                Set.of("LOAD1"),
                new LinkedHashMap<>(Map.of(
                        "CAN1", Integer.MAX_VALUE,
                        "CAN2", Integer.MAX_VALUE
                ))
        );
        Map<String, RailFamilyFootprint> footprints = Map.of(
                "CAN1", new RailFamilyFootprint("CAN1", "CAN", 1),
                "CAN2", new RailFamilyFootprint("CAN2", "CAN", 1)
        );

        assertThrows(ArithmeticException.class, () -> new RailPalletCalculator().calculate(aggregate, footprints));
    }
}
