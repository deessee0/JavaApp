package com.example.padel_app.model.enums;

/**
 * Enum MatchType - Tipologia di partita
 * 
 * BUSINESS LOGIC:
 * 
 * FISSA (Partita Fissa):
 * - Data, ora e luogo già stabiliti
 * - Esempio: "Sabato 15/10/2025 ore 18:00 - Tennis Club Milano"
 * - I giocatori si iscrivono sapendo già quando giocare
 * - Più comune in contesti organizzati
 * 
 * PROPOSTA (Partita Proposta):
 * - Data/ora da concordare tra i 4 giocatori dopo
 * - Esempio: "Padel in zona Navigli - giorno da decidere"
 * - Utile per trovare compagni, poi si organizza in chat
 * - Più flessibile ma richiede coordinamento
 * 
 * USO NELL'APP:
 * Attualmente entrambi i tipi usano dateTime obbligatorio.
 * In futuro PROPOSTA potrebbe avere dateTime opzionale/da-definire.
 */
public enum MatchType {
    FISSA("Partita Fissa"),         // Data/ora già definite
    PROPOSTA("Partita Proposta");   // Data/ora da concordare
    
    private final String displayName;
    
    MatchType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
