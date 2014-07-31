package uk.ac.cam.cl.dtg.segue.dao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import uk.ac.cam.cl.dtg.segue.api.Constants.BooleanOperator;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;

/**
 * Implementation that specifically works with MongoDB Content objects.
 * 
 */
public class MongoContentManager implements IContentManager {

	private final DB database;
	private final ContentMapper mapper;

	@Inject
	public MongoContentManager(DB database, ContentMapper mapper) {
		this.database = database;
		this.mapper = mapper;
	}

	@Override
	public <T extends Content> String save(T objectToSave)
			throws IllegalArgumentException {
		JacksonDBCollection<T, String> jc = JacksonDBCollection.wrap(
				database.getCollection("content"),
				getContentSubclass(objectToSave), String.class);
		WriteResult<T, String> r = jc.save(objectToSave);
		return r.getSavedId().toString();
	}

	@Override
	public Content getById(String id, String version)
			throws IllegalArgumentException {
		if (null == id) {
			return null;
		}

		// version parameter is unused in this particular implementation
		DBCollection dbCollection = database.getCollection("content");

		// Do database query using plain mongodb so we only have to read from
		// the database once.
		DBObject node = dbCollection.findOne(new BasicDBObject("id", id));

		Content c = mapper.mapDBOjectToContentDO(node);

		return c;
	}

	@Override
	public List<String> listAvailableVersions()
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException(
				"MongoDB Content Manager does not support this operation.");
	}

	@SuppressWarnings("unchecked")
	private <T extends Content> Class<T> getContentSubclass(T obj)
			throws IllegalArgumentException {
		if (obj instanceof Content)
			return (Class<T>) obj.getClass();

		throw new IllegalArgumentException("object is not a subtype of Content");
	}

	@Override
	public ByteArrayOutputStream getFileBytes(String version, String filename)
			throws IOException {
		throw new UnsupportedOperationException(
				"This method is not implemented yet.");
	}

	@Override
	public String getLatestVersionId() throws UnsupportedOperationException {
		throw new UnsupportedOperationException(
				"This method is not implemented yet.");
	}

	@Override
	public void clearCache() {
		throw new UnsupportedOperationException(
				"This method is not implemented yet.");
	}

	@Override
	public ResultsWrapper<ContentDTO> getContentByTags(String version,
			Set<String> tags) {
		throw new UnsupportedOperationException(
				"This method is not implemented yet.");
	}

	@Override
	public ResultsWrapper<ContentDTO> searchForContent(String version,
			String searchString, Map<String, List<String>> typesToInclude) {
		throw new UnsupportedOperationException(
				"This method is not implemented yet.");
	}

	@Override
	public Set<String> getTagsList(String version) {
		throw new UnsupportedOperationException(
				"This method is not implemented yet.");
	}
	
	@Override
	public Set<String> getAllUnits(String version) {
		throw new UnsupportedOperationException(
				"This method is not implemented yet.");
	}

	@Override
	public Set<String> getCachedVersionList() {
		throw new UnsupportedOperationException(
				"This method is not implemented yet.");
	}

	@Override
	public void clearCache(String version) {
		throw new UnsupportedOperationException(
				"This method is not implemented yet.");
	}

	@Override
	public boolean isValidVersion(String version) {
		throw new UnsupportedOperationException(
				"This method is not implemented yet.");
	}

	@Override
	public boolean ensureCache(String version) {
		throw new UnsupportedOperationException(
				"This method is not implemented yet.");
	}

	@Override
	public int compareTo(String version1, String version2) {
		throw new UnsupportedOperationException(
				"This method is not implemented yet.");
	}

	@Override
	public Map<Content, List<String>> getProblemMap(String version) {
		throw new UnsupportedOperationException(
				"MongoDB Content Manager does not support this operation.");
	}

	@Override
	public ResultsWrapper<ContentDTO> findByFieldNames(String version,
			Map<Entry<BooleanOperator, String>, List<String>> fieldsToMatch,
			Integer startIndex, Integer limit) {
		throw new UnsupportedOperationException(
				"MongoDB Content Manager does not support this operation.");
	}

	@Override
	public ResultsWrapper<ContentDTO> findByFieldNamesRandomOrder(String version,
			Map<Entry<BooleanOperator, String>, List<String>> fieldsToMatch,
			Integer startIndex, Integer limit) {
		throw new UnsupportedOperationException(
				"MongoDB Content Manager does not support this operation.");
	}

	@Override
	public ResultsWrapper<ContentDTO> getByIdPrefix(String idPrefix,
			String version) {
		throw new UnsupportedOperationException(
				"MongoDB Content Manager does not support this operation.");
	}

	@Override
	public ContentDTO populateContentSummaries(String version, ContentDTO contentDTO) {
		throw new UnsupportedOperationException(
				"This method is not implemented yet.");
	}
}
