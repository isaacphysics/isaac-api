package uk.ac.cam.cl.dtg.segue.api.managers;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager.isEmailValid;
import static uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager.isUserNameValid;

public class UserAccountManagerTest {

    @ParameterizedTest
    @MethodSource("namesToTest")
    public void nameValidation(boolean expectedIsValid, String name) {
        boolean actualIsValid = isUserNameValid(name);
        assertEquals(expectedIsValid, actualIsValid);
    }

    private static Stream<Arguments> namesToTest() {
        return Stream.of(
                Arguments.of(true, "testname"), // Lowercase is valid
                Arguments.of(true, "TESTNAME"), // Uppercase is valid
                Arguments.of(true, "TestName"), // Mixture of cases is valid
                Arguments.of(true, "TestName123"), // Numbers are valid
                Arguments.of(true, "Test_Name"), // Underscores are valid
                Arguments.of(true, "Test Name"), // Double-barrelled names with spaces are valid
                Arguments.of(true, "Test-Name"), // Double-barrelled names with hyphens are valid
                Arguments.of(true, "O'Name"), // Apostrophes are valid
                // Accented and non-English characters are valid.
                // Please note, this is not an exhustive test - unicode supports thousands of such characters
                Arguments.of(true, "ÃëóûÿĉĐŗǥȕḍṦμϱнӯ"),
                Arguments.of(false, null), // Null is not valid
                Arguments.of(false, ""), // Empty string is not valid
                Arguments.of(false, " "), // Only whitespace is not valid
                Arguments.of(false, "a".repeat(256)), // Names exceeding the maximum length of 255 characters are not valid
                // Other special characters are not permitted
                Arguments.of(false, "Test!Name"),
                Arguments.of(false, "Test?Name"),
                Arguments.of(false, "Test#Name"),
                Arguments.of(false, "Test.Name"),
                Arguments.of(false, "Test,Name"),
                Arguments.of(false, "Test*Name"),
                Arguments.of(false, "Test<Name"),
                Arguments.of(false, "Test>Name"),
                Arguments.of(false, "Test:Name"),
                Arguments.of(false, "Test;Name"),
                Arguments.of(false, "Test/Name")
        );
    }

    @ParameterizedTest
    @MethodSource("emailsToTest")
    public void emailValidation(boolean expectedIsValid, String email) {
        boolean actualIsValid = isEmailValid(email);
        assertEquals(expectedIsValid, actualIsValid);
    }

    private static Stream<Arguments> emailsToTest() {
        return Stream.of(
                Arguments.of(true, "testemail@test.com"), // Standard email format is valid
                // Alternate pattern for twitter|google|facebook is valid
                Arguments.of(true, "testemail-twitter"),
                Arguments.of(true, "testemail-google"),
                Arguments.of(true, "testemail-facebook"),
                Arguments.of(true, "test!#$%&'+-=?^_`.{|}~email@test.com"), // Some special characters are permitted
                Arguments.of(true, "testemail@test.co.uk"), // Multiple seperated .s are valid
                Arguments.of(false, null), // Null is not valid
                Arguments.of(false, ""), // Empty string is not valid
                Arguments.of(false, " "), // Only whitespace is not valid
                Arguments.of(false, "test.email@testcom"), // Email must have at least one . after the @
                Arguments.of(false, "testemailtest.com"), // Standard email must include an @
                Arguments.of(false, "testemail@test."), // Email must have be at least character after the last .
                Arguments.of(false, "testemail@.com"), // Email must have at least one character between the @ and the last .
                Arguments.of(false, "testemail@test..com"), // Email cannot contain consecutive full stops
                // Other special characters are not permitted
                Arguments.of(false, "test\"email@test.com"),
                Arguments.of(false, "test(email@test.com"),
                Arguments.of(false, "test)email@test.com"),
                Arguments.of(false, "test*email@test.com"),
                Arguments.of(false, "test/email@test.com"),
                Arguments.of(false, "test<email@test.com"),
                Arguments.of(false, "test>email@test.com"),
                Arguments.of(false, "test:email@test.com"),
                Arguments.of(false, "test;email@test.com"),
                // Other alternate endings are not permitted
                Arguments.of(false, "testemail-example"),
                Arguments.of(false, "testemail-twittter")
        );
    }
}
