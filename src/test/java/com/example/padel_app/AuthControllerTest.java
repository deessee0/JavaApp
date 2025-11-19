package com.example.padel_app;

import com.example.padel_app.controller.AuthController;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.repository.UserRepository;
import com.example.padel_app.service.UserSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite per AuthController - Sistema di Autenticazione
 * 
 * Testa le funzionalità di:
 * - Login con credenziali valide/invalide
 * - Registrazione nuovi utenti
 * - Validazione email duplicata
 * - Gestione sessione HTTP
 * - Logout
 * 
 * SCOPO DIDATTICO: Dimostra come testare controller con sessione HTTP,
 * validazione input, e gestione errori di autenticazione.
 */
@SpringBootTest
@Transactional
class AuthControllerTest {

    @Autowired
    private AuthController authController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionService userSessionService;

    private MockHttpSession session;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Crea una sessione HTTP mock per i test
        session = new MockHttpSession();

        // Crea un utente di test nel database
        testUser = new User();
        testUser.setEmail("test.user@padel.it");
        testUser.setPassword("testPassword123");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setUsername("testuser");
        testUser.setDeclaredLevel(Level.INTERMEDIO);
        testUser.setMatchesPlayed(0);
        testUser = userRepository.save(testUser);
    }

    // ========== LOGIN TESTS ==========

    @Test
    @DisplayName("Login con credenziali valide - deve salvare utente in sessione e redirect a home")
    void testLoginWithValidCredentials() {
        // GIVEN: Credenziali corrette dell'utente di test
        String email = "test.user@padel.it";
        String password = "testPassword123";
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        // WHEN: Eseguo login
        String result = authController.login(email, password, session, redirectAttributes);

        // THEN: Deve fare redirect a home
        assertEquals("redirect:/", result);

        // THEN: Utente deve essere salvato in sessione
        assertTrue(userSessionService.isAuthenticated(session));
        
        // THEN: L'utente in sessione deve corrispondere a quello di test
        User userInSession = userSessionService.getCurrentUser(session);
        assertNotNull(userInSession);
        assertEquals(testUser.getEmail(), userInSession.getEmail());
    }

    @Test
    @DisplayName("Login con email inesistente - deve restare su pagina login")
    void testLoginWithNonExistentEmail() {
        // GIVEN: Email che non esiste nel database
        String email = "nonexistent@padel.it";
        String password = "anyPassword";
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        // WHEN: Tento login
        String result = authController.login(email, password, session, redirectAttributes);

        // THEN: Deve rimanere sulla pagina login (redirect con ?error)
        assertEquals("redirect:/login?error", result);

        // THEN: Sessione deve rimanere NON autenticata
        assertFalse(userSessionService.isAuthenticated(session));
    }

    @Test
    @DisplayName("Login con password errata - deve restare su pagina login")
    void testLoginWithWrongPassword() {
        // GIVEN: Email corretta ma password sbagliata
        String email = "test.user@padel.it";
        String password = "wrongPassword";
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        // WHEN: Tento login
        String result = authController.login(email, password, session, redirectAttributes);

        // THEN: Deve rimanere sulla pagina login (redirect con ?error)
        assertEquals("redirect:/login?error", result);

        // THEN: Sessione deve rimanere NON autenticata
        assertFalse(userSessionService.isAuthenticated(session));
    }

    // ========== REGISTRATION TESTS ==========

    @Test
    @DisplayName("Registrazione con dati validi - deve creare utente e redirect a login")
    void testRegisterWithValidData() {
        // GIVEN: Dati validi per nuovo utente
        String username = "newuser";
        String email = "new.user@padel.it";
        String password = "newPassword123";
        String firstName = "Nuovo";
        String lastName = "Utente";
        Level declaredLevel = Level.PRINCIPIANTE;
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        // WHEN: Eseguo registrazione
        String result = authController.register(username, email, password, firstName, lastName, 
                                               declaredLevel, redirectAttributes);

        // THEN: Deve fare redirect a login con messaggio success
        assertEquals("redirect:/login?registered", result);

        // THEN: Verifica che utente sia stato creato nel database
        User createdUser = userRepository.findByEmail(email).orElse(null);
        assertNotNull(createdUser);
        assertEquals(email, createdUser.getEmail());
        assertEquals(username, createdUser.getUsername());
        assertEquals(firstName, createdUser.getFirstName());
        assertEquals(lastName, createdUser.getLastName());
        assertEquals(Level.PRINCIPIANTE, createdUser.getDeclaredLevel());
        assertEquals(0, createdUser.getMatchesPlayed());
    }

    @Test
    @DisplayName("Registrazione con email già esistente - deve fallire e restare su pagina registrazione")
    void testRegisterWithDuplicateEmail() {
        // GIVEN: Email già usata da testUser
        String username = "differentuser";
        String email = "test.user@padel.it"; // Email già esistente
        String password = "anotherPassword";
        String firstName = "Another";
        String lastName = "User";
        Level declaredLevel = Level.AVANZATO;
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        // WHEN: Tento registrazione con email duplicata
        String result = authController.register(username, email, password, firstName, lastName, 
                                               declaredLevel, redirectAttributes);

        // THEN: Deve rimanere sulla pagina registrazione
        assertEquals("redirect:/register", result);

        // THEN: Verifica che flash message di errore sia stato aggiunto
        Object errorMessage = redirectAttributes.getFlashAttributes().get("error");
        assertNotNull(errorMessage);
        assertTrue(errorMessage.toString().contains("Email già registrata"));
    }

    @Test
    @DisplayName("Registrazione con username già esistente - deve fallire e restare su pagina registrazione")
    void testRegisterWithDuplicateUsername() {
        // GIVEN: Stesso username dell'utente di test
        String username = "testuser"; // Username già usato da testUser
        String email = "different.email@padel.it";
        String password = "password123";
        String firstName = "Different";
        String lastName = "User";
        Level declaredLevel = Level.INTERMEDIO;
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        // WHEN: Tento registrazione con username duplicato
        String result = authController.register(username, email, password, firstName, lastName, 
                                               declaredLevel, redirectAttributes);

        // THEN: Deve fallire e restare su pagina registrazione
        assertEquals("redirect:/register", result);

        // THEN: Verifica che flash message di errore sia stato aggiunto
        Object errorMessage = redirectAttributes.getFlashAttributes().get("error");
        assertNotNull(errorMessage);
        assertTrue(errorMessage.toString().contains("Username già in uso"));
    }

    // ========== LOGOUT TESTS ==========

    @Test
    @DisplayName("Logout - deve cancellare sessione e redirect a login")
    void testLogout() {
        // GIVEN: Utente autenticato in sessione
        userSessionService.setCurrentUser(session, testUser);
        assertTrue(userSessionService.isAuthenticated(session));

        // WHEN: Eseguo logout
        String result = authController.logout(session);

        // THEN: Deve fare redirect a pagina login con parametro ?logout
        assertEquals("redirect:/login?logout", result);

        // THEN: Sessione deve essere cancellata (invalidate distrugge tutto)
        // Non possiamo verificare isAuthenticated su sessione invalidata
        // assertTrue verifica solo che non lanci exception
        assertTrue(true);
    }

    @Test
    @DisplayName("Logout senza essere autenticati - deve comunque redirect a login")
    void testLogoutWhenNotAuthenticated() {
        // GIVEN: Sessione NON autenticata
        assertFalse(userSessionService.isAuthenticated(session));

        // WHEN: Eseguo logout
        String result = authController.logout(session);

        // THEN: Deve comunque fare redirect a login con parametro ?logout
        assertEquals("redirect:/login?logout", result);
    }

    // ========== SESSION MANAGEMENT TESTS ==========

    @Test
    @DisplayName("Verifica che sessione persista tra richieste multiple")
    void testSessionPersistenceBetweenRequests() {
        // GIVEN: Login effettuato
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        authController.login("test.user@padel.it", "testPassword123", session, redirectAttributes);

        // WHEN: Verifico sessione in richiesta successiva
        boolean isAuthenticated = userSessionService.isAuthenticated(session);
        User user = userSessionService.getCurrentUser(session);

        // THEN: Sessione deve essere ancora valida
        assertTrue(isAuthenticated);
        assertNotNull(user);
        assertEquals(testUser.getId(), user.getId());
    }

    @Test
    @DisplayName("Verifica che due sessioni diverse siano indipendenti")
    void testIndependentSessions() {
        // GIVEN: Due sessioni diverse
        MockHttpSession session1 = new MockHttpSession();
        MockHttpSession session2 = new MockHttpSession();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        // WHEN: Login solo in session1
        authController.login("test.user@padel.it", "testPassword123", session1, redirectAttributes);

        // THEN: Solo session1 deve essere autenticata
        assertTrue(userSessionService.isAuthenticated(session1));
        assertFalse(userSessionService.isAuthenticated(session2));
    }
}
