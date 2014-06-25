package uk.ac.cam.cl.dtg.isaac.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

import uk.ac.cam.cl.dtg.isaac.configuration.IsaacGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.isaac.models.content.Gameboard;
import uk.ac.cam.cl.dtg.isaac.models.content.GameboardItem;
import uk.ac.cam.cl.dtg.isaac.models.content.IsaacQuestionInfo;
import uk.ac.cam.cl.dtg.isaac.models.content.Wildcard;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.api.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.Content;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.isaac.app.Constants.*;

public class GameManager {
	private static final Logger log = LoggerFactory.getLogger(GameManager.class);
	private final SegueApiFacade api;
	
	public GameManager(SegueApiFacade api){
		this.api = api;
	}
	
	public Gameboard generateRandomGameboard(){
		return this.generateRandomGameboard(null, null, null, null, null);
	}

	/**
	 * This method expects only one of its 3 subject tag filter parameters to have more than one element due to restrictions on the question filter interface.
	 * 
	 * @param subjectsList
	 * @param fieldsList
	 * @param topicsList
	 * @param levelsList
	 * @param conceptsList
	 * @return a gameboard if possible that satisifies the conditions provided by the parameters.
	 */
	public Gameboard generateRandomGameboard(List<String> subjectsList, List<String> fieldsList, List<String> topicsList, List<String> levelsList, List<String> conceptsList){
		
		Map<Map.Entry<Constants.BooleanOperator,String>, List<String>> fieldsToMap = new HashMap<Map.Entry<Constants.BooleanOperator,String>, List<String>>();
		fieldsToMap.put(com.google.common.collect.Maps.immutableEntry(Constants.BooleanOperator.AND, TYPE_FIELDNAME), Arrays.asList(QUESTION_TYPE));
		
		fieldsToMap.putAll(IsaacController.generateFieldToMatchForQuestionFilter(subjectsList, fieldsList, topicsList, levelsList, conceptsList));
		
		// Search for questions that match the fields to map variable.//TODO: fix magic numbers
		ResultsWrapper<Content> results = api.findMatchingContentRandomOrder(api.getLiveVersion(), fieldsToMap, 0, 20); 
		
		if(!results.getResults().isEmpty()){
			String uuid = UUID.randomUUID().toString();
			
			Integer sizeOfGameboard = GAME_BOARD_SIZE;
			// TODO: if there are not enough questions then really we should fill it with random questions or just say no
			if(GAME_BOARD_SIZE > results.getResults().size()){
				sizeOfGameboard = results.getResults().size();
			}
			
			List<Content> questionsForGameboard = results.getResults().subList(0, sizeOfGameboard);
			
			// build gameboard
			Injector injector = Guice.createInjector(new IsaacGuiceConfigurationModule(), new SegueGuiceConfigurationModule());
			Mapper mapper = injector.getInstance(Mapper.class);
			List<GameboardItem> gameboardReadyQuestions = new ArrayList<GameboardItem>();
			
			// Map each Content object into an IsaacQuestionInfo object
			for(Content c : questionsForGameboard){
				IsaacQuestionInfo questionInfo = mapper.map(c, IsaacQuestionInfo.class);
				questionInfo.setUri(IsaacController.generateApiUrl(c));
				gameboardReadyQuestions.add(questionInfo);
			}

			log.debug("Created gameboard " + uuid);
			return new Gameboard(uuid, gameboardReadyQuestions, new Date());			
		}
		else{
			return new Gameboard();
		}
	}
	
	public boolean storeGameboard(Gameboard gameboardToStore){
		return false;
	}
	
	public Wildcard getRandomWildcardTile(){
		return null;
	}
}
