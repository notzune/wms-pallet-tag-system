package com.tbg.wms.core.rail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RailLabelTypographyTest {

    @Test
    void dateTextShouldBeThreePointsLargerThanRouteHeader() {
        assertEquals(RailLabelTypography.ROUTE_HEADER_SIZE + 3f, RailLabelTypography.DATE_SIZE, 0.001f);
    }
}
