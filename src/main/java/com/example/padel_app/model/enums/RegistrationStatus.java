package com.example.padel_app.model.enums;

/**
 * Enum RegistrationStatus - Stati dell'iscrizione di un utente a una partita
 * 
 * BUSINESS LOGIC:
 * 
 * JOINED (Iscritto):
 * - Utente attivo nella partita
 * - Conta per raggiungere il limite di 4 giocatori
 * - Se 4 JOINED → Match diventa CONFIRMED
 * - Default quando un utente si iscrive
 * 
 * CANCELLED (Cancellato/Disiscritto):
 * - Utente si è disiscritto dalla partita
 * - NON conta più per i 4 giocatori
 * - Il record rimane nel DB per audit/storico
 * - La partita torna disponibile (3/4 giocatori)
 * 
 * PERCHÉ NON ELIMINARE IL RECORD?
 * 1. Audit trail: sapere chi si era iscritto e poi ritirato
 * 2. Statistiche: quante volte un utente si disiscritto?
 * 3. Partite FINISHED: se qualcuno si disiscrivo DOPO la fine,
 *    vogliamo che appaia ancora nel roster per il feedback
 * 
 * QUERY IMPORTANTI:
 * - Contare giocatori attivi: COUNT WHERE status = JOINED
 * - Storico completo: SELECT * (include anche CANCELLED)
 */
public enum RegistrationStatus {
    JOINED("Iscritto"),        // Attivo, conta per i 4 posti
    CANCELLED("Cancellato");   // Disiscritto, non conta più
    
    private final String displayName;
    
    RegistrationStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
