package com.eylul.ticketplatform.identity.internal.registration;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestValidationTests {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private Set<ConstraintViolation<RegisterRequest>> validate(String password) {
        return validator.validate(new RegisterRequest("test.user@example.com", password));
    }

    @Test
    void givenValidPassword_whenValidate_thenNoViolations() {
        assertThat(validate("Passw0rd!x")).isEmpty();
    }

    @Test
    void givenPasswordWithSevenCharacters_whenValidate_thenSizeViolation() {
        Set<ConstraintViolation<RegisterRequest>> violations = validate("Pass1!x");

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Password must have 8 to 72 characters");
    }
    @Test
    void givenPasswordWithoutUpperCase_whenValidate_thenUpperCaseViolation() {
        Set<ConstraintViolation<RegisterRequest>> violations = validate("passw0rd!x");

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Password must contain an upper case letter");
    }

    @Test
    void givenPasswordWithoutDigit_whenValidate_thenDigitViolation() {
        Set<ConstraintViolation<RegisterRequest>> violations = validate("Password!x");

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Password must contain a digit");
    }

    @Test
    void givenPasswordWithoutSpecialCharacter_whenValidate_thenSpecialCharacterViolation() {
        Set<ConstraintViolation<RegisterRequest>> violations = validate("Passw0rdxx");

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Password must contain a special character");
    }

    @Test
    void givenPasswordWithSeventyThreeCharacters_whenValidate_thenSizeViolation() {
        String password = "Aa1!" + "x".repeat(69);

        Set<ConstraintViolation<RegisterRequest>> violations = validate(password);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Password must have 8 to 72 characters");
    }

    @Test
    void givenPasswordWithEightCharacters_whenValidate_thenNoViolations() {
        assertThat(validate("Passw0r!")).isEmpty();
    }

    @Test
    void givenPasswordWithSeventyTwoCharacters_whenValidate_thenNoViolations() {
        String password = "Aa1!" + "x".repeat(68);

        assertThat(validate(password)).isEmpty();
    }
}