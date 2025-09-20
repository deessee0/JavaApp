package com.example.padel_app.strategy;

import com.example.padel_app.model.Match;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component("levelSorting")
public class LevelSortingStrategy implements MatchSortingStrategy {
    
    @Override
    public List<Match> sort(List<Match> matches) {
        return matches.stream()
                .sorted(Comparator.comparing(match -> match.getRequiredLevel().ordinal()))
                .toList();
    }
    
    @Override
    public String getStrategyName() {
        return "Livello";
    }
}