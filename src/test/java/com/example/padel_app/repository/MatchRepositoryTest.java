package com.example.padel_app.repository;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.Registration;
import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.model.enums.MatchType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository Test (Slice Test) per MatchRepository.
 * 
 * OBIETTIVO:
 * Testare le query JPQL complesse e i Derived Query Methods.
 * Usa @DataJpaTest che carica SOLO il layer di persistenza (più leggero di @SpringBootTest).
 */
@DataJpaTest
class MatchRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MatchRepository matchRepository;

    @Test
    @DisplayName("findAllOrderByPopularity: deve ordinare per numero di iscritti decrescente")
    void testFindAllOrderByPopularity() {
        // GIVEN
        User user = new User();
        user.setUsername("u1"); user.setEmail("u1@test.com"); user.setPassword("pwd");
        user.setFirstName("F"); user.setLastName("L"); user.setDeclaredLevel(Level.INTERMEDIO);
        entityManager.persist(user);

        // Match 1: 2 registrations
        Match popularMatch = createMatch("Popular", MatchStatus.WAITING);
        entityManager.persist(popularMatch);
        createRegistration(user, popularMatch);
        createRegistration(user, popularMatch); // Tecnicamente duplicato, ma per il count va bene nel test

        // Match 2: 0 registrations
        Match unpopularMatch = createMatch("Unpopular", MatchStatus.WAITING);
        entityManager.persist(unpopularMatch);

        entityManager.flush();

        // WHEN
        List<Match> result = matchRepository.findAllOrderByPopularity();

        // THEN
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getLocation()).isEqualTo("Popular");
        assertThat(result.get(1).getLocation()).isEqualTo("Unpopular");
    }

    @Test
    @DisplayName("findByStatusAndDateTimeAfter: deve trovare solo match futuri con status corretto")
    void testFindByStatusAndDateTimeAfter() {
        // GIVEN
        Match futureConfirmed = createMatch("Future Confirmed", MatchStatus.CONFIRMED);
        futureConfirmed.setDateTime(LocalDateTime.now().plusDays(1));
        entityManager.persist(futureConfirmed);

        Match pastConfirmed = createMatch("Past Confirmed", MatchStatus.CONFIRMED);
        pastConfirmed.setDateTime(LocalDateTime.now().minusDays(1));
        entityManager.persist(pastConfirmed);

        Match futureWaiting = createMatch("Future Waiting", MatchStatus.WAITING);
        futureWaiting.setDateTime(LocalDateTime.now().plusDays(1));
        entityManager.persist(futureWaiting);

        entityManager.flush();

        // WHEN
        List<Match> result = matchRepository.findByStatusAndDateTimeAfter(MatchStatus.CONFIRMED, LocalDateTime.now());

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLocation()).isEqualTo("Future Confirmed");
    }

    @Test
    @DisplayName("findAllWithCreator: deve caricare i match")
    void testFindAllWithCreator() {
        // GIVEN
        User creator = new User();
        creator.setUsername("creator"); creator.setEmail("c@test.com"); creator.setPassword("pwd");
        creator.setFirstName("C"); creator.setLastName("L"); creator.setDeclaredLevel(Level.AVANZATO);
        entityManager.persist(creator);

        Match match = createMatch("Match with Creator", MatchStatus.WAITING);
        match.setCreator(creator);
        entityManager.persist(match);

        entityManager.flush();
        entityManager.clear(); // Pulisce la cache per forzare la query DB

        // WHEN
        List<Match> result = matchRepository.findAllWithCreator();

        // THEN
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getCreator()).isNotNull();
        assertThat(result.get(0).getCreator().getUsername()).isEqualTo("creator");
    }

    // Helpers
    private Match createMatch(String location, MatchStatus status) {
        Match m = new Match();
        m.setLocation(location);
        m.setStatus(status);
        m.setDateTime(LocalDateTime.now());
        m.setRequiredLevel(Level.INTERMEDIO);
        m.setType(MatchType.FISSA);
        m.setCreatedAt(LocalDateTime.now());
        return m;
    }

    private void createRegistration(User user, Match match) {
        Registration r = new Registration();
        r.setUser(user);
        r.setMatch(match);
        entityManager.persist(r);
    }
}
