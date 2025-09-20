package com.example.padel_app.model.enums;

public enum MatchStatus {
    WAITING("In Attesa"),
    CONFIRMED("Confermata"),
    FINISHED("Terminata"),
    CANCELLED("Annullata");
    
    private final String displayName;
    
    MatchStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}