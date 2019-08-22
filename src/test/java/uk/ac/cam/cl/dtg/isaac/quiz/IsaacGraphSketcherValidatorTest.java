/*
 * Copyright 2019 University of Cambridge
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
package uk.ac.cam.cl.dtg.isaac.quiz;

import com.google.api.client.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacGraphSketcherQuestion;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.GraphChoice;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for the Graph Sketcher Validator class.
 *
 */
@PowerMockIgnore({"javax.ws.*"})
public class IsaacGraphSketcherValidatorTest {
    private IsaacGraphSketcherValidator validator;

    /**
     * Initial configuration of tests.
     *
     */
    @Before
    public final void setUp() {
        validator = new IsaacGraphSketcherValidator();
    }

    /*
        Test that the "did not provide an answer" response is returned for empty input.
     */
    @Test
    public final void emptyValue_InvalidResponseShouldBeReturned() {
        // Set up the question object:
        IsaacGraphSketcherQuestion someGraphSketcherQuestion = new IsaacGraphSketcherQuestion();

        List<Choice> answerList = Lists.newArrayList();
        GraphChoice someCorrectAnswer = new GraphChoice();
        someCorrectAnswer.setValue("Testing123");
        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);

        someGraphSketcherQuestion.setChoices(answerList);

        String explanationShouldContain = "did not provide an answer";

        // Set up user answer:
        GraphChoice c = new GraphChoice();
        c.setValue("");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someGraphSketcherQuestion, c);
        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains(explanationShouldContain));
    }

    @Test
    public final void correctAnswerPasses() {
        // Set up the question object:
        IsaacGraphSketcherQuestion someGraphSketcherQuestion = new IsaacGraphSketcherQuestion();

        List<Choice> answerList = Lists.newArrayList();
        GraphChoice someCorrectAnswer = new GraphChoice();
        someCorrectAnswer.setValue("");
        someCorrectAnswer.setCorrect(true);
        someCorrectAnswer.setGraphSpec("through: bottomLeft, origin, topRight");
        answerList.add(someCorrectAnswer);

        someGraphSketcherQuestion.setChoices(answerList);

        // Set up user answer:
        GraphChoice c = new GraphChoice();
        c.setValue("{\"canvasWidth\":1440,\"canvasHeight\":503,\"curves\":[{\"pts\":[[-0.1318,-0.3429],[-0.1298,-0.3358],[-0.1276,-0.3286],[-0.1251,-0.3211],[-0.1224,-0.3136],[-0.1197,-0.3056],[-0.1168,-0.2974],[-0.1136,-0.2888],[-0.1105,-0.2801],[-0.107,-0.2709],[-0.1035,-0.2617],[-0.1001,-0.2525],[-0.0965,-0.2431],[-0.0928,-0.2337],[-0.0891,-0.2243],[-0.0855,-0.2149],[-0.0819,-0.2057],[-0.0782,-0.1965],[-0.0746,-0.1875],[-0.0712,-0.1786],[-0.0678,-0.1699],[-0.0644,-0.1612],[-0.0611,-0.1527],[-0.0577,-0.1444],[-0.0546,-0.1362],[-0.0514,-0.1283],[-0.0484,-0.1203],[-0.0453,-0.1127],[-0.0425,-0.105],[-0.0396,-0.0977],[-0.0367,-0.0905],[-0.0341,-0.0833],[-0.0314,-0.0763],[-0.0287,-0.0696],[-0.0261,-0.063],[-0.0237,-0.0564],[-0.0212,-0.0502],[-0.0186,-0.044],[-0.0163,-0.0379],[-0.0139,-0.032],[-0.0116,-0.0263],[-0.0094,-0.0206],[-0.0071,-0.0151],[-0.0049,-0.0096],[-0.0027,-0.0041],[-0.0005,0.0009],[0.0014,0.0061],[0.0036,0.0115],[0.0058,0.0167],[0.0079,0.0217],[0.0101,0.0269],[0.0122,0.0323],[0.0144,0.0375],[0.0165,0.0428],[0.0188,0.0483],[0.021,0.0537],[0.0233,0.0593],[0.0255,0.065],[0.0278,0.0709],[0.0302,0.0769],[0.0324,0.083],[0.0349,0.0893],[0.0373,0.0958],[0.0399,0.1026],[0.0425,0.1096],[0.045,0.1166],[0.0477,0.124],[0.0506,0.1315],[0.0533,0.1394],[0.0562,0.1474],[0.0592,0.1556],[0.0621,0.1641],[0.0652,0.1728],[0.0682,0.1816],[0.0715,0.1906],[0.0746,0.1998],[0.0779,0.2091],[0.0813,0.2185],[0.0846,0.228],[0.088,0.2374],[0.0914,0.247],[0.0946,0.2564],[0.098,0.2658],[0.1013,0.275],[0.1045,0.2839],[0.1076,0.2927],[0.1108,0.3012],[0.1136,0.3093],[0.1165,0.3171],[0.1192,0.3245],[0.1218,0.3313],[0.1241,0.3378],[0.1264,0.3438],[0.1283,0.3493],[0.1302,0.3543],[0.1318,0.3588],[0.1333,0.3628],[0.1345,0.3662],[0.1355,0.3691],[0.1363,0.3713],[0.1367,0.3726]],\"minX\":-0.13,\"maxX\":0.136,\"minY\":0.372,\"maxY\":-0.342,\"endPt\":[[530.0640000000001,423.9787],[916.272,64.7361]],\"interX\":[[-0.0009,0]],\"interY\":[[0,0.0024]],\"maxima\":[],\"minima\":[],\"colorIdx\":0}]}");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someGraphSketcherQuestion, c);
        assertTrue(response.isCorrect());
    }

    @Test
    public final void incorrectAnswerFails() {
        // Set up the question object:
        IsaacGraphSketcherQuestion someGraphSketcherQuestion = new IsaacGraphSketcherQuestion();

        List<Choice> answerList = Lists.newArrayList();
        GraphChoice someCorrectAnswer = new GraphChoice();
        someCorrectAnswer.setValue("");
        someCorrectAnswer.setCorrect(true);
        someCorrectAnswer.setGraphSpec("through: topLeft, origin, topRight");
        answerList.add(someCorrectAnswer);

        someGraphSketcherQuestion.setChoices(answerList);

        // Set up user answer:
        GraphChoice c = new GraphChoice();
        c.setValue("{\"canvasWidth\":1440,\"canvasHeight\":503,\"curves\":[{\"pts\":[[-0.1318,-0.3429],[-0.1298,-0.3358],[-0.1276,-0.3286],[-0.1251,-0.3211],[-0.1224,-0.3136],[-0.1197,-0.3056],[-0.1168,-0.2974],[-0.1136,-0.2888],[-0.1105,-0.2801],[-0.107,-0.2709],[-0.1035,-0.2617],[-0.1001,-0.2525],[-0.0965,-0.2431],[-0.0928,-0.2337],[-0.0891,-0.2243],[-0.0855,-0.2149],[-0.0819,-0.2057],[-0.0782,-0.1965],[-0.0746,-0.1875],[-0.0712,-0.1786],[-0.0678,-0.1699],[-0.0644,-0.1612],[-0.0611,-0.1527],[-0.0577,-0.1444],[-0.0546,-0.1362],[-0.0514,-0.1283],[-0.0484,-0.1203],[-0.0453,-0.1127],[-0.0425,-0.105],[-0.0396,-0.0977],[-0.0367,-0.0905],[-0.0341,-0.0833],[-0.0314,-0.0763],[-0.0287,-0.0696],[-0.0261,-0.063],[-0.0237,-0.0564],[-0.0212,-0.0502],[-0.0186,-0.044],[-0.0163,-0.0379],[-0.0139,-0.032],[-0.0116,-0.0263],[-0.0094,-0.0206],[-0.0071,-0.0151],[-0.0049,-0.0096],[-0.0027,-0.0041],[-0.0005,0.0009],[0.0014,0.0061],[0.0036,0.0115],[0.0058,0.0167],[0.0079,0.0217],[0.0101,0.0269],[0.0122,0.0323],[0.0144,0.0375],[0.0165,0.0428],[0.0188,0.0483],[0.021,0.0537],[0.0233,0.0593],[0.0255,0.065],[0.0278,0.0709],[0.0302,0.0769],[0.0324,0.083],[0.0349,0.0893],[0.0373,0.0958],[0.0399,0.1026],[0.0425,0.1096],[0.045,0.1166],[0.0477,0.124],[0.0506,0.1315],[0.0533,0.1394],[0.0562,0.1474],[0.0592,0.1556],[0.0621,0.1641],[0.0652,0.1728],[0.0682,0.1816],[0.0715,0.1906],[0.0746,0.1998],[0.0779,0.2091],[0.0813,0.2185],[0.0846,0.228],[0.088,0.2374],[0.0914,0.247],[0.0946,0.2564],[0.098,0.2658],[0.1013,0.275],[0.1045,0.2839],[0.1076,0.2927],[0.1108,0.3012],[0.1136,0.3093],[0.1165,0.3171],[0.1192,0.3245],[0.1218,0.3313],[0.1241,0.3378],[0.1264,0.3438],[0.1283,0.3493],[0.1302,0.3543],[0.1318,0.3588],[0.1333,0.3628],[0.1345,0.3662],[0.1355,0.3691],[0.1363,0.3713],[0.1367,0.3726]],\"minX\":-0.13,\"maxX\":0.136,\"minY\":0.372,\"maxY\":-0.342,\"endPt\":[[530.0640000000001,423.9787],[916.272,64.7361]],\"interX\":[[-0.0009,0]],\"interY\":[[0,0.0024]],\"maxima\":[],\"minima\":[],\"colorIdx\":0}]}");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someGraphSketcherQuestion, c);
        assertFalse(response.isCorrect());
    }
}
