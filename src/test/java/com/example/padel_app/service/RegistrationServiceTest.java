package com.example.padel_app.service;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.Registration;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.model.enums.RegistrationStatus;
import com.example.padel_app.repository.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test completo per RegistrationService
 *
 * BUSINESS LOGIC TESTATA:
 * - Join match con validazioni (max 4 players, no duplicati)
 * - Leave match con logica creatore vs normale
 * - Query registrazioni per user/match/status
 * - Contatori attivi vs totali
 * - Riuso registration CANCELLED (fix unique constraint)
 *
 * PATTERN UTILIZZATI:
 * - AAA (Arrange-Act-Assert)
 * - Mock isolation (no DB, no Spring)
 * - Edge case testing (partita piena, già iscritto, ecc.)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationService Unit Tests")
class RegistrationServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private MatchService matchService;

    @InjectMocks
    private RegistrationService registrationService;

    private User testUser;
    private Match testMatch;
    private Registration testRegistration;

    @BeforeEach
    void setUp() {
        // User setup
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("alice");
        testUser.setDeclaredLevel(Level.INTERMEDIO);

        // Match setup
        testMatch = new Match();
        testMatch.setId(1L);
        testMatch.setLocation("Campo 1");
        testMatch.setStatus(MatchStatus.WAITING);
        testMatch.setRequiredLevel(Level.INTERMEDIO);
        testMatch.setDateTime(LocalDateTime.now().plusDays(1));
        testMatch.setCreator(testUser);

        // Registration setup
        testRegistration = new Registration();
        testRegistration.setId(1L);
        testRegistration.setUser(testUser);
        testRegistration.setMatch(testMatch);
        testRegistration.setStatus(RegistrationStatus.JOINED);
        testRegistration.setRegisteredAt(LocalDateTime.now());
    }

    // ==================== QUERY METHODS ====================

    @Test
    @DisplayName("getRegistrationsByUser - should return all user registrations")
    void getRegistrationsByUser_shouldReturnAllRegistrations() {
        // Arrange
        List<Registration> expected = Arrays.asList(testRegistration);
        when(registrationRepository.findByUser(testUser)).thenReturn(expected);

        // Act
        List<Registration> result = registrationService.getRegistrationsByUser(testUser);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser()).isEqualTo(testUser);
        verify(registrationRepository).findByUser(testUser);
    }

    @Test
    @DisplayName("getRegistrationsByMatch - should return all match registrations")
    void getRegistrationsByMatch_shouldReturnAllRegistrations() {
        // Arrange
        when(registrationRepository.findByMatch(testMatch)).thenReturn(Arrays.asList(testRegistration));

        // Act
        List<Registration> result = registrationService.getRegistrationsByMatch(testMatch);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMatch()).isEqualTo(testMatch);
        verify(registrationRepository).findByMatch(testMatch);
    }

    @Test
    @DisplayName("getActiveRegistrationsByMatch - should return only JOINED registrations")
    void getActiveRegistrationsByMatch_shouldReturnOnlyJoined() {
        // Arrange
        when(registrationRepository.findByMatchAndStatus(testMatch, RegistrationStatus.JOINED))
            .thenReturn(Arrays.asList(testRegistration));

        // Act
        List<Registration> result = registrationService.getActiveRegistrationsByMatch(testMatch);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(r -> r.getStatus() == RegistrationStatus.JOINED);
        verify(registrationRepository).findByMatchAndStatus(testMatch, RegistrationStatus.JOINED);
    }

    @Test
    @DisplayName("isUserRegisteredForMatch - should return true when user is JOINED")
    void isUserRegisteredForMatch_shouldReturnTrue_whenJoined() {
        // Arrange
        when(registrationRepository.existsByUserAndMatchAndStatus(testUser, testMatch, RegistrationStatus.JOINED))
            .thenReturn(true);

        // Act
        boolean result = registrationService.isUserRegisteredForMatch(testUser, testMatch);

        // Assert
        assertThat(result).isTrue();
        verify(registrationRepository).existsByUserAndMatchAndStatus(testUser, testMatch, RegistrationStatus.JOINED);
    }

    @Test
    @DisplayName("isUserRegisteredForMatch - should return false when user is CANCELLED")
    void isUserRegisteredForMatch_shouldReturnFalse_whenCancelled() {
        // Arrange
        when(registrationRepository.existsByUserAndMatchAndStatus(testUser, testMatch, RegistrationStatus.JOINED))
            .thenReturn(false);

        // Act
        boolean result = registrationService.isUserRegisteredForMatch(testUser, testMatch);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getActiveRegistrationsCount - should count only JOINED registrations")
    void getActiveRegistrationsCount_shouldCountOnlyJoined() {
        // Arrange
        when(registrationRepository.countActiveRegistrationsByMatch(testMatch)).thenReturn(3);

        // Act
        int count = registrationService.getActiveRegistrationsCount(testMatch);

        // Assert
        assertThat(count).isEqualTo(3);
        verify(registrationRepository).countActiveRegistrationsByMatch(testMatch);
    }

    @Test
    @DisplayName("getAllRegistrationsCount - should count all registrations (JOINED + CANCELLED)")
    void getAllRegistrationsCount_shouldCountAll() {
        // Arrange
        when(registrationRepository.countAllRegistrationsByMatch(testMatch)).thenReturn(5);

        // Act
        int count = registrationService.getAllRegistrationsCount(testMatch);

        // Assert
        assertThat(count).isEqualTo(5);
        verify(registrationRepository).countAllRegistrationsByMatch(testMatch);
    }

    // ==================== JOIN MATCH - SUCCESS CASES ====================

    @Test
    @DisplayName("joinMatch - should create new registration successfully")
    void joinMatch_shouldCreateNewRegistration_whenValid() {
        // Arrange
        when(registrationRepository.existsByUserAndMatchAndStatus(testUser, testMatch, RegistrationStatus.JOINED))
            .thenReturn(false);
        when(registrationRepository.countActiveRegistrationsByMatch(testMatch)).thenReturn(2);
        when(registrationRepository.findByUserAndMatchAndStatus(testUser, testMatch, RegistrationStatus.CANCELLED))
            .thenReturn(Optional.empty());
        when(registrationRepository.save(any(Registration.class))).thenReturn(testRegistration);
        doNothing().when(matchService).checkAndConfirmMatch(testMatch);

        // Act
        Registration result = registrationService.joinMatch(testUser, testMatch);

        // Assert
        assertThat(result).isNotNull();
        verify(registrationRepository).save(any(Registration.class));
        verify(matchService).checkAndConfirmMatch(testMatch);
    }

    @Test
    @DisplayName("joinMatch - should reactivate CANCELLED registration")
    void joinMatch_shouldReactivateCancelled_whenExists() {
        // Arrange: utente aveva registration CANCELLED che viene riusata
        Registration cancelledReg = new Registration();
        cancelledReg.setUser(testUser);
        cancelledReg.setMatch(testMatch);
        cancelledReg.setStatus(RegistrationStatus.CANCELLED);

        when(registrationRepository.existsByUserAndMatchAndStatus(testUser, testMatch, RegistrationStatus.JOINED))
            .thenReturn(false);
        when(registrationRepository.countActiveRegistrationsByMatch(testMatch)).thenReturn(2);
        when(registrationRepository.findByUserAndMatchAndStatus(testUser, testMatch, RegistrationStatus.CANCELLED))
            .thenReturn(Optional.of(cancelledReg));
        when(registrationRepository.save(cancelledReg)).thenReturn(cancelledReg);

        // Act
        Registration result = registrationService.joinMatch(testUser, testMatch);

        // Assert
        assertThat(result.getStatus()).isEqualTo(RegistrationStatus.JOINED);
        verify(registrationRepository).save(cancelledReg);
        verify(matchService).checkAndConfirmMatch(testMatch);
    }

    // ==================== JOIN MATCH - VALIDATION FAILURES ====================

    @Test
    @DisplayName("joinMatch - should throw exception when already registered")
    void joinMatch_shouldThrowException_whenAlreadyRegistered() {
        // Arrange
        when(registrationRepository.existsByUserAndMatchAndStatus(testUser, testMatch, RegistrationStatus.JOINED))
            .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> registrationService.joinMatch(testUser, testMatch))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already registered");

        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("joinMatch - should throw exception when match is full")
    void joinMatch_shouldThrowException_whenMatchFull() {
        // Arrange
        when(registrationRepository.existsByUserAndMatchAndStatus(testUser, testMatch, RegistrationStatus.JOINED))
            .thenReturn(false);
        when(registrationRepository.countActiveRegistrationsByMatch(testMatch)).thenReturn(4);

        // Act & Assert
        assertThatThrownBy(() -> registrationService.joinMatch(testUser, testMatch))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("full");

        verify(registrationRepository, never()).save(any());
    }

    // ==================== LEAVE MATCH ====================

    @Test
    @DisplayName("leaveMatch - normal user should cancel registration")
    void leaveMatch_normalUser_shouldCancelRegistration() {
        // Arrange: user NON è il creatore
        User normalUser = new User();
        normalUser.setId(2L);
        normalUser.setUsername("bob");

        Registration reg = new Registration();
        reg.setUser(normalUser);
        reg.setMatch(testMatch);
        reg.setStatus(RegistrationStatus.JOINED);

        when(registrationRepository.findByUserAndMatch(normalUser, testMatch))
            .thenReturn(Optional.of(reg));
        when(registrationRepository.save(reg)).thenReturn(reg);
        when(registrationRepository.countActiveRegistrationsByMatch(testMatch)).thenReturn(2);

        // Act
        registrationService.leaveMatch(normalUser, testMatch);

        // Assert
        assertThat(reg.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
        verify(registrationRepository).save(reg);
        verify(matchService, never()).deleteMatch(any());
    }

    @Test
    @DisplayName("leaveMatch - creator should delete entire match")
    void leaveMatch_creator_shouldDeleteMatch() {
        // Arrange: testUser è il creatore del match
        when(registrationRepository.findByUserAndMatch(testUser, testMatch))
            .thenReturn(Optional.of(testRegistration));
        doNothing().when(matchService).deleteMatch(testMatch.getId());

        // Act
        registrationService.leaveMatch(testUser, testMatch);

        // Assert
        verify(matchService).deleteMatch(testMatch.getId());
        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("leaveMatch - should throw exception when not registered")
    void leaveMatch_shouldThrowException_whenNotRegistered() {
        // Arrange
        when(registrationRepository.findByUserAndMatch(testUser, testMatch))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> registrationService.leaveMatch(testUser, testMatch))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not registered");
    }

    @Test
    @DisplayName("leaveMatch - should throw exception when already left")
    void leaveMatch_shouldThrowException_whenAlreadyCancelled() {
        // Arrange
        testRegistration.setStatus(RegistrationStatus.CANCELLED);
        when(registrationRepository.findByUserAndMatch(testUser, testMatch))
            .thenReturn(Optional.of(testRegistration));

        // Act & Assert
        assertThatThrownBy(() -> registrationService.leaveMatch(testUser, testMatch))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already left");
    }

    // ==================== CRUD OPERATIONS ====================

    @Test
    @DisplayName("saveRegistration - should save registration")
    void saveRegistration_shouldSaveRegistration() {
        // Arrange
        when(registrationRepository.save(testRegistration)).thenReturn(testRegistration);

        // Act
        Registration result = registrationService.saveRegistration(testRegistration);

        // Assert
        assertThat(result).isEqualTo(testRegistration);
        verify(registrationRepository).save(testRegistration);
    }

    @Test
    @DisplayName("deleteRegistration - should delete by ID")
    void deleteRegistration_shouldDeleteById() {
        // Arrange
        doNothing().when(registrationRepository).deleteById(1L);

        // Act
        registrationService.deleteRegistration(1L);

        // Assert
        verify(registrationRepository).deleteById(1L);
    }

    @Test
    @DisplayName("getActiveRegistrationsByUser - should return only JOINED for user")
    void getActiveRegistrationsByUser_shouldReturnOnlyJoined() {
        // Arrange
        when(registrationRepository.findByUserAndStatus(testUser, RegistrationStatus.JOINED))
            .thenReturn(Arrays.asList(testRegistration));

        // Act
        List<Registration> result = registrationService.getActiveRegistrationsByUser(testUser);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).allMatch(r -> r.getStatus() == RegistrationStatus.JOINED);
        verify(registrationRepository).findByUserAndStatus(testUser, RegistrationStatus.JOINED);
    }
}
