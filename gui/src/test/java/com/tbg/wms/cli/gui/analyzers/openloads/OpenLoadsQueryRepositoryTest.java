package com.tbg.wms.cli.gui.analyzers.openloads;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenLoadsQueryRepositoryTest {

    @Test
    void fetchRows_shouldExecuteOpenLoadsSqlMapRowsAndCloseJdbcResources() throws Exception {
        JdbcStub jdbc = JdbcStub.withRow(rowValues());
        OpenLoadsQueryRepository repository = new OpenLoadsQueryRepository(jdbc.dataSource(), new OpenLoadsRowMapper());

        List<OpenLoadsRow> rows = repository.fetchRows();

        assertEquals(OpenLoadsSql.query(), jdbc.preparedSql);
        assertEquals(1, rows.size());
        OpenLoadsRow row = rows.get(0);
        assertEquals("3002", row.warehouseId());
        assertEquals("243177", row.carrierMoveId());
        assertEquals(LocalDateTime.of(2026, 3, 25, 8, 0), row.appointment());
        assertEquals(7, row.staged());
        assertTrue(jdbc.connectionClosed);
        assertTrue(jdbc.statementClosed);
        assertTrue(jdbc.resultSetClosed);
    }

    private static Map<String, Object> rowValues() {
        Map<String, Object> values = new HashMap<>();
        values.put("wh_id", "3002");
        values.put("car_move_id", "243177");
        values.put("ordnum", "8000574009");
        values.put("ship_id", "8000574009");
        values.put("d_l", "DROP");
        values.put("carcod", "ABCD");
        values.put("trlr_num", "TRL123");
        values.put("yard_loc", "Y1");
        values.put("shpsts", "Scheduled");
        values.put("appt", Timestamp.valueOf(LocalDateTime.of(2026, 3, 25, 8, 0)));
        values.put("dstloc", "ATL");
        values.put("customer", "Customer");
        values.put("carnam", "Carrier");
        values.put("casepicks", 120);
        values.put("case_picks_comp", 100);
        values.put("picks_rem", 20);
        values.put("platform", "PA");
        values.put("shp_dck_flg", "Y");
        values.put("trlr_cod", "RCV");
        values.put("nottxt", "note");
        values.put("staged", 7);
        values.put("stop_seq", 2);
        values.put("short", "SHORT");
        return values;
    }

    private static final class JdbcStub {
        private final Map<String, Object> rowValues;
        private boolean rowPending = true;
        private boolean lastWasNull;
        private String preparedSql;
        private boolean connectionClosed;
        private boolean statementClosed;
        private boolean resultSetClosed;

        private JdbcStub(Map<String, Object> rowValues) {
            this.rowValues = rowValues;
        }

        private static JdbcStub withRow(Map<String, Object> rowValues) {
            return new JdbcStub(rowValues);
        }

        private DataSource dataSource() {
            return proxy(DataSource.class, (proxy, method, args) -> {
                if ("getConnection".equals(method.getName())) {
                    return connection();
                }
                return defaultValue(proxy, method, args);
            });
        }

        private Connection connection() {
            return proxy(Connection.class, (proxy, method, args) -> {
                if ("prepareStatement".equals(method.getName())) {
                    preparedSql = (String) args[0];
                    return statement();
                }
                if ("close".equals(method.getName())) {
                    connectionClosed = true;
                    return null;
                }
                if ("isClosed".equals(method.getName())) {
                    return connectionClosed;
                }
                return defaultValue(proxy, method, args);
            });
        }

        private PreparedStatement statement() {
            return proxy(PreparedStatement.class, (proxy, method, args) -> {
                if ("executeQuery".equals(method.getName())) {
                    return resultSet();
                }
                if ("close".equals(method.getName())) {
                    statementClosed = true;
                    return null;
                }
                if ("isClosed".equals(method.getName())) {
                    return statementClosed;
                }
                return defaultValue(proxy, method, args);
            });
        }

        private ResultSet resultSet() {
            return proxy(ResultSet.class, (proxy, method, args) -> {
                String methodName = method.getName();
                if ("next".equals(methodName)) {
                    boolean hasRow = rowPending;
                    rowPending = false;
                    return hasRow;
                }
                if ("getString".equals(methodName)) {
                    Object value = rowValues.get((String) args[0]);
                    lastWasNull = value == null;
                    return value == null ? null : value.toString();
                }
                if ("getInt".equals(methodName)) {
                    Object value = rowValues.get((String) args[0]);
                    lastWasNull = value == null;
                    return value == null ? 0 : ((Number) value).intValue();
                }
                if ("getTimestamp".equals(methodName)) {
                    Object value = rowValues.get((String) args[0]);
                    lastWasNull = value == null;
                    return value;
                }
                if ("wasNull".equals(methodName)) {
                    return lastWasNull;
                }
                if ("close".equals(methodName)) {
                    resultSetClosed = true;
                    return null;
                }
                if ("isClosed".equals(methodName)) {
                    return resultSetClosed;
                }
                return defaultValue(proxy, method, args);
            });
        }

        @SuppressWarnings("unchecked")
        private static <T> T proxy(Class<T> type, InvocationHandler handler) {
            return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
        }

        private static Object defaultValue(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "toString" -> proxy.getClass().getInterfaces()[0].getSimpleName() + "Proxy";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> primitiveDefault(method.getReturnType());
            };
        }

        private static Object primitiveDefault(Class<?> returnType) {
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == byte.class) {
                return (byte) 0;
            }
            if (returnType == short.class) {
                return (short) 0;
            }
            if (returnType == int.class) {
                return 0;
            }
            if (returnType == long.class) {
                return 0L;
            }
            if (returnType == float.class) {
                return 0F;
            }
            if (returnType == double.class) {
                return 0D;
            }
            if (returnType == char.class) {
                return '\0';
            }
            return null;
        }
    }
}
