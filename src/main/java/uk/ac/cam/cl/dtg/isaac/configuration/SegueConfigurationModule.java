package uk.ac.cam.cl.dtg.isaac.configuration;

import java.util.HashMap;
import java.util.Map;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacFeaturedProfile;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacMultiChoiceQuestion;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestion;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionPage;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacSymbolicQuestion;
import uk.ac.cam.cl.dtg.segue.configuration.ISegueDTOConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;

public class SegueConfigurationModule implements ISegueDTOConfigurationModule {

	@Override
	public Map<String, Class<? extends Content>> getContentDataTransferObjectMap() {
		Map<String, Class<? extends Content>> supplementaryContentDTOs = new HashMap<String, Class<? extends Content>>();

		supplementaryContentDTOs.put("isaacQuestion", IsaacQuestion.class);
		supplementaryContentDTOs.put("isaacMultiChoiceQuestion",
				IsaacMultiChoiceQuestion.class);
		supplementaryContentDTOs.put("isaacNumericQuestion",
				IsaacNumericQuestion.class);
		supplementaryContentDTOs.put("isaacSymbolicQuestion",
				IsaacSymbolicQuestion.class);
		supplementaryContentDTOs.put("isaacQuestionPage",
				IsaacQuestionPage.class);
		supplementaryContentDTOs.put("isaacFeaturedProfile",
				IsaacFeaturedProfile.class);

		return supplementaryContentDTOs;
	}

}
