package uk.ac.cam.cl.dtg.segue.etl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.isaac.dos.users.School;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.google.common.collect.Maps.*;

import static uk.ac.cam.cl.dtg.segue.api.Constants.SCHOOLS_INDEX_BASE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SCHOOLS_INDEX_TYPE;

/**
 * Created by Ian on 17/10/2016.
 */
class SchoolIndexer {
    private static final Logger log = LoggerFactory.getLogger(SchoolIndexer.class);
    private ElasticSearchIndexer es;
    private ContentMapper mapper;
    private String schoolsListPath;

    SchoolIndexer(ElasticSearchIndexer es, ContentMapper mapper, String schoolsListPath) {
        this.es = es;
        this.mapper = mapper;
        this.schoolsListPath = schoolsListPath;
    }

    /**
     * Build the index for the search schools provider.
     *
     * @throws UnableToIndexSchoolsException
     *             - when there is a problem building the index of schools.
     */
    synchronized void indexSchoolsWithSearchProvider() throws UnableToIndexSchoolsException {
        if (es.hasIndex(SCHOOLS_INDEX_BASE, SCHOOLS_INDEX_TYPE.SCHOOL_SEARCH.toString())) {
            log.info("Schools index already exists");
            return;
        }

        log.info("Creating schools index with search provider.");
        List<School> schoolList = this.loadAndBuildSchoolList();
        List<Map.Entry<String, String>> indexList = Lists.newArrayList();
        ObjectMapper objectMapper = mapper.getSharedContentObjectMapper();

        for (School school : schoolList) {
            try {
                indexList.add(immutableEntry(school.getUrn(), objectMapper.writeValueAsString(school)));
            } catch (JsonProcessingException e) {
                log.error("Unable to serialize the school object into json.", e);
            }
        }

        File f = new File(schoolsListPath);
        try {
            es.indexObject(SCHOOLS_INDEX_BASE, SCHOOLS_INDEX_TYPE.METADATA.toString(), objectMapper.writeValueAsString(
                    ImmutableMap.of("lastModified", f.lastModified())), "sourceFile");
        } catch (SegueSearchException e) {
            log.error("Unable to index school list metadata.", e);
        } catch (JsonProcessingException e) {
            log.error("Unable to serialise school list last modified date to JSON.", e);
        }

        try {
            es.bulkIndexWithIDs(SCHOOLS_INDEX_BASE, SCHOOLS_INDEX_TYPE.SCHOOL_SEARCH.toString(), indexList);
            log.info("School list index request complete.");
        } catch (SegueSearchException e) {
            log.error("Unable to complete bulk index operation for schools list.", e);
        }

        // Create an alias (could be anything) to prevent this schools index from being garbage-collected by ElasticSearchIndexer.expungeOldIndices
        List<String> allSchoolTypes = Arrays.stream(SCHOOLS_INDEX_TYPE.values())
                .map((schoolIndexType) -> schoolIndexType.toString()).collect(Collectors.toList());
        es.addOrMoveIndexAlias("schools-latest", SCHOOLS_INDEX_BASE, allSchoolTypes);
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
        List<School> schools = com.google.api.client.util.Lists.newArrayList();

        try (FileInputStream fs = new FileInputStream(schoolsListPath);
             InputStreamReader is = new InputStreamReader(fs, StandardCharsets.UTF_8);
             CSVReader reader = new CSVReader(is);
        ) {


            // use first line to determine field names.
            String[] columns = reader.readNext();

            Map<String, Integer> fieldNameMapping = new TreeMap<String, Integer>();

            for (int i = 0; i < columns.length; i++) {
                fieldNameMapping.put(columns[i].trim().replace("\"", ""), i);
            }

            // We expect the columns to have the following names/structure and be UTF-8 encoded:
            // URN | EstablishmentName | Postcode | Closed | DataSource
            String[] schoolArray;
            while ((schoolArray = reader.readNext()) != null) {
                try {
                    School.SchoolDataSource source = School.SchoolDataSource
                            .valueOf(schoolArray[fieldNameMapping.get(Constants.SCHOOL_DATA_SOURCE_FIELDNAME)]);

                    School schoolToSave = new School(schoolArray[fieldNameMapping.get(Constants.SCHOOL_URN_FIELDNAME)],
                            schoolArray[fieldNameMapping.get(Constants.SCHOOL_ESTABLISHMENT_NAME_FIELDNAME)],
                            schoolArray[fieldNameMapping.get(Constants.SCHOOL_POSTCODE_FIELDNAME)],
                            // CSV file contains string "t" and "f" values to denote true and false, but need a boolean:
                            "t".equals(schoolArray[fieldNameMapping.get(Constants.SCHOOL_CLOSED_FIELDNAME)]),
                            source);

                    if (null == schoolToSave.getPostcode() || schoolToSave.getPostcode().isEmpty()) {
                        log.warn("School with missing postcode! URN:" + schoolToSave.getUrn());
                    }

                    schools.add(schoolToSave);
                } catch (IndexOutOfBoundsException e) {
                    // This happens when the school does not have the required data
                    log.warn("Unable to load the following school into the school list due to missing required fields. "
                            + Arrays.toString(schoolArray));
                }
            }
        } catch (FileNotFoundException e) {
            log.error("Unable to locate the file requested", e);
            throw new UnableToIndexSchoolsException("Unable to locate the file requested", e);
        } catch (IOException e) {
            throw new UnableToIndexSchoolsException("Unable to load the file requested", e);
        } catch (CsvValidationException e) {
            throw new UnableToIndexSchoolsException("Unable to parse the file requested", e);
        }

        return schools;
    }

}
