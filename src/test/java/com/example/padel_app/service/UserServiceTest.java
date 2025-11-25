package com.example.padel_app.service;

import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test completo per UserService
 *
 * OBIETTIVO: Coverage > 80% del service layer
 *
 * PATTERN DI TEST:
 * - Unit test con Mockito (mock del repository)
 * - AAA pattern: Arrange, Act, Assert
 * - Test isolati (no database reale, no Spring context)
 *
 * CONCETTI DIMOSTRATI:
 * - @ExtendWith(MockitoExtension.class): abilita Mockito in JUnit 5
 * - @Mock: crea mock del repository
 * - @InjectMocks: inietta mock nel service
 * - when().thenReturn(): stubbing behavior del mock
 * - verify(): verifica chiamate ai metodi mock
 * - assertThat(): assertions fluent (AssertJ)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    /**
     * Setup eseguito prima di ogni test
     * Crea un utente di test con dati standard
     */
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("alice");
        testUser.setEmail("alice@example.com");
        testUser.setPassword("hashedPassword123");
        testUser.setDeclaredLevel(Level.INTERMEDIO);
        testUser.setPerceivedLevel(Level.INTERMEDIO);
        testUser.setMatchesPlayed(5);
    }

    // ==================== CRUD READ OPERATIONS ====================

    @Test
    @DisplayName("getAllUsers - should return all users")
    void getAllUsers_shouldReturnAllUsers() {
        // Arrange: prepara mock data
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("bob");
        List<User> expectedUsers = Arrays.asList(testUser, user2);

        when(userRepository.findAll()).thenReturn(expectedUsers);

        // Act: esegui metodo da testare
        List<User> result = userService.getAllUsers();

        // Assert: verifica risultato
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(testUser, user2);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("getUserById - should return user when exists")
    void getUserById_shouldReturnUser_whenExists() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.getUserById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
        assertThat(result.get().getUsername()).isEqualTo("alice");
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("getUserById - should return empty when not exists")
    void getUserById_shouldReturnEmpty_whenNotExists() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.getUserById(999L);

        // Assert
        assertThat(result).isEmpty();
        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("getUserByUsername - should return user when exists")
    void getUserByUsername_shouldReturnUser_whenExists() {
        // Arrange
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.getUserByUsername("alice");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("alice");
        verify(userRepository).findByUsername("alice");
    }

    @Test
    @DisplayName("getUserByEmail - should return user when exists")
    void getUserByEmail_shouldReturnUser_whenExists() {
        // Arrange
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.getUserByEmail("alice@example.com");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("alice@example.com");
        verify(userRepository).findByEmail("alice@example.com");
    }

    @Test
    @DisplayName("getUsersByDeclaredLevel - should filter by declared level")
    void getUsersByDeclaredLevel_shouldFilterByLevel() {
        // Arrange
        User user2 = new User();
        user2.setDeclaredLevel(Level.INTERMEDIO);
        List<User> intermediateUsers = Arrays.asList(testUser, user2);

        when(userRepository.findByDeclaredLevel(Level.INTERMEDIO)).thenReturn(intermediateUsers);

        // Act
        List<User> result = userService.getUsersByDeclaredLevel(Level.INTERMEDIO);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(u -> u.getDeclaredLevel() == Level.INTERMEDIO);
        verify(userRepository).findByDeclaredLevel(Level.INTERMEDIO);
    }

    @Test
    @DisplayName("getUsersByPerceivedLevel - should filter by perceived level")
    void getUsersByPerceivedLevel_shouldFilterByLevel() {
        // Arrange
        when(userRepository.findByPerceivedLevel(Level.AVANZATO)).thenReturn(Arrays.asList(testUser));

        // Act
        List<User> result = userService.getUsersByPerceivedLevel(Level.AVANZATO);

        // Assert
        assertThat(result).hasSize(1);
        verify(userRepository).findByPerceivedLevel(Level.AVANZATO);
    }

    @Test
    @DisplayName("getUsersOrderByMatchesPlayed - should return ordered list")
    void getUsersOrderByMatchesPlayed_shouldReturnOrderedList() {
        // Arrange
        User user1 = new User();
        user1.setMatchesPlayed(10);
        User user2 = new User();
        user2.setMatchesPlayed(5);
        List<User> orderedUsers = Arrays.asList(user1, user2);

        when(userRepository.findAllOrderByMatchesPlayedDesc()).thenReturn(orderedUsers);

        // Act
        List<User> result = userService.getUsersOrderByMatchesPlayed();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMatchesPlayed()).isGreaterThanOrEqualTo(result.get(1).getMatchesPlayed());
        verify(userRepository).findAllOrderByMatchesPlayedDesc();
    }

    // ==================== CRUD WRITE OPERATIONS ====================

    @Test
    @DisplayName("saveUser - should save new user")
    void saveUser_shouldSaveNewUser() {
        // Arrange
        User newUser = new User();
        newUser.setUsername("charlie");

        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Act
        User result = userService.saveUser(newUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("charlie");
        verify(userRepository).save(newUser);
    }

    @Test
    @DisplayName("saveUser - should update existing user")
    void saveUser_shouldUpdateExistingUser() {
        // Arrange
        testUser.setEmail("newemail@example.com");
        when(userRepository.save(testUser)).thenReturn(testUser);

        // Act
        User result = userService.saveUser(testUser);

        // Assert
        assertThat(result.getEmail()).isEqualTo("newemail@example.com");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("deleteUser - should delete user by ID")
    void deleteUser_shouldDeleteUserById() {
        // Arrange
        doNothing().when(userRepository).deleteById(1L);

        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userRepository).deleteById(1L);
    }

    // ==================== BUSINESS LOGIC OPERATIONS ====================

    @Test
    @DisplayName("incrementMatchesPlayed - should increment counter")
    void incrementMatchesPlayed_shouldIncrementCounter() {
        // Arrange
        int initialCount = testUser.getMatchesPlayed();
        when(userRepository.save(testUser)).thenReturn(testUser);

        // Act
        User result = userService.incrementMatchesPlayed(testUser);

        // Assert
        assertThat(result.getMatchesPlayed()).isEqualTo(initialCount + 1);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updatePerceivedLevel - should update perceived level")
    void updatePerceivedLevel_shouldUpdateLevel() {
        // Arrange
        when(userRepository.save(testUser)).thenReturn(testUser);

        // Act
        User result = userService.updatePerceivedLevel(testUser, Level.AVANZATO);

        // Assert
        assertThat(result.getPerceivedLevel()).isEqualTo(Level.AVANZATO);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updateDeclaredLevel - should update declared level")
    void updateDeclaredLevel_shouldUpdateLevel() {
        // Arrange
        when(userRepository.save(testUser)).thenReturn(testUser);

        // Act
        User result = userService.updateDeclaredLevel(testUser, Level.PRINCIPIANTE);

        // Assert
        assertThat(result.getDeclaredLevel()).isEqualTo(Level.PRINCIPIANTE);
        verify(userRepository).save(testUser);
    }

    // ==================== VALIDATION OPERATIONS ====================

    @Test
    @DisplayName("existsByUsername - should return true when username exists")
    void existsByUsername_shouldReturnTrue_whenExists() {
        // Arrange
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        // Act
        boolean result = userService.existsByUsername("alice");

        // Assert
        assertThat(result).isTrue();
        verify(userRepository).existsByUsername("alice");
    }

    @Test
    @DisplayName("existsByUsername - should return false when username not exists")
    void existsByUsername_shouldReturnFalse_whenNotExists() {
        // Arrange
        when(userRepository.existsByUsername("nonexistent")).thenReturn(false);

        // Act
        boolean result = userService.existsByUsername("nonexistent");

        // Assert
        assertThat(result).isFalse();
        verify(userRepository).existsByUsername("nonexistent");
    }

    @Test
    @DisplayName("existsByEmail - should return true when email exists")
    void existsByEmail_shouldReturnTrue_whenExists() {
        // Arrange
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        // Act
        boolean result = userService.existsByEmail("alice@example.com");

        // Assert
        assertThat(result).isTrue();
        verify(userRepository).existsByEmail("alice@example.com");
    }

    @Test
    @DisplayName("existsByEmail - should return false when email not exists")
    void existsByEmail_shouldReturnFalse_whenNotExists() {
        // Arrange
        when(userRepository.existsByEmail("unknown@example.com")).thenReturn(false);

        // Act
        boolean result = userService.existsByEmail("unknown@example.com");

        // Assert
        assertThat(result).isFalse();
        verify(userRepository).existsByEmail("unknown@example.com");
    }
}
