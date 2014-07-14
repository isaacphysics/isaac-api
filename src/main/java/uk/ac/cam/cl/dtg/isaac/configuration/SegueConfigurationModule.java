package uk.ac.cam.cl.dtg.isaac.configuration;

import java.util.List;
import com.google.api.client.util.Lists;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacFeaturedProfile;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacMultiChoiceQuestion;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestion;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionPage;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacSymbolicQuestion;
import uk.ac.cam.cl.dtg.segue.configuration.ISegueDTOConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;

/**
 * Segue Configuration module for Isaac. 
 * 
 * This is used to register isaac specific DOs and DTOs with Segue.
 */
public class SegueConfigurationModule implements ISegueDTOConfigurationModule {

	@Override
	public List<Class<? extends Content>> getContentDataTransferObjectMap() {
		List<Class<? extends Content>> supplementaryContentDTOs = Lists.newArrayList();

		supplementaryContentDTOs.add(IsaacQuestion.class);
		supplementaryContentDTOs.add(IsaacMultiChoiceQuestion.class);
		supplementaryContentDTOs.add(IsaacNumericQuestion.class);
		supplementaryContentDTOs.add(IsaacSymbolicQuestion.class);
		supplementaryContentDTOs.add(IsaacQuestionPage.class);
		supplementaryContentDTOs.add(IsaacFeaturedProfile.class);

		return supplementaryContentDTOs;
	}

}
