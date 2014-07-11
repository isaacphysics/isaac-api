package uk.ac.cam.cl.dtg.segue.quiz;

import java.util.List;

import uk.ac.cam.cl.dtg.segue.dos.content.Question;

public interface IMultiFieldQuestion {
	public List<? extends Question> getFields();
}
