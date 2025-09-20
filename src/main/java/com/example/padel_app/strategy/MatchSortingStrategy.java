package com.example.padel_app.strategy;

import com.example.padel_app.model.Match;

import java.util.List;

/**
 * Strategy Pattern Interface - Definisce le strategie di ordinamento partite
 */
public interface MatchSortingStrategy {
    List<Match> sort(List<Match> matches);
    String getStrategyName();
}