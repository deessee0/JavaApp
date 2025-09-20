package com.example.padel_app.model.enums;

public enum RegistrationStatus {
    JOINED("Iscritto"),
    CANCELLED("Cancellato");
    
    private final String displayName;
    
    RegistrationStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}