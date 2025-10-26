package com.example.padel_app.model.enums;

/**
 * Enum MatchStatus - Stati del ciclo di vita di una partita
 * 
 * LIFECYCLE (Diagramma Stati):
 * 
 *     [WAITING] ──(4° giocatore si iscrive)──> [CONFIRMED]
 *        │                                          │
 *        │                                          │
 *        │                                   (Termina partita)
 *        │                                          │
 *        │                                          ↓
 *        └──────────(Creatore annulla)──────> [CANCELLED] ← [FINISHED]
 * 
 * BUSINESS RULES:
 * 
 * WAITING (In Attesa):
 * - Stato iniziale quando si crea una partita
 * - 0-3 giocatori iscritti
 * - Si può join/leave liberamente
 * - AUTO-TRANSIZIONE a CONFIRMED quando 4° giocatore si iscrive
 * 
 * CONFIRMED (Confermata):
 * - Esattamente 4 giocatori JOINED
 * - Partita sicura, pubblicato evento MatchConfirmedEvent
 * - Non si può più leave (regola business)
 * - Può terminare manualmente → FINISHED
 * 
 * FINISHED (Terminata):
 * - Partita completata
 * - Si possono dare feedback ai compagni
 * - Incrementato User.matchesPlayed per tutti i partecipanti
 * - Pubblicato evento MatchFinishedEvent
 * 
 * CANCELLED (Annullata):
 * - Partita cancellata dal creatore
 * - Non usata attivamente nell'app corrente
 * - Prevista per future funzionalità
 */
public enum MatchStatus {
    WAITING("In Attesa"),       // 0-3 giocatori, in attesa di conferma
    CONFIRMED("Confermata"),    // 4 giocatori, partita sicura
    FINISHED("Terminata"),      // Completata, si possono dare feedback
    CANCELLED("Annullata");     // Annullata (future use)
    
    private final String displayName;
    
    MatchStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
