package com.example.padel_app.strategy;

import com.example.padel_app.model.Match;
import com.example.padel_app.model.Registration;
import com.example.padel_app.model.enums.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StrategyPatternTest {

    private List<Match> matches;
    private Match match1;
    private Match match2;
    private Match match3;

    @BeforeEach
    void setUp() {
        match1 = new Match();
        match1.setId(1L);
        match1.setLocation("Location 1");
        match1.setDateTime(LocalDateTime.now().plusDays(3));
        match1.setRequiredLevel(Level.AVANZATO);
        match1.setRegistrations(createRegistrations(2)); // 2 players

        match2 = new Match();
        match2.setId(2L);
        match2.setLocation("Location 2");
        match2.setDateTime(LocalDateTime.now().plusDays(1));
        match2.setRequiredLevel(Level.PRINCIPIANTE);
        match2.setRegistrations(createRegistrations(4)); // 4 players

        match3 = new Match();
        match3.setId(3L);
        match3.setLocation("Location 3");
        match3.setDateTime(LocalDateTime.now().plusDays(2));
        match3.setRequiredLevel(Level.INTERMEDIO);
        match3.setRegistrations(createRegistrations(1)); // 1 player

        matches = new ArrayList<>(Arrays.asList(match1, match2, match3));
    }

    private List<Registration> createRegistrations(int count) {
        List<Registration> regs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            regs.add(new Registration());
        }
        return regs;
    }

    @Test
    void testDateSorting_AscendingOrder() {
        MatchSortingStrategy strategy = new DateSortingStrategy();
        List<Match> sorted = strategy.sort(matches);

        assertEquals(3, sorted.size());
        assertEquals(match2.getId(), sorted.get(0).getId()); // Day 1
        assertEquals(match3.getId(), sorted.get(1).getId()); // Day 2
        assertEquals(match1.getId(), sorted.get(2).getId()); // Day 3
    }

    @Test
    void testPopularitySorting_DescendingOrder() {
        MatchSortingStrategy strategy = new PopularitySortingStrategy();
        List<Match> sorted = strategy.sort(matches);

        assertEquals(3, sorted.size());
        assertEquals(match2.getId(), sorted.get(0).getId()); // 4 players
        assertEquals(match1.getId(), sorted.get(1).getId()); // 2 players
        assertEquals(match3.getId(), sorted.get(2).getId()); // 1 player
    }

    @Test
    void testLevelSorting_AscendingOrder() {
        MatchSortingStrategy strategy = new LevelSortingStrategy();
        List<Match> sorted = strategy.sort(matches);

        assertEquals(3, sorted.size());
        assertEquals(Level.PRINCIPIANTE, sorted.get(0).getRequiredLevel());
        assertEquals(Level.INTERMEDIO, sorted.get(1).getRequiredLevel());
        assertEquals(Level.AVANZATO, sorted.get(2).getRequiredLevel());
    }

    @Test
    void testStrategyInterchangeability() {
        // Test that all strategies work with same input
        List<MatchSortingStrategy> strategies = Arrays.asList(
            new DateSortingStrategy(),
            new PopularitySortingStrategy(),
            new LevelSortingStrategy()
        );

        for (MatchSortingStrategy strategy : strategies) {
            List<Match> sorted = strategy.sort(new ArrayList<>(matches));
            assertNotNull(sorted);
            assertEquals(3, sorted.size());
        }
    }

    @Test
    void testEmptyList() {
        List<Match> emptyList = new ArrayList<>();
        
        MatchSortingStrategy dateStrategy = new DateSortingStrategy();
        List<Match> sorted = dateStrategy.sort(emptyList);

        assertNotNull(sorted);
        assertTrue(sorted.isEmpty());
    }

    @Test
    void testSingleMatch() {
        List<Match> singleMatch = Arrays.asList(match1);
        
        MatchSortingStrategy strategy = new PopularitySortingStrategy();
        List<Match> sorted = strategy.sort(singleMatch);

        assertEquals(1, sorted.size());
        assertEquals(match1.getId(), sorted.get(0).getId());
    }
}
