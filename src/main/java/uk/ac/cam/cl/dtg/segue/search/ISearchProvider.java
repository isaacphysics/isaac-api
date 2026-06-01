/*
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.search;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.api.Constants;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Map;

/**
 * Interface describing behaviour of search providers.
 */
public interface ISearchProvider {

    public String getNestedFieldConnector();

    /**
     * Verifies the existence of a given index.
     * 
     * @param indexBase
     *            to verify
     * @param indexType
     *            to verify
     * @return true if the index exists false if not.
     */
    boolean hasIndex(final String indexBase, final String indexType);
    
    /**
     * @return the list of all indices.
     */
    Collection<String> getAllIndices();

    ResultsWrapper<String> nestedMatchSearch(
            final String indexBase, final String indexType, final Integer startIndex, final Integer limit,
            @NotNull final BooleanInstruction matchInstruction, @Nullable Long randomSeed,
            @Nullable final Map<String, Constants.SortOrder> sortOrder
    ) throws SegueSearchException;

    /*
     * TODO: We need to change the return type of these two methods to avoid having ES specific things
     */
    GetResponse<ObjectNode> getById(String indexBase, String indexType, String id) throws SegueSearchException;

    SearchResponse<ObjectNode> getAllFromIndex(String indexBase, String indexType) throws SegueSearchException;
}
