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
package uk.ac.cam.cl.dtg.isaac.dao;

import static com.google.common.collect.Maps.immutableEntry;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.FAST_TRACK_QUESTION_TYPE;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.QUESTION_TYPE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;

import java.io.IOException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;
import com.google.inject.name.Named;
import ma.glasnost.orika.MapperFacade;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.api.managers.URIManager;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardCreationMethod;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardDO;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacWildcard;
import uk.ac.cam.cl.dtg.isaac.dto.GameFilter;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Sets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

/**
 * This class is responsible for managing and persisting user data.
 */
public class GameboardPersistenceManager {

	private static final Logger log = LoggerFactory.getLogger(GameboardPersistenceManager.class);
	private static final Long GAMEBOARD_TTL_MINUTES = 30L;

	private final PostgresSqlDb database;
    private final Cache<String, GameboardDO> gameboardNonPersistentStorage;
    
	private final MapperFacade mapper; // used for content object mapping.
	private final ObjectMapper objectMapper; // used for json serialisation
	
	private final IContentManager contentManager;
    private final String contentIndex;

    private final URIManager uriManager;

    /**
     * Creates a new user data manager object.
     * 
     * @param database
     *            - the database reference used for persistence.
     * @param contentManager
     *            - allows us to lookup gameboard content.
     * @param mapper
     *            - An instance of an automapper that can be used for mapping to and from GameboardDOs and DTOs.
     * @param objectMapper
     *            - An instance of an automapper that can be used for converting objects to and from json.
     * 
     * @param uriManager
     *            - so we can generate appropriate content URIs.
     */
	@Inject
    public GameboardPersistenceManager(final PostgresSqlDb database, final IContentManager contentManager,
                                       final MapperFacade mapper, final ObjectMapper objectMapper, final URIManager uriManager, @Named(CONTENT_INDEX) final String contentIndex) {
		this.database = database;
		this.mapper = mapper;
		this.contentManager = contentManager;
        this.contentIndex = contentIndex;
        this.objectMapper = objectMapper;
        this.uriManager = uriManager;		
        this.gameboardNonPersistentStorage = CacheBuilder.newBuilder()
                .expireAfterAccess(GAMEBOARD_TTL_MINUTES, TimeUnit.MINUTES).<String, GameboardDO> build();
	}

	/**
     * Find a gameboard by id.
     * 
     * @param gameboardId
     *            - the id to search for.
     * @return the gameboard or null if we can't find it..
     * @throws SegueDatabaseException  - if there is a problem accessing the database.
     */
    public GameboardDTO getGameboardById(final String gameboardId) throws SegueDatabaseException {
        return getGameboardById(gameboardId, true);
    }
    
    /**
     * getLiteGameboardById. This method will get a gameboard by id but not
     * resolve any fine grain details about the board. E.g. no question details
     * will be retrieved.
     * 
     * @param gameboardId
     *            - to retrieve.
     * @return a lightly populated gameboard.
     * @throws SegueDatabaseException
     *             - if there are problems with the database.
     */
    public GameboardDTO getLiteGameboardById(final String gameboardId) throws SegueDatabaseException {
        return getGameboardById(gameboardId, false);
    }
	
	/**
     * Keep generated gameboard in non-persistent storage.
     * 
     * This will be removed if the gameboard is saved to persistent storage.
     * 
     * @param gameboard
     *            to temporarily store.
     * @return gameboard id
     */
    public String temporarilyStoreGameboard(final GameboardDTO gameboard) {
        this.gameboardNonPersistentStorage.put(gameboard.getId(), this.convertToGameboardDO(gameboard));

        return gameboard.getId();
    }
	
	/**
	 * Save a gameboard to persistent storage.
	 * 
	 * @param gameboard
	 *            - gameboard to save
	 * @return internal database id for the saved gameboard.
	 * @throws SegueDatabaseException
	 *             - if there is a problem saving the gameboard in the database.
	 * @throws JsonProcessingException 
	 */
	public String saveGameboardToPermanentStorage(final GameboardDTO gameboard)
		throws SegueDatabaseException {
		GameboardDO gameboardToSave = mapper.map(gameboard, GameboardDO.class);
		// the mapping operation won't work for the list so we should just
		// create a new one.
		gameboardToSave.setQuestions(new ArrayList<String>());

		// Map each question into an IsaacQuestionInfo object
		for (GameboardItem c : gameboard.getQuestions()) {
			gameboardToSave.getQuestions().add(c.getId());
		}

		// This operation may not be atomic due to underlying DB. Gameboard create first then link to user view second.
		try {
            this.saveGameboard(gameboardToSave);
        } catch (JsonProcessingException e) {
            throw new SegueDatabaseException("Unable to process json while saving gameboard.", e);
        }
		
		// add the gameboard to the users myboards list.
		this.createOrUpdateUserLinkToGameboard(gameboardToSave.getOwnerUserId(), gameboardToSave.getId());

		// make sure that it is not still in temporary storage
		this.gameboardNonPersistentStorage.invalidate(gameboard.getId());

		return gameboardToSave.getId();
	}

	/**
     * Allows a gameboard title to be updated. (assumes persistently stored)
     * 
     * @param gameboard
     *            - with updated title.
     * @return new gameboard after update.
     * @throws SegueDatabaseException - if there is a problem setting the title.
     */
    public GameboardDTO updateGameboardTitle(final GameboardDTO gameboard) throws SegueDatabaseException {      
        // create a new user to gameboard connection.
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst = conn.prepareStatement("UPDATE gameboards SET title = ? WHERE id = ?;");
            pst.setString(1, gameboard.getTitle());
            pst.setString(2, gameboard.getId());

            int affectedRows = pst.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating gameboard but no rows changed");
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }

        return gameboard;
    }
	
	/**
     * Determine if the gameboard only exists in temporary storage.
     * 
     * @param gameboardIdToTest - the gameboard to check the existence of.
     * @return true if the gameboard with that id exists in permanent storage false if not.
     * @throws SegueDatabaseException if there is a database error.
     */
    public boolean isPermanentlyStored(final String gameboardIdToTest) throws SegueDatabaseException {
        boolean isAtemporaryBoard = this.gameboardNonPersistentStorage.getIfPresent(gameboardIdToTest) != null;
        boolean isAPersistentBoard = this.getGameboardById(gameboardIdToTest, false) != null;
        
        return isAPersistentBoard && !isAtemporaryBoard;
    }
	
    /**
     * Determines whether a given game board is already in a users my boards list. Only boards in persistent storage
     * should be linked to a user.
     * 
     * @param userId
     *            to check
     * @param gameboardId
     *            to look up
     * @return true if it is false if not
     * @throws SegueDatabaseException
     *             if there is a database error
     */
	public boolean isBoardLinkedToUser(final Long userId, final String gameboardId) throws SegueDatabaseException {
        if (userId == null || gameboardId == null) {
            return false;
        }

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn
                    .prepareStatement("SELECT COUNT(*) AS TOTAL "
                            + "FROM user_gameboards WHERE user_id = ? AND gameboard_id = ?;");
            pst.setLong(1, userId);
            pst.setObject(2, gameboardId);

            ResultSet results = pst.executeQuery();
            results.next();
            return results.getInt("TOTAL") == 1;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
	}
	
	/**
	 * Create a link between a user and a gameboard or update the last visited date.
	 * 
	 * @param userId
	 *            - userId to link
	 * @param gameboardId
	 *            - gameboard to link
	 * @throws SegueDatabaseException
	 *             - if there is a problem persisting the link in the database.
	 */
	public void createOrUpdateUserLinkToGameboard(final Long userId, final String gameboardId)
		throws SegueDatabaseException {	        
	        
	    // create a new user to gameboard connection.	    
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;

            if (this.isBoardLinkedToUser(userId, gameboardId)) {
                pst = conn
                        .prepareStatement(
                                "UPDATE user_gameboards SET last_visited = ? WHERE user_id = ? AND gameboard_id = ?;");
                pst.setTimestamp(1, new Timestamp(new Date().getTime()));
                pst.setLong(2, userId);
                pst.setString(3, gameboardId);

                int affectedRows = pst.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Updating user link to gameboard but no rows changed");
                }
            } else {
                pst = conn
                        .prepareStatement(
                                "INSERT INTO user_gameboards(user_id, gameboard_id, created, last_visited) VALUES (?, ?, ?, ?);");
                
                pst.setLong(1, userId);
                pst.setString(2, gameboardId);
                pst.setTimestamp(3, new Timestamp(new Date().getTime()));
                pst.setTimestamp(4, new Timestamp(new Date().getTime()));
                
                log.debug("Saving gameboard to user relationship...");
                int affectedRows = pst.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating user link to gameboard failed, no rows changed");
                }
            }            
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
	}	
	
	/**
	 * Allows a link between users and a gameboard to be destroyed.
	 * 
	 * @param userId - users id.
	 * @param gameboardId - gameboard's id
	 * @throws SegueDatabaseException - if there is an error during the delete operation.
	 */
	public void removeUserLinkToGameboard(final Long userId, final String gameboardId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("DELETE FROM user_gameboards WHERE user_id = ? AND gameboard_id = ?");
            pst.setLong(1, userId);
            pst.setString(2, gameboardId);
            
            pst.execute();
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
	}

	/**
	 * Retrieve all gameboards (without underlying Gameboard Items) for a given
	 * user.
	 * 
	 * @param user
	 *            - to search for
	 * @return gameboards as a list - note these gameboards will not have the
	 *         questions fully populated as it is expected only summary objects
	 *         are required.
	 * @throws SegueDatabaseException
	 *             - if there is an error when accessing the database.
	 */
	public List<GameboardDTO> getGameboardsByUserId(final RegisteredUserDTO user) throws SegueDatabaseException {
		// find all gameboards related to this user.
	    List<GameboardDO> listOfResults = Lists.newArrayList();
	    Map<String, Date> lastVisitedDate = Maps.newHashMap();
	    
	    try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM gameboards INNER JOIN user_gameboards"
                    + " ON gameboards.id = user_gameboards.gameboard_id WHERE user_gameboards.user_id = ?");

            pst.setLong(1, user.getId());
            
            ResultSet results = pst.executeQuery();

            while (results.next()) {
                GameboardDO gameboard = this.convertFromSQLToGameboardDO(results);
                listOfResults.add(gameboard);
                lastVisitedDate.put(gameboard.getId(), results.getTimestamp("last_visited"));
            }

        } catch (SQLException | IOException e) {
            throw new SegueDatabaseException("Unable to find assignment by id", e);
        }

		List<GameboardDTO> gameboardDTOs = this.convertToGameboardDTOs(listOfResults, false);

		// we need to augment each gameboard with its visited date.
		for (GameboardDTO gameboardDTO : gameboardDTOs) {
			gameboardDTO.setLastVisited(lastVisitedDate.get(gameboardDTO.getId()));
		}

		return gameboardDTOs;
	}
	
	/**
	 * Find the list of invalid question ids.
	 * @param gameboardDTO - to check
	 * @return a List containing the ideas of any invalid or inaccessible questions - the list will be empty if none.
	 */
	public List<String> getInvalidQuestionIdsFromGameboard(final GameboardDTO gameboardDTO) {
		GameboardDO gameboardDO = this.convertToGameboardDO(gameboardDTO);
		
		// build query the db to get full question information
		Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMap = Maps.newHashMap();

        fieldsToMap.put(
                immutableEntry(Constants.BooleanOperator.OR, Constants.ID_FIELDNAME + '.'
                        + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX), gameboardDO.getQuestions());

        fieldsToMap.put(immutableEntry(Constants.BooleanOperator.OR, Constants.TYPE_FIELDNAME),
                Arrays.asList(QUESTION_TYPE, FAST_TRACK_QUESTION_TYPE));

        // Search for questions that match the ids.       
        ResultsWrapper<ContentDTO> results;
        try {
            results = this.contentManager.findByFieldNames(this.contentIndex,
                    fieldsToMap, 0, gameboardDO.getQuestions().size());
        } catch (ContentManagerException e) {
            results = new ResultsWrapper<ContentDTO>();
            log.error("Unable to select questions for gameboard.", e);
        }
        
		List<ContentDTO> questionsForGameboard = results.getResults();

		// Map each Content object into an GameboardItem object
		Map<String, GameboardItem> gameboardReadyQuestions = Maps.newHashMap();

		for (ContentDTO c : questionsForGameboard) {
			GameboardItem questionInfo = mapper.map(c, GameboardItem.class);
			questionInfo.setUri(uriManager.generateApiUrl(c));
			gameboardReadyQuestions.put(c.getId(), questionInfo);
		}
		
		List<String> errors = Lists.newArrayList();
		
		for (String questionid : gameboardDO.getQuestions()) {
			// There is a possibility that the question cannot be found any more for some reason
			// In this case we will simply pretend it isn't there.
			GameboardItem item = gameboardReadyQuestions.get(questionid);
			if (null == item) {
				errors.add(questionid);
			}
		}
		
		return errors;
	}

	/**
	 * Attempt to improve performance of getting gameboard items in a batch.
	 * 
	 * This method will attempt to ensure that all gameboards provided have their associated
	 * gameboard items populated with meaningful titles.
	 * 
	 * @param gameboards - list of gameboards to fully augment.
	 */
	public void augmentGameboardItems(final List<GameboardDTO> gameboards) {
		Set<String> questionIds = Sets.newHashSet();
		Map<String, List<String>> gameboardToQuestionsMap = Maps.newHashMap();

		// go through all game boards working out the set of question ids.
		for (GameboardDTO game : gameboards) {
			List<String> ids = getQuestionIds(game);
			questionIds.addAll(ids);
			gameboardToQuestionsMap.put(game.getId(), ids);
		}

        if (!questionIds.isEmpty()) {
            Map<String, GameboardItem> gameboardReadyQuestions = getGameboardItemMap(Lists.newArrayList(questionIds));
            for (GameboardDTO game : gameboards) {
                ArrayList<GameboardItem> newItems = new ArrayList<GameboardItem>(); 
                for (GameboardItem oldItem : game.getQuestions()) {
                    GameboardItem newItem = gameboardReadyQuestions.get(oldItem.getId());
                    if (newItem != null) {
                        newItem.setStatusInformation(
                                oldItem.getQuestionPartsCorrect(), oldItem.getQuestionPartsIncorrect(),
                                oldItem.getQuestionPartsNotAttempted(), oldItem.getPassMark());
                        newItems.add(newItem);
                    } else {
                        log.warn("The gameboard: " + game.getId() + " has a reference to a question (" + oldItem.getId()
                                + ") that we cannot find. Removing it from the DTO.");
                    }
                }
                game.setQuestions(newItems);
            }
        } else {
            log.info("No question ids found; returning without augmenting.");
        }
    }

    /**
     * Utility method to get a map of gameboard id to list of users who are connected to it.
     * 
     * @return map of gameboard id to list of users
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    public Map<String, List<String>> getBoardToUserIdMapping() throws SegueDatabaseException {
        Map<String, List<String>> results = Maps.newHashMap();

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT gameboard_id, user_id FROM user_gameboards;");

            ResultSet sqlResults = pst.executeQuery();

            while (sqlResults.next()) {
                String gameboardId = sqlResults.getString("gameboard_id");
                String userId = sqlResults.getString("user_id");

                if (results.containsKey(gameboardId)) {
                    results.get(gameboardId).add(userId);
                } else {
                    List<String> users = Lists.newArrayList();
                    users.add(userId);
                    results.put(gameboardId, users);
                }
            }
            
        } catch (SQLException e) {
            throw new SegueDatabaseException("Unable to find assignment by id", e);
        }

        return results;
    }
	
    /**
     * Utility function to create a gameboard item from a content DTO (Should be a question page).
     * 
     * @param content
     *            to convert
     * @return the gameboard item with augmented URI.
     */
    public GameboardItem convertToGameboardItem(final ContentDTO content) {
        GameboardItem questionInfo = mapper.map(content, GameboardItem.class);
        questionInfo.setUri(uriManager.generateApiUrl(content));
        return questionInfo;
    }

    /**
     * saveGameboard.
     * @param gameboardToSave
     * @return the DO persisted.
     * @throws JsonProcessingException
     * @throws SegueDatabaseException
     */
    private GameboardDO saveGameboard(final GameboardDO gameboardToSave) 
            throws JsonProcessingException, SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn
                    .prepareStatement("INSERT INTO "
                            + "gameboards(id, title, questions, wildcard, wildcard_position, "
                            + "game_filter, owner_user_id, creation_method, creation_date)"
                            + " VALUES (?, ?, ?, ?::text::jsonb, ?, ?::text::jsonb, ?, ?, ?);");
            Array questionIds = conn.createArrayOf("varchar", gameboardToSave.getQuestions().toArray());
            
            pst.setObject(1, gameboardToSave.getId());
            pst.setString(2, gameboardToSave.getTitle());
            pst.setObject(3, questionIds);
            pst.setString(4, objectMapper.writeValueAsString(gameboardToSave.getWildCard()));
            pst.setInt(5, gameboardToSave.getWildCardPosition());
            pst.setString(6, objectMapper.writeValueAsString(gameboardToSave.getGameFilter()));
            pst.setLong(7, gameboardToSave.getOwnerUserId());
            pst.setString(8, gameboardToSave.getCreationMethod().toString());            
            
            if (gameboardToSave.getCreationDate() != null) {
                pst.setTimestamp(9, new java.sql.Timestamp(gameboardToSave.getCreationDate().getTime()));
            } else {
                pst.setTimestamp(9, new java.sql.Timestamp(new Date().getTime()));
            }

            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save assignment.");
            }
            
            log.debug("Saving gameboard... Gameboard ID: " + gameboardToSave.getId());
            
            return gameboardToSave;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }        
    }
    
	/**
	 * Convert form a list of gameboard DOs to a list of Gameboard DTOs.
	 * 
	 * @param gameboardDOs
	 *            to convert
	 * @param populateGameboardItems
	 *            - true if we should fully populate the gameboard DTO with
	 *            gameboard items false if a summary is ok do? i.e. should game board items have titles etc.
	 * @return gameboard DTO
	 */
	private List<GameboardDTO> convertToGameboardDTOs(final List<GameboardDO> gameboardDOs,
            final boolean populateGameboardItems) {
		Validate.notNull(gameboardDOs);

		List<GameboardDTO> gameboardDTOs = Lists.newArrayList();

		for (GameboardDO gameboardDO : gameboardDOs) {
			gameboardDTOs.add(this.convertToGameboardDTO(gameboardDO, populateGameboardItems));
		}

		return gameboardDTOs;
	}

	/**
	 * Convert form a gameboard DO to a Gameboard DTO.
	 * 
	 * This method relies on the api to fully resolve questions.
	 * 
	 * @param gameboardDO
	 *            - to convert
	 * @return gameboard DTO
	 */
	private GameboardDTO convertToGameboardDTO(final GameboardDO gameboardDO) {
		return this.convertToGameboardDTO(gameboardDO, true);
	}

	/**
	 * Convert form a gameboard DO to a Gameboard DTO.
	 * 
	 * This method relies on the api to fully resolve questions.
	 * 
	 * @param gameboardDO
	 *            - to convert
	 * @param populateGameboardItems
	 *            - true if we should fully populate the gameboard DTO with
	 *            gameboard items false if just the question ids will do?
	 * @return gameboard DTO
	 */
    private GameboardDTO convertToGameboardDTO(final GameboardDO gameboardDO, final boolean populateGameboardItems) {
		GameboardDTO gameboardDTO = mapper.map(gameboardDO, GameboardDTO.class);

		if (!populateGameboardItems) {
			List<GameboardItem> listOfSparseGameItems = Lists.newArrayList();

			for (String questionPageId : gameboardDO.getQuestions()) {
				GameboardItem gameboardItem = new GameboardItem();
				gameboardItem.setId(questionPageId);
				listOfSparseGameItems.add(gameboardItem);
			}
			gameboardDTO.setQuestions(listOfSparseGameItems);
			return gameboardDTO;
		}

		// Map each Content object into an GameboardItem object
		Map<String, GameboardItem> gameboardReadyQuestions = getGameboardItemMap(gameboardDO.getQuestions());

		// empty and repopulate the gameboard dto.
		gameboardDTO.setQuestions(new ArrayList<GameboardItem>());
		for (String questionid : gameboardDO.getQuestions()) {
			// There is a possibility that the question cannot be found any more for some reason
			// In this case we will simply pretend it isn't there.
			GameboardItem item = gameboardReadyQuestions.get(questionid);
			if (item != null) {
				gameboardDTO.getQuestions().add(item);	
			} else {
                log.warn("The gameboard: " + gameboardDTO.getId() + " has a reference to a question (" + questionid
                        + ") that we cannot find. Removing it from the DTO.");
			}
		}
		return gameboardDTO;
	}

	/**
	 * Convert from a gameboard DTO to a gameboard DO.
	 * 
	 * @param gameboardDTO
	 *            - DTO to convert.
	 * @return GameboardDO.
	 */
	private GameboardDO convertToGameboardDO(final GameboardDTO gameboardDTO) {
		GameboardDO gameboardDO = mapper.map(gameboardDTO, GameboardDO.class);
		// the mapping operation won't work for the list so we should just
		// create a new one.
		gameboardDO.setQuestions(new ArrayList<String>());

		// Map each question into an IsaacQuestionInfo object
		for (GameboardItem c : gameboardDTO.getQuestions()) {
			gameboardDO.getQuestions().add(c.getId());
		}

		return gameboardDO;
	}

	/**
	 * Utility method to allow all gameboard related questions to be retrieved in one big batch.
	 * 
	 * @param questionIds to populate.
	 * @return a map of question id to fully populated gameboard item.
	 */
	private Map<String, GameboardItem> getGameboardItemMap(final List<String> questionIds) {
		// build query the db to get full question information
		Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMap = Maps.newHashMap();

		fieldsToMap.put(
                immutableEntry(Constants.BooleanOperator.OR, Constants.ID_FIELDNAME + '.'
						+ Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX), questionIds);

		fieldsToMap.put(immutableEntry(Constants.BooleanOperator.OR, Constants.TYPE_FIELDNAME),
                Arrays.asList(QUESTION_TYPE, FAST_TRACK_QUESTION_TYPE));

		// Search for questions that match the ids.
		ResultsWrapper<ContentDTO> results;
        try {
            results = this.contentManager.findByFieldNames(this.contentIndex,
                    fieldsToMap, 0, questionIds.size());
        } catch (ContentManagerException e) {
            results = new ResultsWrapper<ContentDTO>();
            log.error("Unable to locate questions for gameboard. Using empty results", e);
        }
        
	    Map<String, GameboardItem> gameboardReadyQuestions = Maps.newHashMap();
		
	    if (null == results) {
	        return gameboardReadyQuestions;
		}
		
	    List<ContentDTO> questionsForGameboard = results.getResults();

		// Map each Content object into an GameboardItem object
		for (ContentDTO c : questionsForGameboard) {
			GameboardItem questionInfo = this.convertToGameboardItem(c);
			gameboardReadyQuestions.put(c.getId(), questionInfo);
		}
		
		return gameboardReadyQuestions;
	}
	
    /**
     * Utility method to allow us to retrieve a gameboard either from temporary storage or permanent.
     * 
     * @param gameboardId
     *            - gameboard to find
     * @param fullyPopulate
     *            - true or false
     * @return gameboard dto or null if we cannot find the gameboard requested
     * @throws SegueDatabaseException
     *             - if there is a problem with the database
     */
    private GameboardDTO getGameboardById(final String gameboardId, final boolean fullyPopulate)
            throws SegueDatabaseException {
        if (null == gameboardId) {
            return null;
        }
        
        // first try temporary storage
        if (this.gameboardNonPersistentStorage.getIfPresent(gameboardId) != null) {
            return this.convertToGameboardDTO(this.gameboardNonPersistentStorage.getIfPresent(gameboardId));
        }

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM gameboards WHERE id = ?;");

            pst.setString(1, gameboardId);
            
            ResultSet results = pst.executeQuery();
 
            List<GameboardDO> listOfResults = Lists.newArrayList();
            while (results.next()) {
                listOfResults.add(this.convertFromSQLToGameboardDO(results));
            }

            if (listOfResults.size() == 0) {
                return null;
            }
            
            if (listOfResults.size() > 1) {
                throw new SegueDatabaseException("Ambiguous result, expected single result and found more than one"
                        + listOfResults);
            }          

            return this.convertToGameboardDTO(listOfResults.get(0), fullyPopulate);

        } catch (SQLException | IOException e) {
            throw new SegueDatabaseException("Unable to find assignment by id", e);
        }      
    }

    /**
     * @param results - the results from sql.
     * @return a gameboard DO.
     * @throws SQLException
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    private GameboardDO convertFromSQLToGameboardDO(final ResultSet results) throws SQLException, JsonParseException,
            JsonMappingException, IOException {
        GameboardDO gameboardDO = new GameboardDO();
        gameboardDO.setId(results.getString("id"));
        gameboardDO.setTitle(results.getString("title"));
        gameboardDO.setQuestions(Arrays.asList((String[]) results.getArray("questions").getArray()));
        gameboardDO.setWildCard(objectMapper.readValue(results.getObject("wildcard").toString(), IsaacWildcard.class));
        gameboardDO.setWildCardPosition(results.getInt("wildcard_position"));
        gameboardDO.setGameFilter(objectMapper
                .readValue(results.getObject("game_filter").toString(), GameFilter.class));
        gameboardDO.setOwnerUserId(results.getLong("owner_user_id"));
        
        if (results.getString("creation_method") != null) {
            gameboardDO.setCreationMethod(GameboardCreationMethod.valueOf(results.getString("creation_method")));    
        }
        
        gameboardDO.setCreationDate(new Date(results.getTimestamp("creation_date").getTime()));
        return gameboardDO;
    }

    /**
     * Helper method to get a list of question ids from a dto.
     * 
     * @param gameboardDTO
     *            - to extract.
     * @return List of question ids for the gameboard provided.
     */
    private static List<String> getQuestionIds(final GameboardDTO gameboardDTO) {
        List<String> listOfQuestionIds = Lists.newArrayList();

        if (gameboardDTO.getQuestions() == null || gameboardDTO.getQuestions().isEmpty()) {
            return listOfQuestionIds;
        }

        for (GameboardItem gameItem : gameboardDTO.getQuestions()) {
            if (gameItem.getId() == null || gameItem.getId().isEmpty()) {
                continue;
            }
            listOfQuestionIds.add(gameItem.getId());
        }
        return listOfQuestionIds;
    }
}
