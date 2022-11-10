/*
 * Copyright 2014 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.dao.schools;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.users.School;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;

import java.io.IOException;
import java.util.List;

import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_RESULTS_LIMIT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SCHOOLS_INDEX_BASE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SCHOOL_ESTABLISHMENT_NAME_FIELDNAME_POJO;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SCHOOLS_INDEX_TYPE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SCHOOL_POSTCODE_FIELDNAME_POJO;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SCHOOL_URN_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SCHOOL_URN_FIELDNAME_POJO;
import static uk.ac.cam.cl.dtg.segue.api.Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX;

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

        String modificationDate;
        try {
            modificationDate = searchProvider.getById(
                    SCHOOLS_INDEX_BASE, SCHOOLS_INDEX_TYPE.METADATA.toString(), "sourceFile").getSource().get("lastModified").toString();
        } catch (SegueSearchException e) {
            log.error("Failed to retrieve school list modification date", e);
            modificationDate = "unknown";
        }
        dataSourceModificationDate = modificationDate;
    }

    /**
     * findSchoolByNameOrPostCode.
     * 
     * @param searchQuery
     *            - school to search for - either name or postcode.
     * @return list of schools matching the criteria or an empty list.
     * @throws UnableToIndexSchoolsException
     *             - if there is an error access the index of schools.
     */
    public List<School> findSchoolByNameOrPostCode(final String searchQuery) throws UnableToIndexSchoolsException, SegueSearchException {
        if (!this.ensureSchoolList()) {
            log.error("Unable to ensure school search cache.");
            throw new UnableToIndexSchoolsException("unable to ensure the cache has been populated");
        }

        // FIXME: for one release cycle, we need backwards compatibility and so cannot use the fieldsThatMustMatch property
        // It should be set to ImmutableMap.of("closed", ImmutableList.of("false"))
        List<String> schoolSearchResults = searchProvider.fuzzySearch(SCHOOLS_INDEX_BASE, SCHOOLS_INDEX_TYPE.SCHOOL_SEARCH.toString(),
                searchQuery, 0, DEFAULT_RESULTS_LIMIT, null, null, SCHOOL_URN_FIELDNAME_POJO,
                SCHOOL_ESTABLISHMENT_NAME_FIELDNAME_POJO, SCHOOL_POSTCODE_FIELDNAME_POJO)
                .getResults();

        List<School> resultList = Lists.newArrayList();
        for (String schoolString : schoolSearchResults) {
            try {
                School school = mapper.readValue(schoolString, School.class);
                if (school.isClosed() != null && school.isClosed()) {
                    // FIXME: this filtering will be unnecessary once the above fix is implemented!
                    continue;
                }
                resultList.add(school);
            } catch (JsonParseException | JsonMappingException e) {
                log.error("Unable to parse the school " + schoolString, e);
            } catch (IOException e) {
                log.error("IOException " + schoolString, e);
            }
        }
        return resultList;
    }

    /**
     * Find school by Id.
     * 
     * @param schoolURN
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
    public School findSchoolById(final String schoolURN) throws UnableToIndexSchoolsException, JsonParseException,
            JsonMappingException, IOException, SegueSearchException {

        if (!this.ensureSchoolList()) {
            log.error("Unable to ensure school search cache.");
            throw new UnableToIndexSchoolsException("unable to ensure the cache has been populated");
        }

        List<String> matchingSchoolList;
        
        matchingSchoolList = searchProvider.findByExactMatch(SCHOOLS_INDEX_BASE, SCHOOLS_INDEX_TYPE.SCHOOL_SEARCH.toString(),
                SCHOOL_URN_FIELDNAME.toLowerCase() + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX,
                schoolURN, 0, DEFAULT_RESULTS_LIMIT, null).getResults();

        if (matchingSchoolList.isEmpty()) {
            return null;
        }
        
        if (matchingSchoolList.size() > 1) {
            log.error("Error occurred while trying to look a school up by id... Found more than one match for "
                    + schoolURN + " results: " + matchingSchoolList);
        }

        return mapper.readValue(matchingSchoolList.get(0), School.class);
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
