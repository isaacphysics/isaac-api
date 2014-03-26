package uk.ac.cam.cl.dtg.isaac.configuration;

import java.util.HashMap;
import java.util.Map;

import uk.ac.cam.cl.dtg.isaac.models.IsaacMultiChoiceQuestion;
import uk.ac.cam.cl.dtg.isaac.models.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.models.IsaacQuestion;
import uk.ac.cam.cl.dtg.isaac.models.IsaacQuestionPage;
import uk.ac.cam.cl.dtg.isaac.models.IsaacSymbolicQuestion;
import uk.ac.cam.cl.dtg.segue.api.ISegueConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dto.Content;

public class SegueConfigurationModule implements ISegueConfigurationModule {

	@Override
	public Map<String, Class<? extends Content>> getContentDataTransferObjectMap() {
		Map<String, Class<? extends Content>> supplementaryContentDTOs = new HashMap<String, Class<? extends Content>>();
		
		supplementaryContentDTOs.put("isaacQuestion", IsaacQuestion.class);
		supplementaryContentDTOs.put("isaacMultiChoiceQuestion", IsaacMultiChoiceQuestion.class);
		supplementaryContentDTOs.put("isaacNumericQuestion", IsaacNumericQuestion.class);
		supplementaryContentDTOs.put("isaacSymbolicQuestion", IsaacSymbolicQuestion.class);
		supplementaryContentDTOs.put("isaacQuestionPage", IsaacQuestionPage.class);
		
		return supplementaryContentDTOs;
	}

}
