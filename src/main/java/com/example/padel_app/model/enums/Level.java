package com.example.padel_app.model.enums;

public enum Level {
    PRINCIPIANTE("Principiante"),
    INTERMEDIO("Intermedio"), 
    AVANZATO("Avanzato"),
    PROFESSIONISTA("Professionista");
    
    private final String displayName;
    
    Level(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}