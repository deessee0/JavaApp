package com.example.padel_app.model.enums;

/**
 * Enum Level - Livelli di abilità nel padel
 * 
 * PATTERN: Enum con attributi custom (displayName)
 * 
 * PERCHÉ USARE ENUM invece di String?
 * ✅ Type safety: impossibile scrivere "intermedio" (minuscolo) per errore
 * ✅ Validazione automatica: solo valori predefiniti
 * ✅ Autocomplete IDE: vede subito i valori possibili
 * ✅ Refactoring sicuro: cambio INTERMEDIO → MEDIO e tutto si aggiorna
 * 
 * DISPLAYNAME:
 * Separazione tra:
 * - Valore tecnico: PRINCIPIANTE (uppercase, per codice Java)
 * - Valore UI: "Principiante" (user-friendly, per template Thymeleaf)
 * 
 * ORDINAMENTO:
 * L'ordine di dichiarazione è importante! 
 * ordinal() ritorna: PRINCIPIANTE=0, INTERMEDIO=1, AVANZATO=2, PROFESSIONISTA=3
 * Usato per sorting e confronti (livello minimo richiesto)
 */
public enum Level {
    PRINCIPIANTE("Principiante"),    // Ordinal 0: principiante assoluto
    INTERMEDIO("Intermedio"),         // Ordinal 1: gioca regolarmente
    AVANZATO("Avanzato"),            // Ordinal 2: tecnica solida
    PROFESSIONISTA("Professionista"); // Ordinal 3: gioca a livello competitivo
    
    private final String displayName;
    
    /**
     * Costruttore privato (obbligatorio per enum)
     * Assegna displayName a ogni valore
     */
    Level(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Getter per mostrare il nome user-friendly nei template HTML
     * Esempio: Level.INTERMEDIO.getDisplayName() → "Intermedio"
     */
    public String getDisplayName() {
        return displayName;
    }
}
