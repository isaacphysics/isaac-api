package uk.ac.cam.cl.dtg.isaac.quiz;

import com.google.api.client.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.legacy.PowerMockRunner;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicChemistryQuestion;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.ChemicalFormula;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the ability of IsaacSymbolicChemistryValidator to validate chemistry answers correctly.
 *
 * Created by hhrl2 on 14/07/2016.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({IsaacSymbolicChemistryValidator.class})
public class IsaacSymbolicChemistryValidatorTest {

    @Before
    public final void setUp() {
        // Nothing needs to be set up in the beginning.
    }

    /**
     * Mocks the mhchem server communuication in IsaacSymbolicChemistryValidator.
     *  Returns fixed JSON objects provided in arguments.
     *
     * @param answer The student answer.
     * @param choices List of possible choices.
     * @param mockJsonStrings The list of JSON objects that should be returned by the server during comparison of answer and choice.
     * @throws Exception
     */
    private IsaacSymbolicChemistryValidator prepareTest(ChemicalFormula answer, List<Choice> choices, List<String> mockJsonStrings) throws Exception {

        // Mock the method JsonPostAndGet in symbolic chemistry validator.
        IsaacSymbolicChemistryValidator toReturn = PowerMock.createPartialMock(IsaacSymbolicChemistryValidator.class,
                                                                                "JsonPostAndGet");

        for (int i = 0; i < choices.size(); i++)
            PowerMock.expectPrivate(toReturn, "JsonPostAndGet", answer.getMhchemExpression(),
                    ((ChemicalFormula) choices.get(i)).getMhchemExpression()).andReturn(mockJsonStrings.get(i));

        // Mock all objects now.
        PowerMock.replayAll();

        return toReturn;

    }

    /**
     * Automates the whole chemistry checking problem.
     *
     * @param std_answer Student's mhchem expression, in String.
     * @param correct_choices List of mhchem strings, where each string is a correct choice from the problem.
     * @param wrong_choices List of mhchem strings, where each string is a wrong choice from the problem.
     * @param mockJsonStrings List of mock JSON strings that are to be returned by the mock mhchem server in response to
     *                        different choices.
     * @param std_correct Determines if student is correct or not. This is asserted by tester.
     * @param expectedExplanation The explanation that tester expects from chemistry validator.
     * @throws Exception
     */
    private void setTypicalChemistryQuestion(String std_answer, List<String> correct_choices,
                                                                List<String> wrong_choices,
                                                                List<String> mockJsonStrings,
                                                                boolean std_correct,
                                                                String expectedExplanation) throws Exception {

        // Asserts the list of mock JSON strings has same length as the list of all correct and wrong choices.
        assertTrue(correct_choices.size() + wrong_choices.size() == mockJsonStrings.size());

        // Create a typical chemistry question
        IsaacSymbolicChemistryQuestion testQuestion = new IsaacSymbolicChemistryQuestion();

        // List of choices
        ArrayList<Choice> answerList = Lists.newArrayList();

        // Put in the list of correct choices
        for (String correct_choice: correct_choices) {
            ChemicalFormula correct = new ChemicalFormula();
            correct.setMhchemExpression(correct_choice);
            correct.setCorrect(true);
            answerList.add(correct);
        }

        // Put in the list of wrong choices
        for (String wrong_choice: wrong_choices) {
            ChemicalFormula wrong = new ChemicalFormula();
            wrong.setMhchemExpression(wrong_choice);
            wrong.setCorrect(false);
            answerList.add(wrong);
        }

        // Set the choices in question
        testQuestion.setChoices(answerList);

        // Set student answer
        ChemicalFormula answer = new ChemicalFormula();
        answer.setMhchemExpression(std_answer);

        // Set up the mock validator
        IsaacSymbolicChemistryValidator test = prepareTest(answer, answerList, mockJsonStrings);

        // Return the validation results from mocked validator.
        QuestionValidationResponse response = test.validateQuestionResponse(testQuestion, answer);

        // Verify that objects are indeed mocked.
        PowerMock.verifyAll();

        // Make assertions based on the expected correctness of student answer
        if (std_correct)
        {
            assertTrue("Expected response to be correct, but it turns out wrong.", response.isCorrect());
        }
        else
        {
            assertFalse("Expected response to be wrong, but it turns out correct.", response.isCorrect());
            assertTrue("Received \"" + response.getExplanation().getValue() + "\", but expected \"" + expectedExplanation + "\"",
                    response.getExplanation().equals(new Content(expectedExplanation)));
        }
    }

    /**
     * Tests if back end could detect type mismatch from answer.
     * Correct answer: Na + Cl -> NaCl (Chemical equation)
     * Student answer: Na (Chemical expression)
     * @throws Exception
     */
    @Test
    public final void typeMismatchTest() throws Exception {

        // Create the JSON object that server should return
        String mockJsonString = "{\n" +
                "  \"testString\" : \"Na\",\n" +
                "  \"targetString\" : \"Na+Cl->NaCl\",\n" +
                "  \"test\" : \"Na\",\n" +
                "  \"target\" : \"Na + Cl -> NaCl\",\n" +
                "  \"containsError\" : false,\n" +
                "  \"equal\" : false,\n" +
                "  \"typeMismatch\" : true,\n" +
                "  \"expectedType\" : \"equation\",\n" +
                "  \"receivedType\" : \"expression\",\n" +
                "  \"weaklyEquivalent\" : false,\n" +
                "  \"sameCoefficient\" : false,\n" +
                "  \"sameState\" : false,\n" +
                "  \"wrongTerms\" : [ ]\n" +
                "}";

        String testString = "Na";
        String targetString = "Na + Cl -> NaCl";

        // Get response from IsaacSymbolicChemistryValidator
        setTypicalChemistryQuestion(testString,
                Collections.singletonList(targetString), new ArrayList<String>(),
                Collections.singletonList(mockJsonString), false,
                "Your answer is an expression but we expected an equation.");
    }

    /**
     * Tests if back end could detect error term(s) from user input.
     * User input: Na++Cl
     * Correct ans: Na+Cl
     */
    @Test
    public final void errorTermTest() throws Exception {

        String mockJsonString = "{\n" +
                "  \"testString\" : \"Na++Cl\",\n" +
                "  \"targetString\" : \"Na+Cl\",\n" +
                "  \"test\" : \"Na + ERROR + Cl\",\n" +
                "  \"target\" : \"Na + Cl\",\n" +
                "  \"containsError\" : true,\n" +
                "  \"equal\" : false,\n" +
                "  \"typeMismatch\" : false,\n" +
                "  \"expectedType\" : \"expression\",\n" +
                "  \"receivedType\" : \"expression\",\n" +
                "  \"weaklyEquivalent\" : false,\n" +
                "  \"sameCoefficient\" : false,\n" +
                "  \"sameState\" : false,\n" +
                "  \"wrongTerms\" : [ ]\n" +
                "}";

        String testString = "Na++Cl";
        String targetString = "Na+Cl";

        // Get response from IsaacSymbolicChemistryValidator
        setTypicalChemistryQuestion(testString,
                Collections.singletonList(targetString), new ArrayList<String>(),
                Collections.singletonList(mockJsonString), false,
                "Your answer contains invalid syntax!");
    }

    /**
     * Tests if back end could check if equation is unbalanced, if it doesn't match with any choices we have.
     */
    @Test
    public final void unbalancedEquationTest() throws Exception {

        String mockJsonString = "{\n" +
                "  \"testString\" : \"Cr2O7^{2-}+13H3O^{+}+6e^{-}->2Cr^{3+}\",\n" +
                "  \"targetString\" : \"Cr2O7^{2-}+14H3O^{+}+6e^{-}->2Cr^{3+}+21H2O\",\n" +
                "  \"test\" : \"Cr2O7^{2-} + 13H3O^{+} + 6e^{-} -> 2Cr^{3+}\",\n" +
                "  \"target\" : \"Cr2O7^{2-} + 14H3O^{+} + 6e^{-} -> 2Cr^{3+} + 21H2O\",\n" +
                "  \"containsError\" : false,\n" +
                "  \"equal\" : false,\n" +
                "  \"typeMismatch\" : false,\n" +
                "  \"expectedType\" : \"equation\",\n" +
                "  \"receivedType\" : \"equation\",\n" +
                "  \"weaklyEquivalent\" : false,\n" +
                "  \"sameCoefficient\" : false,\n" +
                "  \"sameState\" : false,\n" +
                "  \"sameArrow\" : true,\n" +
                "  \"isBalanced\" : false,\n" +
                "  \"balancedAtoms\" : false,\n" +
                "  \"balancedCharge\" : false,\n" +
                "  \"wrongTerms\" : [ \"13H3O^{+}\" ]\n" +
                "}";

        // Create a typical chemistry question
        String testString = "Cr2O7^{2-}+13H3O^{+}+6e^{-}->2Cr^{3+}";
        String targetString = "Cr2O7^{2-}+14H3O^{+}+6e^{-}->2Cr^{3+}+21H2O";

        // Get response from IsaacSymbolicChemistryValidator
        setTypicalChemistryQuestion(testString,
                Collections.singletonList(targetString), new ArrayList<String>(),
                Collections.singletonList(mockJsonString), false,
                "Your equation is unbalanced.");
    }

    /**
     * Tests if back end could check if nuclear equation is invalid, given that it doesn't match with any choices we have.
     * @throws Exception
     */
    @Test
    public final void invalidNuclearEquationTest() throws Exception {

        String mockJsonString = "{\n" +
                "  \"testString\" : \"^{222}_{88}Ra -> ^{4}_{2}H + ^{218}_{86}Rn\",\n" +
                "  \"targetString\" : \"^{222}_{88}Ra -> ^{4}_{2}\\\\alphaparticle + ^{218}_{86}Rn\",\n" +
                "  \"test\" : \"^{222}_{88}Ra -> ^{4}_{2}H + ^{218}_{86}Rn\",\n" +
                "  \"target\" : \"^{222}_{88}Ra -> ^{4}_{2}\\\\alphaparticle + ^{218}_{86}Rn\",\n" +
                "  \"containsError\" : false,\n" +
                "  \"equal\" : false,\n" +
                "  \"typeMismatch\" : false,\n" +
                "  \"expectedType\" : \"nuclearequation\",\n" +
                "  \"receivedType\" : \"nuclearequation\",\n" +
                "  \"weaklyEquivalent\" : false,\n" +
                "  \"isBalanced\" : true,\n" +
                "  \"balancedAtomic\" : true,\n" +
                "  \"balancedMass\" : true,\n" +
                "  \"validAtomicNumber\" : false,\n" +
                "  \"wrongTerms\" : [ \"^{4}_{2}H\" ]\n" +
                "}";

        // Create a typical chemistry question
        String testString = "^{222}_{88}Ra -> ^{4}_{2}H + ^{218}_{86}Rn";
        String targetString = "^{222}_{88}Ra -> ^{4}_{2}\\\\alphaparticle + ^{218}_{86}Rn";

        // Get response from IsaacSymbolicChemistryValidator
        setTypicalChemistryQuestion(testString,
                Collections.singletonList(targetString), new ArrayList<String>(),
                Collections.singletonList(mockJsonString), false,
                "Check your atomic/mass numbers!");
    }

    /**
     * Tests for chemical equation/expressions, which is weakly equivalent to the correct answer.
     * Weakly equivalent: All terms match except state symbols and coefficients, ignoring arrows.
     *
     * Correct answer here: Na^{+}(aq) + Cl^{-}(aq) -> NaCl(aq)
     *
     * 1. Unbalanced equation: Unbalanced atom count. E.g. Na^{+}(aq) + Cl^{-}(aq) -> 2NaCl(aq)
     * 2. Unbalanced equation: Unbalanced charge. E.g.
     * 3. Wrong state symbols in some terms. E.g. Na^{+} + Cl^{-} -> NaCl
     * 4. Wrong coefficients in some terms. E.g. 2Na^{+}(aq) + 2Cl^{-}(aq) -> 2NaCl(aq)
     * 5. Wrong arrow used.
     *
     * @throws Exception
     */
    @Test
    public final void ChemicalWeakEquivalenceTest() throws Exception {

    }


}
