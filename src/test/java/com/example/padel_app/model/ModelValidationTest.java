package com.example.padel_app.model;

import com.example.padel_app.model.enums.Level;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit Test per Model Validation.
 * 
 * OBIETTIVO:
 * Verificare che le annotazioni di validazione (@NotNull, @Email, @Size)
 * sulle entità funzionino come previsto.
 */
class ModelValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("User valido non deve avere violazioni")
    void testValidUser() {
        User user = new User();
        user.setUsername("validuser");
        user.setEmail("valid@test.com");
        user.setPassword("password123");
        user.setFirstName("Mario");
        user.setLastName("Rossi");
        user.setDeclaredLevel(Level.INTERMEDIO);

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty(), "User valido non dovrebbe avere violazioni");
    }

    @Test
    @DisplayName("User con email non valida deve avere violazioni")
    void testInvalidEmail() {
        User user = new User();
        user.setUsername("validuser");
        user.setEmail("not-an-email"); // Invalid format
        user.setPassword("password123");
        user.setFirstName("Mario");
        user.setLastName("Rossi");
        user.setDeclaredLevel(Level.INTERMEDIO);

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "Email non valida dovrebbe generare violazioni");
        
        boolean hasEmailError = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        assertTrue(hasEmailError, "Dovrebbe esserci un errore sul campo email");
    }

    @Test
    @DisplayName("User con campi obbligatori nulli deve avere violazioni")
    void testNullFields() {
        User user = new User();
        // Username, Email, Password, ecc. sono null

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "Campi nulli dovrebbero generare violazioni");
    }
}
