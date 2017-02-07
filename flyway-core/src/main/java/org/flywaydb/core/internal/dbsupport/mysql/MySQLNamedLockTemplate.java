/**
 * Copyright 2010-2016 Boxfuse GmbH
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flywaydb.core.internal.dbsupport.mysql;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.internal.dbsupport.FlywaySqlException;
import org.flywaydb.core.internal.dbsupport.JdbcTemplate;
import org.flywaydb.core.internal.util.logging.Log;
import org.flywaydb.core.internal.util.logging.LogFactory;

import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * Spring-like template for executing with MySQL named locks.
 */
public class MySQLNamedLockTemplate {
    private static final Log LOG = LogFactory.getLog(MySQLNamedLockTemplate.class);

    /**
     * The connection for the named lock.
     */
    private final JdbcTemplate jdbcTemplate;

    private final String lockName;

    /**
     * Creates a new named lock template for this connection.
     *
     * @param jdbcTemplate  The jdbcTemplate for the connection.
     * @param discriminator A number to discriminate between locks.
     */
    MySQLNamedLockTemplate(JdbcTemplate jdbcTemplate, int discriminator) {
        this.jdbcTemplate = jdbcTemplate;
        lockName = "Flyway-" + discriminator;
    }

    /**
     * Executes this callback with a named lock.
     *
     * @param callable The callback to execute.
     * @return The result of the callable code.
     */
    public <T> T execute(Callable<T> callable) {
        try {
            jdbcTemplate.execute("SELECT GET_LOCK('" + lockName + "',100000)");
            return callable.call();
        } catch (SQLException e) {
            throw new FlywaySqlException("Unable to acquire MySQL named lock: " + lockName, e);
        } catch (Exception e) {
            RuntimeException rethrow;
            if (e instanceof RuntimeException) {
                rethrow = (RuntimeException) e;
            } else {
                rethrow = new FlywayException(e);
            }
            throw rethrow;
        } finally {
            try {
                jdbcTemplate.execute("SELECT RELEASE_LOCK('" + lockName + "')");
            } catch (SQLException e) {
                LOG.error("Unable to release MySQL named lock: " + lockName, e);
            }
        }
    }
}
