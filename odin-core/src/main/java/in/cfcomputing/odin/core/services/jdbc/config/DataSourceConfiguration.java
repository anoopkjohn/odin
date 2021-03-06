/*
 * Copyright (c) 2017 cFactor Computing Pvt. Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package in.cfcomputing.odin.core.services.jdbc.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import in.cfcomputing.odin.core.services.jdbc.DataSourceProperties;
import in.cfcomputing.odin.core.services.jdbc.exception.DataSourceConfigurationException;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author gibugeorge on 12/12/16.
 * @version 1.0
 */
@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
@ConditionalOnProperty(prefix = "jdbc.data-source", value = "enabled", havingValue = "true", matchIfMissing = false)
public class DataSourceConfiguration implements EnvironmentAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceConfiguration.class);
    @Autowired
    private DataSourceProperties dataSourceProperties;

    private Environment environment;


    private String[] getDsNames() {
        final String commaSeparatedDSNames = dataSourceProperties.getNames();
        if (StringUtils.isEmpty(commaSeparatedDSNames)) {
            throw new DataSourceConfigurationException("\"jdbc.data-source.names\" property cannot be empty when \"jdbc.data-source.enabled=true\".");
        }
        return commaSeparatedDSNames.split(",");
    }

    private String getProperty(final String key, final String dataSourceName) {
        final String actualKey = "jdbc.data-source." + dataSourceName + "." + key;
        return environment.getProperty(actualKey) != null ? environment.getProperty(actualKey) : environment.getProperty("jdbc.data-source.default." + key);
    }

    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }

    @Bean
    public boolean createDataSources(final ConfigurableApplicationContext applicationContext) {
        final String[] dataSourceNames = getDsNames();
        for (final String dataSourceName : dataSourceNames) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Configuring datasource with name {}", dataSourceName);
            }
            final HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setPoolName(dataSourceName);
            final String jdbcUrl = getProperty("jdbc-url", dataSourceName);
            if (StringUtils.isEmpty(jdbcUrl)) {
                throw new DataSourceConfigurationException("jdbc-url property cannot be empty for datasource " + dataSourceName);
            }
            hikariConfig.setJdbcUrl(jdbcUrl);
            final String userName = getProperty("user-name", dataSourceName) != null ? getProperty("user-name", dataSourceName) : "";
            hikariConfig.setUsername(userName);
            final String password = getProperty("password", dataSourceName) != null ? getProperty("password", dataSourceName) : "";
            hikariConfig.setPassword(password);
            final String driverClassName = getProperty("driver-class-name", dataSourceName) != null ? getProperty("driver-class-name", dataSourceName) : null;
            if (StringUtils.isNotEmpty(driverClassName)) {
                hikariConfig.setDriverClassName(driverClassName);
            }
            final String autoCommit = getProperty("auto-commit", dataSourceName);
            if (StringUtils.isEmpty(autoCommit)) {
                hikariConfig.setAutoCommit(false);
            } else {
                hikariConfig.setAutoCommit(BooleanUtils.toBoolean(autoCommit));
            }
            final String connectionTestQuery = getProperty("connection-test-query", dataSourceName);
            if (StringUtils.isNotEmpty(connectionTestQuery)) {
                hikariConfig.setConnectionTestQuery(connectionTestQuery);
            }
            hikariConfig.addDataSourceProperty("cachePrepStmts", getProperty("cache-prep-stmts", dataSourceName));
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", getProperty("prep-stmt-cache-size", dataSourceName));
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", getProperty("prep-stmt-cache-sql-limit", dataSourceName));
            final HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
            hikariDataSource.setMaximumPoolSize(Integer.parseInt(getProperty("maximum-pool-size", dataSourceName)));
            hikariDataSource.setMaxLifetime(Integer.parseInt(getProperty("max-life-time", dataSourceName)));
            hikariDataSource.setMaximumPoolSize(Integer.parseInt(getProperty("idle-timeout", dataSourceName)));
            applicationContext.getBeanFactory().registerSingleton(dataSourceName, hikariDataSource);

        }
        return true;
    }
}
