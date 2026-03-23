package com.tbg.wms.cli.gui.analyzers.unpicked;

import com.tbg.wms.cli.gui.analyzers.AnalyzerContext;
import com.tbg.wms.cli.gui.analyzers.AnalyzerResult;
import com.tbg.wms.core.AppConfig;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnpickedPartialsDataProviderTest {

    @Test
    void provider_shouldMapQueryRowsIntoTypedAnalyzerRows() throws Exception {
        FakeUnpickedPartialsQueryService query = new FakeUnpickedPartialsQueryService(List.of(
                new UnpickedPartialsQueryService.QueryRow(
                        "3002",
                        LocalDateTime.parse("2026-03-23T10:00:00"),
                        "1000057168",
                        "LOBLAWS",
                        255,
                        0,
                        0,
                        0,
                        255,
                        "3002-JERSEY CITY",
                        "LOBLAWS DC 67",
                        "1000 MAIN ST",
                        "AJAX",
                        "ON"
                )
        ));
        Clock clock = Clock.fixed(Instant.parse("2026-03-23T10:15:00Z"), ZoneOffset.UTC);
        UnpickedPartialsDataProvider provider =
                new UnpickedPartialsDataProvider(query, new UnpickedPartialsRuleClassifier(), clock);

        AnalyzerResult<UnpickedPartialsRow> result = provider.load(new AnalyzerContext(new AppConfig(), clock));

        assertEquals(1, result.rows().size());
        assertEquals("1000057168", result.rows().get(0).orderNumber());
        assertEquals(UnpickedPartialsRule.LOBLAWS, result.rows().get(0).rule());
        assertEquals(Instant.parse("2026-03-23T10:15:00Z"), result.fetchedAt());
    }

    private static final class FakeUnpickedPartialsQueryService extends UnpickedPartialsQueryService {
        private final List<QueryRow> rows;

        private FakeUnpickedPartialsQueryService(List<QueryRow> rows) {
            this.rows = rows;
        }

        @Override
        List<QueryRow> fetchRows(AppConfig config) {
            return rows;
        }
    }
}
