package com.example.padel_app.service;

import com.example.padel_app.model.Feedback;
import com.example.padel_app.model.Match;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.repository.FeedbackRepository;
import com.example.padel_app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test completo per FeedbackService
 *
 * BUSINESS LOGIC TESTATA:
 * - Creazione feedback con validazione unicità
 * - Calcolo perceived level (media aritmetica feedback)
 * - Query feedback per author/target/match
 * - Edge cases: nessun feedback, user non trovato, duplicati
 *
 * ALGORITMO PERCEIVED LEVEL:
 * - Conversione enum → ordinal (PRINCIPIANTE=0, INTERMEDIO=1, AVANZATO=2, PROFESSIONISTA=3)
 * - Media aritmetica + arrotondamento
 * - Esempio: [INTERMEDIO, AVANZATO, INTERMEDIO] → [1,2,1] → media=1.33 → round=1 → INTERMEDIO
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackService Unit Tests")
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    private User alice;
    private User bob;
    private Match testMatch;
    private Feedback testFeedback;

    @BeforeEach
    void setUp() {
        // Alice: author del feedback
        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        alice.setDeclaredLevel(Level.INTERMEDIO);

        // Bob: target del feedback
        bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");
        bob.setDeclaredLevel(Level.PRINCIPIANTE);
        bob.setPerceivedLevel(Level.PRINCIPIANTE);

        // Match
        testMatch = new Match();
        testMatch.setId(1L);
        testMatch.setLocation("Campo 1");

        // Feedback
        testFeedback = new Feedback();
        testFeedback.setId(1L);
        testFeedback.setAuthor(alice);
        testFeedback.setTargetUser(bob);
        testFeedback.setMatch(testMatch);
        testFeedback.setSuggestedLevel(Level.INTERMEDIO);
        testFeedback.setComment("Buon giocatore");
        testFeedback.setCreatedAt(LocalDateTime.now());
    }

    // ==================== CREATE FEEDBACK ====================

    @Test
    @DisplayName("createFeedback - should create new feedback successfully")
    void createFeedback_shouldCreateSuccessfully() {
        // Arrange
        when(feedbackRepository.findByAuthorAndTargetUserAndMatch(alice, bob, testMatch))
            .thenReturn(Optional.empty());
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(testFeedback);
        when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));
        when(feedbackRepository.findByTargetUser(bob)).thenReturn(Arrays.asList(testFeedback));
        when(userRepository.save(bob)).thenReturn(bob);

        // Act
        Feedback result = feedbackService.createFeedback(
            alice, bob, testMatch, Level.INTERMEDIO, "Buon giocatore"
        );

        // Assert
        assertThat(result).isNotNull();
        verify(feedbackRepository).save(any(Feedback.class));
        verify(userRepository).save(bob); // updatePerceivedLevel chiamato
    }

    @Test
    @DisplayName("createFeedback - should throw exception when feedback already exists")
    void createFeedback_shouldThrowException_whenAlreadyExists() {
        // Arrange: feedback già esistente
        when(feedbackRepository.findByAuthorAndTargetUserAndMatch(alice, bob, testMatch))
            .thenReturn(Optional.of(testFeedback));

        // Act & Assert
        assertThatThrownBy(() ->
            feedbackService.createFeedback(alice, bob, testMatch, Level.AVANZATO, "Test")
        )
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("already exists");

        verify(feedbackRepository, never()).save(any());
    }

    // ==================== UPDATE PERCEIVED LEVEL ====================

    @Test
    @DisplayName("updatePerceivedLevel - should calculate average correctly with single feedback")
    void updatePerceivedLevel_singleFeedback_shouldSetExactLevel() {
        // Arrange: 1 solo feedback AVANZATO
        Feedback feedback1 = createFeedback(alice, bob, testMatch, Level.AVANZATO);

        when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));
        when(feedbackRepository.findByTargetUser(bob)).thenReturn(Arrays.asList(feedback1));
        when(userRepository.save(bob)).thenReturn(bob);

        // Act
        feedbackService.updatePerceivedLevel(bob.getId());

        // Assert
        assertThat(bob.getPerceivedLevel()).isEqualTo(Level.AVANZATO);
        verify(userRepository).save(bob);
    }

    @Test
    @DisplayName("updatePerceivedLevel - should calculate average correctly with multiple feedbacks")
    void updatePerceivedLevel_multipleFeedbacks_shouldCalculateAverage() {
        // Arrange: 3 feedback → INTERMEDIO(1), AVANZATO(2), INTERMEDIO(1)
        // Media: (1+2+1)/3 = 1.33 → round = 1 → INTERMEDIO
        Feedback f1 = createFeedback(alice, bob, testMatch, Level.INTERMEDIO);
        Feedback f2 = createFeedback(alice, bob, testMatch, Level.AVANZATO);
        Feedback f3 = createFeedback(alice, bob, testMatch, Level.INTERMEDIO);

        when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));
        when(feedbackRepository.findByTargetUser(bob)).thenReturn(Arrays.asList(f1, f2, f3));
        when(userRepository.save(bob)).thenReturn(bob);

        // Act
        feedbackService.updatePerceivedLevel(bob.getId());

        // Assert
        assertThat(bob.getPerceivedLevel()).isEqualTo(Level.INTERMEDIO);
        verify(userRepository).save(bob);
    }

    @Test
    @DisplayName("updatePerceivedLevel - should round up correctly")
    void updatePerceivedLevel_shouldRoundUpCorrectly() {
        // Arrange: AVANZATO(2) + PROFESSIONISTA(3) → media=2.5 → round=3 → PROFESSIONISTA
        Feedback f1 = createFeedback(alice, bob, testMatch, Level.AVANZATO);
        Feedback f2 = createFeedback(alice, bob, testMatch, Level.PROFESSIONISTA);

        when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));
        when(feedbackRepository.findByTargetUser(bob)).thenReturn(Arrays.asList(f1, f2));
        when(userRepository.save(bob)).thenReturn(bob);

        // Act
        feedbackService.updatePerceivedLevel(bob.getId());

        // Assert
        assertThat(bob.getPerceivedLevel()).isEqualTo(Level.PROFESSIONISTA);
    }

    @Test
    @DisplayName("updatePerceivedLevel - should not update when no feedbacks")
    void updatePerceivedLevel_noFeedbacks_shouldNotUpdate() {
        // Arrange
        Level originalLevel = bob.getPerceivedLevel();
        when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));
        when(feedbackRepository.findByTargetUser(bob)).thenReturn(Collections.emptyList());

        // Act
        feedbackService.updatePerceivedLevel(bob.getId());

        // Assert
        assertThat(bob.getPerceivedLevel()).isEqualTo(originalLevel); // unchanged
        verify(userRepository, never()).save(bob);
    }

    @Test
    @DisplayName("updatePerceivedLevel - should handle user not found gracefully")
    void updatePerceivedLevel_userNotFound_shouldNotThrow() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert: no exception thrown
        feedbackService.updatePerceivedLevel(999L);

        verify(feedbackRepository, never()).findByTargetUser(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updatePerceivedLevel - should handle all PRINCIPIANTE feedbacks")
    void updatePerceivedLevel_allPrincipiante_shouldSetPrincipiante() {
        // Arrange: tutti feedback PRINCIPIANTE (ordinal=0)
        Feedback f1 = createFeedback(alice, bob, testMatch, Level.PRINCIPIANTE);
        Feedback f2 = createFeedback(alice, bob, testMatch, Level.PRINCIPIANTE);

        when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));
        when(feedbackRepository.findByTargetUser(bob)).thenReturn(Arrays.asList(f1, f2));
        when(userRepository.save(bob)).thenReturn(bob);

        // Act
        feedbackService.updatePerceivedLevel(bob.getId());

        // Assert
        assertThat(bob.getPerceivedLevel()).isEqualTo(Level.PRINCIPIANTE);
    }

    // ==================== QUERY METHODS ====================

    @Test
    @DisplayName("getFeedbacksByTargetUser - should return all feedbacks received")
    void getFeedbacksByTargetUser_shouldReturnAllReceived() {
        // Arrange
        when(feedbackRepository.findByTargetUser(bob)).thenReturn(Arrays.asList(testFeedback));

        // Act
        List<Feedback> result = feedbackService.getFeedbacksByTargetUser(bob);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTargetUser()).isEqualTo(bob);
        verify(feedbackRepository).findByTargetUser(bob);
    }

    @Test
    @DisplayName("getFeedbacksByAuthor - should return all feedbacks written")
    void getFeedbacksByAuthor_shouldReturnAllWritten() {
        // Arrange
        when(feedbackRepository.findByAuthor(alice)).thenReturn(Arrays.asList(testFeedback));

        // Act
        List<Feedback> result = feedbackService.getFeedbacksByAuthor(alice);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAuthor()).isEqualTo(alice);
        verify(feedbackRepository).findByAuthor(alice);
    }

    @Test
    @DisplayName("getFeedbacksByAuthorAndMatch - should filter by author and match")
    void getFeedbacksByAuthorAndMatch_shouldFilterCorrectly() {
        // Arrange
        when(feedbackRepository.findByAuthorAndMatch(alice, testMatch))
            .thenReturn(Arrays.asList(testFeedback));

        // Act
        List<Feedback> result = feedbackService.getFeedbacksByAuthorAndMatch(alice, testMatch);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAuthor()).isEqualTo(alice);
        assertThat(result.get(0).getMatch()).isEqualTo(testMatch);
        verify(feedbackRepository).findByAuthorAndMatch(alice, testMatch);
    }

    @Test
    @DisplayName("getFeedbacksByMatch - should return all feedbacks for match")
    void getFeedbacksByMatch_shouldReturnAllForMatch() {
        // Arrange
        when(feedbackRepository.findByMatch(testMatch)).thenReturn(Arrays.asList(testFeedback));

        // Act
        List<Feedback> result = feedbackService.getFeedbacksByMatch(testMatch);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMatch()).isEqualTo(testMatch);
        verify(feedbackRepository).findByMatch(testMatch);
    }

    @Test
    @DisplayName("getFeedback - should return specific feedback when exists")
    void getFeedback_shouldReturnFeedback_whenExists() {
        // Arrange
        when(feedbackRepository.findByAuthorAndTargetUserAndMatch(alice, bob, testMatch))
            .thenReturn(Optional.of(testFeedback));

        // Act
        Optional<Feedback> result = feedbackService.getFeedback(alice, bob, testMatch);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testFeedback);
        verify(feedbackRepository).findByAuthorAndTargetUserAndMatch(alice, bob, testMatch);
    }

    @Test
    @DisplayName("getFeedback - should return empty when not exists")
    void getFeedback_shouldReturnEmpty_whenNotExists() {
        // Arrange
        when(feedbackRepository.findByAuthorAndTargetUserAndMatch(alice, bob, testMatch))
            .thenReturn(Optional.empty());

        // Act
        Optional<Feedback> result = feedbackService.getFeedback(alice, bob, testMatch);

        // Assert
        assertThat(result).isEmpty();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Helper per creare feedback con livello specifico
     */
    private Feedback createFeedback(User author, User target, Match match, Level level) {
        Feedback feedback = new Feedback();
        feedback.setAuthor(author);
        feedback.setTargetUser(target);
        feedback.setMatch(match);
        feedback.setSuggestedLevel(level);
        feedback.setCreatedAt(LocalDateTime.now());
        return feedback;
    }
}
