package uk.ac.cam.cl.dtg.isaac.configuration;

import java.util.HashMap;
import java.util.Map;

import uk.ac.cam.cl.dtg.isaac.models.content.IsaacFeaturedProfile;
import uk.ac.cam.cl.dtg.isaac.models.content.IsaacMultiChoiceQuestion;
import uk.ac.cam.cl.dtg.isaac.models.content.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.models.content.IsaacQuestion;
import uk.ac.cam.cl.dtg.isaac.models.content.IsaacSymbolicQuestion;
import uk.ac.cam.cl.dtg.isaac.models.pages.IsaacQuestionPage;
import uk.ac.cam.cl.dtg.segue.api.ISegueDTOConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dto.content.Content;

public class SegueConfigurationModule implements ISegueDTOConfigurationModule {

	@Override
	public Map<String, Class<? extends Content>> getContentDataTransferObjectMap() {
		Map<String, Class<? extends Content>> supplementaryContentDTOs = new HashMap<String, Class<? extends Content>>();
		
		supplementaryContentDTOs.put("isaacQuestion", IsaacQuestion.class);
		supplementaryContentDTOs.put("isaacMultiChoiceQuestion", IsaacMultiChoiceQuestion.class);
		supplementaryContentDTOs.put("isaacNumericQuestion", IsaacNumericQuestion.class);
		supplementaryContentDTOs.put("isaacSymbolicQuestion", IsaacSymbolicQuestion.class);
		supplementaryContentDTOs.put("isaacQuestionPage", IsaacQuestionPage.class);
		supplementaryContentDTOs.put("isaacFeaturedProfile", IsaacFeaturedProfile.class);
		
		return supplementaryContentDTOs;
	}

}
