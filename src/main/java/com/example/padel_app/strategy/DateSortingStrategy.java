package com.example.padel_app.strategy;

import com.example.padel_app.model.Match;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component("dateSorting")
public class DateSortingStrategy implements MatchSortingStrategy {
    
    @Override
    public List<Match> sort(List<Match> matches) {
        return matches.stream()
                .sorted(Comparator.comparing(Match::getDateTime))
                .toList();
    }
    
    @Override
    public String getStrategyName() {
        return "Data";
    }
}