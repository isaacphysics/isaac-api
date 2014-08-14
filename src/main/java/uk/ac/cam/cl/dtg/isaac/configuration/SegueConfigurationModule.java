package uk.ac.cam.cl.dtg.isaac.configuration;

import java.util.List;

import com.google.api.client.util.Lists;

import uk.ac.cam.cl.dtg.isaac.dos.IsaacConceptPage;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacFeaturedProfile;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacMultiChoiceQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuestionBase;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuestionPage;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuickQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacWildcard;
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
		
		supplementaryContentDTOs.add(IsaacQuickQuestion.class);
		supplementaryContentDTOs.add(IsaacQuestionBase.class);
		supplementaryContentDTOs.add(IsaacMultiChoiceQuestion.class);
		supplementaryContentDTOs.add(IsaacNumericQuestion.class);
		supplementaryContentDTOs.add(IsaacSymbolicQuestion.class);
		supplementaryContentDTOs.add(IsaacQuestionPage.class);
		supplementaryContentDTOs.add(IsaacConceptPage.class);
		supplementaryContentDTOs.add(IsaacFeaturedProfile.class);
		supplementaryContentDTOs.add(IsaacWildcard.class);

		return supplementaryContentDTOs;
	}

}
