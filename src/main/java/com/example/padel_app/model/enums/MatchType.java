package com.example.padel_app.model.enums;

public enum MatchType {
    FISSA("Partita Fissa"),
    PROPOSTA("Partita Proposta");
    
    private final String displayName;
    
    MatchType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}