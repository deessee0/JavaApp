package com.example.padel_app.controller;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.User;
import com.example.padel_app.model.Registration;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.service.*;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test per WebController (Controller principale MVC)
 *
 * OBIETTIVO: Coverage dei metodi chiave del controller
 *
 * METODI TESTATI:
 * - home (home page con lista matches)
 * - myMatches (partite dell'utente)
 * - createMatch (creazione partita)
 * - joinMatch (iscrizione)
 * - leaveMatch (disiscrizione)
 * - Redirect quando utente non loggato
 *
 * PATTERN:
 * - Mock di tutti i service
 * - Mock di HttpSession e Model
 * - Verifica view names e redirect
 * - Verifica attributi aggiunti al model
 *
 * FIXES APPLICATI (Copilot Review):
 * - Corretto index() → home()
 * - Corretto model attribute "matches" → "availableMatches"
 * - Corretto mock matchService.getAvailableMatches() → getAllMatches() + sortMatches()
 * - Corretto allMatches() → matches()
 * - Corretto usersList() → users()
 * - Corretto createMatch() con CreateMatchRequest
 * - Corretto mock matchService.createMatch() → saveMatch()
 * - Corretto flash attribute "successMessage" → "success" e "errorMessage" → "error"
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebController Integration Tests")
class WebControllerTest {

    @Mock
    private MatchService matchService;

    @Mock
    private UserService userService;

    @Mock
    private RegistrationService registrationService;

    @Mock
    private FeedbackService feedbackService;

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private HttpSession session;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private WebController webController;

    private User testUser;
    private Match testMatch;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("alice");
        testUser.setDeclaredLevel(Level.INTERMEDIO);

        testMatch = new Match();
        testMatch.setId(1L);
        testMatch.setLocation("Campo 1");
        testMatch.setStatus(MatchStatus.WAITING);
        testMatch.setRequiredLevel(Level.INTERMEDIO);
        testMatch.setDateTime(LocalDateTime.now().plusDays(1));
        testMatch.setCreator(testUser);
    }

    // ==================== HOME PAGE ====================

    @Test
    @DisplayName("home - should show matches when user authenticated")
    void home_shouldShowMatches_whenAuthenticated() {
        // Arrange
        List<Match> matches = Arrays.asList(testMatch);
        when(userSessionService.getCurrentUser(session)).thenReturn(testUser);
        when(matchService.getAllMatches()).thenReturn(matches);
        when(matchService.sortMatches(any(), eq("date"))).thenReturn(matches);
        when(registrationService.isUserRegistered(testUser, testMatch)).thenReturn(false);
        when(registrationService.getActiveRegistrationsCount(testMatch)).thenReturn(2);
        when(model.addAttribute(anyString(), any())).thenReturn(model);

        // Act
        String viewName = webController.home(session, null, "date", model);

        // Assert
        assertThat(viewName).isEqualTo("index");
        verify(model).addAttribute(eq("currentUser"), eq(testUser));
        verify(model).addAttribute(eq("availableMatches"), any());
        verify(matchService).getAllMatches();
        verify(matchService).sortMatches(any(), eq("date"));
    }

    @Test
    @DisplayName("home - should redirect to login when not authenticated")
    void home_shouldRedirectToLogin_whenNotAuthenticated() {
        // Arrange
        when(userSessionService.getCurrentUser(session)).thenReturn(null);

        // Act
        String viewName = webController.home(session, null, "date", model);

        // Assert
        assertThat(viewName).isEqualTo("redirect:/login");
        verify(matchService, never()).getAllMatches();
    }

    // ==================== MY MATCHES ====================

    @Test
    @DisplayName("myMatches - should show user's matches when authenticated")
    void myMatches_shouldShowUserMatches_whenAuthenticated() {
        // Arrange
        Registration reg = new Registration();
        reg.setMatch(testMatch);
        List<Registration> registrations = Arrays.asList(reg);

        when(userSessionService.getCurrentUser(session)).thenReturn(testUser);
        when(registrationService.getActiveRegistrationsByUser(testUser)).thenReturn(registrations);
        when(matchService.sortMatches(any(), eq("date"))).thenReturn(Arrays.asList(testMatch));
        when(registrationService.getActiveRegistrationsCount(testMatch)).thenReturn(2);
        when(model.addAttribute(anyString(), any())).thenReturn(model);

        // Act
        String viewName = webController.myMatches(session, null, "date", model);

        // Assert
        assertThat(viewName).isEqualTo("my-matches");
        verify(model).addAttribute(eq("currentUser"), eq(testUser));
        verify(model).addAttribute(eq("registeredMatches"), any());
        verify(registrationService).getActiveRegistrationsByUser(testUser);
    }

    @Test
    @DisplayName("myMatches - should redirect to login when not authenticated")
    void myMatches_shouldRedirectToLogin_whenNotAuthenticated() {
        // Arrange
        when(userSessionService.getCurrentUser(session)).thenReturn(null);

        // Act
        String viewName = webController.myMatches(session, null, "date", model);

        // Assert
        assertThat(viewName).isEqualTo("redirect:/login");
        verify(registrationService, never()).getActiveRegistrationsByUser(any());
    }

    // ==================== CREATE MATCH ====================

    @Test
    @DisplayName("createMatchForm - should show form when authenticated")
    void createMatchForm_shouldShowForm_whenAuthenticated() {
        // Arrange
        when(userSessionService.getCurrentUser(session)).thenReturn(testUser);
        when(model.addAttribute(anyString(), any())).thenReturn(model);

        // Act
        String viewName = webController.createMatchForm(session, model);

        // Assert
        assertThat(viewName).isEqualTo("create-match");
        verify(model).addAttribute(eq("matchRequest"), any());
        verify(model).addAttribute(eq("levels"), any());
    }

    @Test
    @DisplayName("createMatch - should create match and join automatically")
    void createMatch_shouldCreateAndJoin_whenValid() {
        // Arrange
        WebController.CreateMatchRequest request = new WebController.CreateMatchRequest();
        request.setLocation("Campo 1");
        request.setDescription("Test match");
        request.setRequiredLevel("INTERMEDIO");
        request.setDateTime("2024-12-25T15:00:00");

        when(userSessionService.getCurrentUser(session)).thenReturn(testUser);
        when(matchService.saveMatch(any(Match.class))).thenReturn(testMatch);
        when(registrationService.joinMatch(testUser, testMatch)).thenReturn(new Registration());

        // Act
        String viewName = webController.createMatch(session, request, model);

        // Assert
        assertThat(viewName).isEqualTo("redirect:/my-matches");
        verify(matchService).saveMatch(any(Match.class));
        verify(registrationService).joinMatch(testUser, testMatch);
    }

    @Test
    @DisplayName("createMatch - should redirect to login when not authenticated")
    void createMatch_shouldRedirectToLogin_whenNotAuthenticated() {
        // Arrange
        WebController.CreateMatchRequest request = new WebController.CreateMatchRequest();
        when(userSessionService.getCurrentUser(session)).thenReturn(null);

        // Act
        String viewName = webController.createMatch(session, request, model);

        // Assert
        assertThat(viewName).isEqualTo("redirect:/login");
        verify(matchService, never()).saveMatch(any());
    }

    // ==================== JOIN MATCH ====================

    @Test
    @DisplayName("joinMatch - should join successfully when authenticated")
    void joinMatch_shouldJoinSuccessfully_whenAuthenticated() {
        // Arrange
        when(userSessionService.getCurrentUser(session)).thenReturn(testUser);
        when(matchService.getMatchById(1L)).thenReturn(Optional.of(testMatch));
        when(registrationService.joinMatch(testUser, testMatch)).thenReturn(new Registration());
        when(redirectAttributes.addFlashAttribute(anyString(), anyString())).thenReturn(redirectAttributes);

        // Act
        String viewName = webController.joinMatch(session, 1L, redirectAttributes);

        // Assert
        assertThat(viewName).isEqualTo("redirect:/");
        verify(registrationService).joinMatch(testUser, testMatch);
        verify(redirectAttributes).addFlashAttribute(eq("success"), anyString());
    }

    @Test
    @DisplayName("joinMatch - should handle match not found")
    void joinMatch_shouldHandleMatchNotFound() {
        // Arrange
        when(userSessionService.getCurrentUser(session)).thenReturn(testUser);
        when(matchService.getMatchById(999L)).thenReturn(Optional.empty());
        when(redirectAttributes.addFlashAttribute(anyString(), anyString())).thenReturn(redirectAttributes);

        // Act
        String viewName = webController.joinMatch(session, 999L, redirectAttributes);

        // Assert
        assertThat(viewName).isEqualTo("redirect:/");
        verify(registrationService, never()).joinMatch(any(), any());
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    @Test
    @DisplayName("joinMatch - should handle already registered exception")
    void joinMatch_shouldHandleAlreadyRegistered() {
        // Arrange
        when(userSessionService.getCurrentUser(session)).thenReturn(testUser);
        when(matchService.getMatchById(1L)).thenReturn(Optional.of(testMatch));
        when(registrationService.joinMatch(testUser, testMatch))
            .thenThrow(new IllegalStateException("already registered"));
        when(redirectAttributes.addFlashAttribute(anyString(), anyString())).thenReturn(redirectAttributes);

        // Act
        String viewName = webController.joinMatch(session, 1L, redirectAttributes);

        // Assert
        assertThat(viewName).isEqualTo("redirect:/");
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    // ==================== LEAVE MATCH ====================

    @Test
    @DisplayName("leaveMatch - should leave successfully when authenticated")
    void leaveMatch_shouldLeaveSuccessfully_whenAuthenticated() {
        // Arrange
        when(userSessionService.getCurrentUser(session)).thenReturn(testUser);
        when(matchService.getMatchById(1L)).thenReturn(Optional.of(testMatch));
        doNothing().when(registrationService).leaveMatch(testUser, testMatch);
        when(redirectAttributes.addFlashAttribute(anyString(), anyString())).thenReturn(redirectAttributes);

        // Act
        String viewName = webController.leaveMatch(session, 1L, redirectAttributes);

        // Assert
        assertThat(viewName).isEqualTo("redirect:/my-matches");
        verify(registrationService).leaveMatch(testUser, testMatch);
        verify(redirectAttributes).addFlashAttribute(eq("success"), anyString());
    }

    @Test
    @DisplayName("leaveMatch - should redirect to login when not authenticated")
    void leaveMatch_shouldRedirectToLogin_whenNotAuthenticated() {
        // Arrange
        when(userSessionService.getCurrentUser(session)).thenReturn(null);

        // Act
        String viewName = webController.leaveMatch(session, 1L, redirectAttributes);

        // Assert
        assertThat(viewName).isEqualTo("redirect:/login");
        verify(registrationService, never()).leaveMatch(any(), any());
    }

    // ==================== MATCHES LIST ====================

    @Test
    @DisplayName("matches - should show all matches when authenticated")
    void matches_shouldShowAllMatches_whenAuthenticated() {
        // Arrange
        List<Match> matches = Arrays.asList(testMatch);
        when(userSessionService.getCurrentUser(session)).thenReturn(testUser);
        when(matchService.getAllMatches()).thenReturn(matches);
        when(matchService.sortMatches(matches, "date")).thenReturn(matches);
        when(model.addAttribute(anyString(), any())).thenReturn(model);

        // Act
        String viewName = webController.matches(session, null, "date", model);

        // Assert
        assertThat(viewName).isEqualTo("matches");
        verify(model).addAttribute(eq("matches"), eq(matches));
        verify(matchService).getAllMatches();
        verify(matchService).sortMatches(matches, "date");
    }

    // ==================== MY PROFILE ====================

    @Test
    @DisplayName("myProfile - should show profile when authenticated")
    void myProfile_shouldShowProfile_whenAuthenticated() {
        // Arrange
        when(userSessionService.getCurrentUser(session)).thenReturn(testUser);
        when(feedbackService.getFeedbacksByTargetUser(testUser)).thenReturn(Collections.emptyList());
        when(model.addAttribute(anyString(), any())).thenReturn(model);

        // Act
        String viewName = webController.myProfile(session, model);

        // Assert
        assertThat(viewName).isEqualTo("my-profile");
        verify(model).addAttribute(eq("currentUser"), eq(testUser));
        verify(feedbackService).getFeedbacksByTargetUser(testUser);
    }

    // ==================== USERS LIST ====================

    @Test
    @DisplayName("users - should show users list when authenticated")
    void users_shouldShowUsersList_whenAuthenticated() {
        // Arrange
        List<User> users = Arrays.asList(testUser);
        when(userSessionService.getCurrentUser(session)).thenReturn(testUser);
        when(userService.getAllUsers()).thenReturn(users);
        when(model.addAttribute(anyString(), any())).thenReturn(model);

        // Act
        String viewName = webController.users(session, model);

        // Assert
        assertThat(viewName).isEqualTo("users");
        verify(model).addAttribute(eq("users"), eq(users));
        verify(userService).getAllUsers();
    }
}
