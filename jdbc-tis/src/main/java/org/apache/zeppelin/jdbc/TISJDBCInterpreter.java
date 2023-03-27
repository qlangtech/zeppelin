package org.apache.zeppelin.jdbc;

import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDriver;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.zeppelin.util.PropertiesUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.plugin.ds.DBConfig;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.DataSourceFactoryPluginStore;
import com.qlangtech.tis.plugin.ds.PostedDSProp;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-12-09 12:04
 **/
public class TISJDBCInterpreter extends JDBCInterpreter {

    private static final Logger logger = LoggerFactory.getLogger(TISJDBCInterpreter.class);
    static final String TIS_DB_NAME = "tisDbName";

    public TISJDBCInterpreter(Properties property) {
        super(property);
    }

    @Override
    protected boolean isKerboseEnabled() {
        return false;
    }

//    @Override
//    protected Connection getConnectionFromPool(
//            String url, String user, String dbPrefix, Properties properties)
//            throws SQLException, ClassNotFoundException {
//        String tisDbName = properties.getProperty(TIS_DB_NAME);
//        if (StringUtils.isEmpty(tisDbName)) {
//            throw new IllegalStateException("param tisDbName can not be null");
//        }
//        DataSourceFactoryPluginStore dsStore = TIS.getDataBasePluginStore(new PostedDSProp(tisDbName));
//        DataSourceFactory dsFactory = dsStore.getPlugin();
//        Objects.requireNonNull(dsFactory, "dsFactory can not be null");
//        List<String> jdbcUrls = Lists.newArrayList();
//        DBConfig dbConfig = dsFactory.getDbConfig();
//        dbConfig.vistDbURL(true, (dbName, dbHost, jdbcUrl) -> {
//            jdbcUrls.add(jdbcUrl);
//        }, false);
//
//        for (String u : jdbcUrls) {
//
//            createConnectionPool(u, user, dbPrefix, () -> {
//                return dsFactory.getConnection(u);
//            });
//            getJDBCConfiguration(user). (dbPrefix, driver);
//            return;
//        }
//        throw new IllegalStateException("tisDbName:" + tisDbName + " can not create relevant Connection");
//    }

    @Override
    protected void createConnectionPool(String url, String user, String dbPrefix, Properties properties) throws SQLException, ClassNotFoundException {
        logger.info("Creating connection pool for url: {}, user: {}, dbPrefix: {}, properties: {}",
                url, user, dbPrefix, properties);

//        /* Remove properties that is not valid properties for presto/trino by checking driver key.
//         * - Presto: com.facebook.presto.jdbc.PrestoDriver
//         * - Trino(ex. PrestoSQL): io.trino.jdbc.TrinoDriver / io.prestosql.jdbc.PrestoDriver
//         */
//        String driverClass = properties.getProperty(DRIVER_KEY);
//        if (driverClass != null && (driverClass.equals("com.facebook.presto.jdbc.PrestoDriver")
//                || driverClass.equals("io.prestosql.jdbc.PrestoDriver")
//                || driverClass.equals("io.trino.jdbc.TrinoDriver"))) {
//            for (String key : properties.stringPropertyNames()) {
//                if (!PRESTO_PROPERTIES.contains(key)) {
//                    properties.remove(key);
//                }
//            }
//        }

//        ConnectionFactory connectionFactory =
//                new DriverManagerConnectionFactory(url, properties);

        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(() -> {

            String tisDbName = properties.getProperty(TIS_DB_NAME);
            if (StringUtils.isEmpty(tisDbName)) {
                throw new IllegalStateException("param tisDbName can not be null");
            }
            DataSourceFactoryPluginStore dsStore = TIS.getDataSourceFactoryPluginStore(PostedDSProp.parse(tisDbName));
            DataSourceFactory dsFactory = dsStore.getPlugin();
            Objects.requireNonNull(dsFactory, "dsFactory can not be null");
            List<String> jdbcUrls = Lists.newArrayList();
            DBConfig dbConfig = dsFactory.getDbConfig();
            dbConfig.vistDbURL(false, (dbName, dbHost, jdbcUrl) -> {
                jdbcUrls.add(jdbcUrl);
            }, false);

            for (String u : jdbcUrls) {
                return dsFactory.getConnection(u, false).getConnection();
            }

            throw new IllegalStateException("tisDbName:" + tisDbName + " can not create relevant Connection");
        }, null);


        final String maxConnectionLifetime =
                StringUtils.defaultIfEmpty(getProperty("zeppelin.jdbc.maxConnLifetime"), "-1");
        poolableConnectionFactory.setMaxConnLifetimeMillis(Long.parseLong(maxConnectionLifetime));
        poolableConnectionFactory.setValidationQuery(
                PropertiesUtil.getString(properties, "validationQuery", "show databases"));
        ObjectPool connectionPool = new GenericObjectPool(poolableConnectionFactory);
        this.configConnectionPool((GenericObjectPool) connectionPool, properties);

        poolableConnectionFactory.setPool(connectionPool);
        // Class.forName(driverClass);
        PoolingDriver driver = new PoolingDriver();
        driver.registerPool(dbPrefix + user, connectionPool);
        getJDBCConfiguration(user).saveDBDriverPool(dbPrefix, driver);
    }


}
