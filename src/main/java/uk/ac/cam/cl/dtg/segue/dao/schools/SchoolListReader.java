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
package uk.ac.cam.cl.dtg.segue.dao.schools;

import co.elastic.clients.elasticsearch.core.GetResponse;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.users.School;
import uk.ac.cam.cl.dtg.segue.search.BooleanInstruction;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.segue.search.MatchInstruction;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;

import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.List;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Class responsible for reading the local school list csv file.
 * 
 * This class is threadsafe providing that the ISearchProvider given as a dependency is not given to another instance of
 * this class. Normally this class should be treated as a singleton to ensure the ISearchProvider is not shared with
 * another instance of this class.
 */
public class SchoolListReader {
    private static final Logger log = LoggerFactory.getLogger(SchoolListReader.class);

    private final ISearchProvider searchProvider;

    private final ObjectMapper mapper = new ObjectMapper();

    private final String dataSourceModificationDate;

    /**
     * SchoolListReader constructor.
     * 
     * @param searchProvider
     *            - search provider that can be used to put and retrieve school data.
     */
    @Inject
    public SchoolListReader(final ISearchProvider searchProvider) {
        this.searchProvider = searchProvider;

        String modificationDate = "unknown";
        try {
            GetResponse<ObjectNode> response = searchProvider.getById(
                    SCHOOLS_INDEX_BASE, SCHOOLS_INDEX_TYPE.METADATA.toString(), "sourceFile");
            if (null != response.source()) {
                modificationDate = response.source().get("lastModified").asText();
            }
        } catch (SegueSearchException e) {
            log.error("Failed to retrieve school list modification date", e);
        }
        dataSourceModificationDate = modificationDate;
    }

    /**
     * findSchoolByNameOrPostCode. Excludes schools marked as closed or excluded.
     *
     * @param searchQuery
     *            - school to search for - either name or postcode.
     * @param limit
     *            - the number of results to return.
     * @return list of schools matching the criteria or an empty list.
     * @throws UnableToIndexSchoolsException
     *             - if there is an error access the index of schools.
     */
    public List<School> findSchoolByNameOrPostCode(final String searchQuery, @Nullable final Integer limit) throws UnableToIndexSchoolsException, SegueSearchException {
        if (!this.ensureSchoolList()) {
            log.error("Unable to ensure school search cache.");
            throw new UnableToIndexSchoolsException("unable to ensure the cache has been populated");
        }

        Integer queryLimit = limit == null ? DEFAULT_RESULTS_LIMIT : limit;

        BooleanInstruction matchInstruction = new BooleanInstruction();
        // Exclude excluded/closed schools
        matchInstruction.must(new MatchInstruction(SCHOOL_EXCLUDED_FIELDNAME, "false", null, false));
        matchInstruction.must(new MatchInstruction(SCHOOL_CLOSED_FIELDNAME, "false", null, false));
        // Attempt to match on school ID, name & postcode
        matchInstruction.should(new MatchInstruction(SCHOOL_ID_FIELDNAME, searchQuery, null, true));
        matchInstruction.should(new MatchInstruction(SCHOOL_NAME_FIELDNAME, searchQuery, null, true));
        matchInstruction.should(new MatchInstruction(SCHOOL_POSTCODE_FIELDNAME, searchQuery, null, true));

        List<String> schoolSearchResults = searchProvider.nestedMatchSearch(SCHOOLS_INDEX_BASE,
                SCHOOLS_INDEX_TYPE.SCHOOL_SEARCH.toString(), 0, queryLimit, matchInstruction, null, null).getResults();

        List<School> resultList = Lists.newArrayList();
        for (String schoolString : schoolSearchResults) {
            try {
                resultList.add(mapper.readValue(schoolString, School.class));
            } catch (JsonParseException | JsonMappingException e) {
                log.error("Unable to parse the school '{}'", schoolString, e);
            } catch (IOException e) {
                log.error("IOException for ({})!", schoolString, e);
            }
        }
        return resultList;
    }

    /**
     * Find school by Id.
     * 
     * @param schoolId
     *            - to search for.
     * @return school.
     * @throws UnableToIndexSchoolsException
     *             - if we cannot complete the indexing process
     * @throws IOException
     *             - If we cannot read the school data
     * @throws JsonMappingException
     *             - if we cannot map to the school class.
     * @throws JsonParseException
     *             - if the school data is malformed
     */
    public School findSchoolById(final String schoolId) throws UnableToIndexSchoolsException, JsonParseException,
            JsonMappingException, IOException, SegueSearchException {

        if (!this.ensureSchoolList()) {
            log.error("Unable to ensure school search cache.");
            throw new UnableToIndexSchoolsException("unable to ensure the cache has been populated");
        }

        List<String> matchingSchoolList;
        
        matchingSchoolList = searchProvider.findByExactMatch(SCHOOLS_INDEX_BASE, SCHOOLS_INDEX_TYPE.SCHOOL_SEARCH.toString(),
                SCHOOL_ID_FIELDNAME, schoolId, 0, DEFAULT_RESULTS_LIMIT, null).getResults();

        if (matchingSchoolList.isEmpty()) {
            return null;
        }
        
        if (matchingSchoolList.size() > 1) {
            log.error("Error while looking up school up by id! More than one match for '{}' results: {}", schoolId, matchingSchoolList);
        }

        return mapper.readValue(matchingSchoolList.getFirst(), School.class);
    }


    /**
     * Ensure School List has been generated.
     * 
     * @return true if we have an index or false if not. If false we cannot guarantee a response.
     */
    private boolean ensureSchoolList() {
        return searchProvider.hasIndex(SCHOOLS_INDEX_BASE, SCHOOLS_INDEX_TYPE.SCHOOL_SEARCH.toString());
    }



    /**
     * Method to help determine freshness of data.
     * @return date when the data source was last modified.
     */
    public String getDataLastModifiedDate() {
        return this.dataSourceModificationDate;
    }
}
