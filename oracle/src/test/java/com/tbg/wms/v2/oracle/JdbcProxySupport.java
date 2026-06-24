package com.tbg.wms.v2.oracle;

import javax.sql.DataSource;
import javax.sql.rowset.CachedRowSet;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public final class JdbcProxySupport {
    private JdbcProxySupport() {
    }

    public static RecordingDatabase database(CachedRowSet rowSet) {
        return new RecordingDatabase(rowSet);
    }

    public static final class RecordingDatabase {
        private final CachedRowSet rowSet;
        private final List<String> preparedSql = new ArrayList<>();
        private final List<Object> parameters = new ArrayList<>();

        private RecordingDatabase(CachedRowSet rowSet) {
            this.rowSet = rowSet;
        }

        public DataSource dataSource() {
            InvocationHandler handler = (proxy, method, args) -> {
                if ("getConnection".equals(method.getName())) {
                    return connection();
                }
                if ("toString".equals(method.getName())) {
                    return "RecordingDataSource";
                }
                return defaultValue(method.getReturnType());
            };
            return (DataSource) Proxy.newProxyInstance(
                    DataSource.class.getClassLoader(),
                    new Class<?>[]{DataSource.class},
                    handler
            );
        }

        public List<String> preparedSql() {
            return List.copyOf(preparedSql);
        }

        public List<Object> parameters() {
            return List.copyOf(parameters);
        }

        private Connection connection() {
            InvocationHandler handler = (proxy, method, args) -> {
                if ("prepareStatement".equals(method.getName())) {
                    preparedSql.add((String) args[0]);
                    return statement();
                }
                if ("close".equals(method.getName())) {
                    return null;
                }
                return defaultValue(method.getReturnType());
            };
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    handler
            );
        }

        private PreparedStatement statement() {
            InvocationHandler handler = (proxy, method, args) -> {
                if ("setString".equals(method.getName()) || "setObject".equals(method.getName())) {
                    parameters.add(args[1]);
                    return null;
                }
                if ("executeQuery".equals(method.getName())) {
                    rowSet.beforeFirst();
                    return rowSet;
                }
                if ("close".equals(method.getName())) {
                    return null;
                }
                return defaultValue(method.getReturnType());
            };
            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    handler
            );
        }

        private static Object defaultValue(Class<?> returnType) {
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
                return 0.0f;
            }
            if (returnType == double.class) {
                return 0.0d;
            }
            return null;
        }
    }
}
