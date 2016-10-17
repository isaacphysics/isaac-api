/**
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

import static com.google.common.collect.Maps.immutableEntry;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_RESULTS_LIMIT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SCHOOLS_SEARCH_INDEX;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SCHOOLS_SEARCH_TYPE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SCHOOL_URN_FIELDNAME;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dos.users.School;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchOperationException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Class responsible for reading the local school list csv file.
 * 
 * This class is threadsafe providing that the ISearchProvider given as a dependency is not given to another instance of
 * this class. Normally this class should be treated as a singleton to ensure the ISearchProvider is not shared with
 * another instance of this class.
 */
public class SchoolListReader {
    private static final Logger log = LoggerFactory.getLogger(SchoolListReader.class);

    private final String fileToLoad;
    private final ISearchProvider searchProvider;

    private final ObjectMapper mapper = new ObjectMapper();

    private final Date dataSourceModificationDate;
    
    /**
     * SchoolListReader constructor.
     * 
     * @param filename
     *            - csv file containing the list of schools.
     * @param searchProvider
     *            - search provider that can be used to put and retrieve school data.
     */
    @Inject
    public SchoolListReader(@Named(Constants.SCHOOL_CSV_LIST_PATH) final String filename,
            final ISearchProvider searchProvider) {
        this.fileToLoad = filename;
        this.searchProvider = searchProvider;
        

        File dataSource = new File(fileToLoad);
        dataSourceModificationDate = new Date(dataSource.lastModified());
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
    public List<School> findSchoolByNameOrPostCode(final String searchQuery) throws UnableToIndexSchoolsException {
        if (!this.ensureSchoolList()) {
            log.error("Unable to ensure school search cache.");
            throw new UnableToIndexSchoolsException("unable to ensure the cache has been populated");
        }

        List<String> schoolSearchResults = searchProvider.fuzzySearch(SCHOOLS_SEARCH_INDEX, SCHOOLS_SEARCH_TYPE,
                searchQuery, 0, DEFAULT_RESULTS_LIMIT, null, Constants.SCHOOL_URN_FIELDNAME_POJO,
                Constants.SCHOOL_ESTABLISHMENT_NAME_FIELDNAME_POJO, Constants.SCHOOL_POSTCODE_FIELDNAME_POJO)
                .getResults();

        List<School> resultList = Lists.newArrayList();
        for (String schoolString : schoolSearchResults) {
            try {
                resultList.add(mapper.readValue(schoolString, School.class));
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
    public School findSchoolById(final Long schoolURN) throws UnableToIndexSchoolsException, JsonParseException,
            JsonMappingException, IOException {

        if (!this.ensureSchoolList()) {
            log.error("Unable to ensure school search cache.");
            throw new UnableToIndexSchoolsException("unable to ensure the cache has been populated");
        }

        List<String> matchingSchoolList;
        
        matchingSchoolList = searchProvider.findByPrefix(SCHOOLS_SEARCH_INDEX, SCHOOLS_SEARCH_TYPE,
                SCHOOL_URN_FIELDNAME.toLowerCase() + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
                schoolURN.toString(), 0, DEFAULT_RESULTS_LIMIT).getResults();

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
     * @throws UnableToIndexSchoolsException
     *             - If there is a problem indexing.
     */
    private boolean ensureSchoolList() throws UnableToIndexSchoolsException {
        return searchProvider.hasIndex(SCHOOLS_SEARCH_INDEX);
    }


    /**
     * Loads the school list from the preconfigured filename.
     * 
     * @return the list of schools.
     * @throws UnableToIndexSchoolsException
     *             - when there is a problem indexing.
     */
    private synchronized List<School> loadAndBuildSchoolList() throws UnableToIndexSchoolsException {
        // otherwise we need to generate it.
        List<School> schools = Lists.newArrayList();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileToLoad));
            String line = null;

            // use first line to determine field names.
            String[] columns = reader.readLine().split(",");

            Map<String, Integer> fieldNameMapping = new TreeMap<String, Integer>();

            for (int i = 0; i < columns.length; i++) {
                fieldNameMapping.put(columns[i].trim().replace("\"", ""), i);
            }

            // we expect the columns to have the followings:
            // SCHOOL URN | EstablishmentNumber | EstablishmentName | Town
            // Postcode
            line = reader.readLine();
            while (line != null && !line.isEmpty()) {
                // we have to remove the quotes from the string as the source
                // file is ugly.
                line = line.replace("\"", "");
                String[] schoolArray = line.split(",");
                try {
                    School schoolToSave = new School(Long.parseLong(schoolArray[fieldNameMapping
                            .get(Constants.SCHOOL_URN_FIELDNAME)]),
                            schoolArray[fieldNameMapping.get(Constants.SCHOOL_ESTABLISHMENT_NUMBER_FIELDNAME)],
                            schoolArray[fieldNameMapping.get(Constants.SCHOOL_ESTABLISHMENT_NAME_FIELDNAME)], null,
                            School.SchoolDataSource.GOVERNMENT);

                    // check if school has a post code as some of them do not.
                    if (schoolArray.length - 1 == fieldNameMapping.get(Constants.SCHOOL_POSTCODE_FIELDNAME)) {
                        schoolToSave
                                .setPostcode(schoolArray[fieldNameMapping.get(Constants.SCHOOL_POSTCODE_FIELDNAME)]);
                    }

                    schools.add(schoolToSave);
                } catch (IndexOutOfBoundsException e) {
                    // this happens when the school does not have the required
                    // data
                    log.warn("Unable to load the following school into the school list due to missing required fields. "
                            + line);
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (FileNotFoundException e) {
            log.error("Unable to locate the file requested", e);
            throw new UnableToIndexSchoolsException("Unable to locate the file requested", e);
        } catch (IOException e) {
            throw new UnableToIndexSchoolsException("Unable to load the file requested", e);
        }

        return schools;
    }
    
    /**
     * Method to help determine freshness of data.
     * @return date when the data source was last modified.
     */
    public Date getDataLastModifiedDate() {
        
        return this.dataSourceModificationDate;
    }
}
