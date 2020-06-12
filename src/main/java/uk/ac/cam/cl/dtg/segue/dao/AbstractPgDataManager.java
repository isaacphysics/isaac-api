/*
 * Copyright 2017 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * Common abstract class for postgres DAOs.
 */
public abstract class AbstractPgDataManager {

    /**
     * Helper that picks the correct pst method based on the value provided.
     *
     * @param pst - prepared statement - already initialised
     * @param index - index of the value to be replaced in the pst
     * @param value - value
     * @throws SQLException - if there is a db problem.
     */
    protected void setValueHelper(final PreparedStatement pst, final int index, final Object value) throws SQLException {
        if (null == value) {
            pst.setNull(index, Types.NULL);
            return;
        }

        if (value.getClass().isEnum()) {
            pst.setString(index, ((Enum<?>) value).name());
        }

        if (value instanceof String) {
            pst.setString(index, (String) value);
        }

        if (value instanceof Integer) {
            pst.setInt(index, (Integer) value);
        }

        if (value instanceof Long) {
            pst.setLong(index, (Long) value);
        }

        if (value instanceof java.util.Date) {
            pst.setTimestamp(index, new Timestamp(((java.util.Date) value).getTime()));
        }
    }
}
