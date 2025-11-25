package com.example.padel_app.service;

import com.example.padel_app.model.User;
import com.example.padel_app.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Test completo per UserSessionService
 *
 * BUSINESS LOGIC TESTATA:
 * - Session management (save/retrieve/clear user)
 * - Authentication verification
 * - Edge cases (user deleted, null values, session invalidation)
 *
 * CONCETTI DIMOSTRATI:
 * - Mock di HttpSession (interfaccia Servlet API)
 * - Test di session attributes
 * - Verifica chiamate a session.invalidate()
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserSessionService Unit Tests")
class UserSessionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpSession session;

    @InjectMocks
    private UserSessionService userSessionService;

    private User testUser;
    private static final String SESSION_KEY = "currentUserId";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("alice");
        testUser.setEmail("alice@example.com");
    }

    // ==================== GET CURRENT USER ====================

    @Test
    @DisplayName("getCurrentUser - should return user when authenticated")
    void getCurrentUser_shouldReturnUser_whenAuthenticated() {
        // Arrange: sessione contiene user ID
        when(session.getAttribute(SESSION_KEY)).thenReturn(testUser.getId());
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        // Act
        User result = userSessionService.getCurrentUser(session);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUser.getId());
        assertThat(result.getUsername()).isEqualTo("alice");
        verify(session).getAttribute(SESSION_KEY);
        verify(userRepository).findById(testUser.getId());
    }

    @Test
    @DisplayName("getCurrentUser - should return null when no session attribute")
    void getCurrentUser_shouldReturnNull_whenNoSessionAttribute() {
        // Arrange: sessione vuota (utente non loggato)
        when(session.getAttribute(SESSION_KEY)).thenReturn(null);

        // Act
        User result = userSessionService.getCurrentUser(session);

        // Assert
        assertThat(result).isNull();
        verify(session).getAttribute(SESSION_KEY);
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getCurrentUser - should return null and clear session when user not found in DB")
    void getCurrentUser_shouldReturnNullAndClearSession_whenUserDeleted() {
        // Arrange: ID in sessione ma user cancellato dal DB
        when(session.getAttribute(SESSION_KEY)).thenReturn(999L);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        User result = userSessionService.getCurrentUser(session);

        // Assert
        assertThat(result).isNull();
        verify(session).removeAttribute(SESSION_KEY); // Pulisce sessione inconsistente
        verify(userRepository).findById(999L);
    }

    // ==================== SET CURRENT USER ====================

    @Test
    @DisplayName("setCurrentUser - should save user ID in session")
    void setCurrentUser_shouldSaveUserIdInSession() {
        // Arrange
        when(session.getId()).thenReturn("SESSION123");
        doNothing().when(session).setAttribute(SESSION_KEY, testUser.getId());

        // Act
        userSessionService.setCurrentUser(session, testUser);

        // Assert
        verify(session).setAttribute(SESSION_KEY, testUser.getId());
        verify(session).getId(); // Per logging
    }

    @Test
    @DisplayName("setCurrentUser - should throw exception when user is null")
    void setCurrentUser_shouldThrowException_whenUserNull() {
        // Act & Assert
        assertThatThrownBy(() -> userSessionService.setCurrentUser(session, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be null");

        verify(session, never()).setAttribute(anyString(), any());
    }

    // ==================== CLEAR SESSION ====================

    @Test
    @DisplayName("clearSession - should invalidate session")
    void clearSession_shouldInvalidateSession() {
        // Arrange
        when(session.getId()).thenReturn("SESSION123");
        when(session.getAttribute(SESSION_KEY)).thenReturn(testUser.getId());
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        doNothing().when(session).invalidate();

        // Act
        userSessionService.clearSession(session);

        // Assert
        verify(session).invalidate();
        verify(session).getId(); // Per logging
    }

    @Test
    @DisplayName("clearSession - should invalidate even when no user in session")
    void clearSession_shouldInvalidate_whenNoUser() {
        // Arrange: sessione vuota
        when(session.getId()).thenReturn("SESSION123");
        when(session.getAttribute(SESSION_KEY)).thenReturn(null);
        doNothing().when(session).invalidate();

        // Act
        userSessionService.clearSession(session);

        // Assert
        verify(session).invalidate();
    }

    // ==================== IS AUTHENTICATED ====================

    @Test
    @DisplayName("isAuthenticated - should return true when user in session")
    void isAuthenticated_shouldReturnTrue_whenUserInSession() {
        // Arrange
        when(session.getAttribute(SESSION_KEY)).thenReturn(testUser.getId());
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        // Act
        boolean result = userSessionService.isAuthenticated(session);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isAuthenticated - should return false when no user in session")
    void isAuthenticated_shouldReturnFalse_whenNoUserInSession() {
        // Arrange
        when(session.getAttribute(SESSION_KEY)).thenReturn(null);

        // Act
        boolean result = userSessionService.isAuthenticated(session);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isAuthenticated - should return false when user deleted from DB")
    void isAuthenticated_shouldReturnFalse_whenUserDeletedFromDB() {
        // Arrange
        when(session.getAttribute(SESSION_KEY)).thenReturn(999L);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        boolean result = userSessionService.isAuthenticated(session);

        // Assert
        assertThat(result).isFalse();
    }

    // ==================== SESSION LIFECYCLE INTEGRATION ====================

    @Test
    @DisplayName("Session lifecycle - login, check, logout flow")
    void sessionLifecycle_shouldWorkCorrectly() {
        // 1. LOGIN: salva user in sessione
        when(session.getId()).thenReturn("SESSION123");
        doNothing().when(session).setAttribute(SESSION_KEY, testUser.getId());

        userSessionService.setCurrentUser(session, testUser);
        verify(session).setAttribute(SESSION_KEY, testUser.getId());

        // 2. CHECK AUTHENTICATION: verifica utente loggato
        when(session.getAttribute(SESSION_KEY)).thenReturn(testUser.getId());
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        boolean isAuth = userSessionService.isAuthenticated(session);
        assertThat(isAuth).isTrue();

        User currentUser = userSessionService.getCurrentUser(session);
        assertThat(currentUser).isNotNull();
        assertThat(currentUser.getUsername()).isEqualTo("alice");

        // 3. LOGOUT: pulisce sessione
        doNothing().when(session).invalidate();
        userSessionService.clearSession(session);
        verify(session).invalidate();
    }
}
