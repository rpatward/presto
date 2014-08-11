/*
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
package com.facebook.presto.raptor.metadata;

import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

import java.sql.SQLException;
import java.util.concurrent.Callable;

public final class SqlUtils
{
    private SqlUtils() {}

    /**
     * Run a SQL query as Runnable ignoring any constraint violations.
     * This is a HACK to allow us to support idempotent inserts on
     */
    public static void runIgnoringConstraintViolation(Runnable task)
    {
        try {
            task.run();
        }
        catch (UnableToExecuteStatementException e) {
            if (e.getCause() instanceof SQLException) {
                String state = ((SQLException) e.getCause()).getSQLState();
                if (state.startsWith("23")) {
                    return;
                }
            }
            throw e;
        }
    }

    /**
     * Run a SQL query as Runnable ignoring any constraint violations.
     * This is a HACK to allow us to support idempotent inserts on
     */
    public static <T> T runIgnoringConstraintViolation(Callable<T> task, T defaultValue)
            throws Exception
    {
        try {
            return task.call();
        }
        catch (UnableToExecuteStatementException e) {
            if (e.getCause() instanceof SQLException) {
                String state = ((SQLException) e.getCause()).getSQLState();
                if (state.startsWith("23")) {
                    return defaultValue;
                }
            }
            throw e;
        }
    }
}