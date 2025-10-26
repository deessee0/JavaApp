package com.example.padel_app.model;

import com.example.padel_app.model.enums.Level;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity Feedback - Rappresenta la valutazione di un giocatore dopo una partita
 * 
 * BUSINESS LOGIC:
 * Dopo una partita FINISHED, ogni giocatore può valutare gli altri 3 compagni/avversari.
 * Il feedback serve per calcolare il "livello percepito" del giocatore target.
 * 
 * ESEMPIO:
 * Partita con Alice, Bob, Carlo, Diana (tutti INTERMEDIO dichiarato)
 * - Alice dà feedback a Bob: suggestedLevel = AVANZATO
 * - Alice dà feedback a Carlo: suggestedLevel = PRINCIPIANTE
 * - Alice dà feedback a Diana: suggestedLevel = INTERMEDIO
 * 
 * VINCOLO CRITICO:
 * @UniqueConstraint(author_id, target_user_id, match_id)
 * Previene che Alice dia 2 feedback a Bob per la stessa partita.
 * 
 * ALGORITMO LIVELLO PERCEPITO:
 * perceivedLevel = media(tutti i feedback ricevuti)
 * Esempio Bob riceve: AVANZATO, AVANZATO, INTERMEDIO
 * → perceivedLevel = AVANZATO (più affidabile del declared level)
 * 
 * RELAZIONI:
 * - author: chi dà il feedback (User)
 * - targetUser: chi riceve il feedback (User)
 * - match: per quale partita si dà il feedback (Match)
 */
@Entity
@Table(name = "feedbacks",
       uniqueConstraints = @UniqueConstraint(columnNames = {"author_id", "target_user_id", "match_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Author - Chi dà il feedback
     * Relazione MANY-TO-ONE: un utente può dare molti feedback (uno per compagno per partita)
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;
    
    /**
     * Target User - Chi riceve il feedback
     * Relazione MANY-TO-ONE: un utente può ricevere molti feedback
     * 
     * Questi feedback vengono usati per calcolare User.perceivedLevel
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;
    
    /**
     * Match - Per quale partita si dà questo feedback
     * Importante per:
     * - Vincolo unicità: un solo feedback per coppia (author, target) per match
     * - Audit: sapere quando/dove è stato dato il feedback
     * - Business logic: feedback solo per partite FINISHED
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;
    
    /**
     * Livello suggerito per il target user
     * 
     * Questo è il "voto" che author dà a targetUser dopo aver giocato insieme.
     * Esempi:
     * - "Bob gioca meglio di quanto dichiara" → suggestedLevel = AVANZATO
     * - "Carlo è sovrastimato" → suggestedLevel = PRINCIPIANTE
     * 
     * La media di tutti i suggestedLevel ricevuti diventa perceivedLevel
     */
    @NotNull(message = "Suggested level is required")
    @Enumerated(EnumType.STRING)
    private Level suggestedLevel;
    
    /**
     * Commento testuale opzionale
     * Esempio: "Ottima tecnica al volo, deve migliorare il servizio"
     * 
     * Max 1000 caratteri (buona pratica limitare text field)
     */
    @Column(length = 1000)
    private String comment;
    
    /**
     * Timestamp creazione feedback
     * Utile per audit e ordinamento cronologico
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
