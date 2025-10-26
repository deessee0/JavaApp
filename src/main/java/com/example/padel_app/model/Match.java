package com.example.padel_app.model;

import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.model.enums.MatchType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity Match - Rappresenta una partita di padel
 * 
 * CONCETTI JPA DIMOSTRATI:
 * - @ManyToOne: Relazione molti-a-uno con User (creatore partita)
 * - @OneToMany: Relazione uno-a-molti con Registration e Feedback
 * - Metodi helper business logic (getActiveRegistrationsCount, isFull)
 * - Default values per campi (status, createdAt)
 * 
 * BUSINESS LOGIC:
 * - Una partita può avere massimo 4 giocatori (2vs2 padel)
 * - Status WAITING → CONFIRMED quando raggiunge 4 iscritti (auto-conferma)
 * - Status CONFIRMED → FINISHED quando termina (manualmente)
 * - Solo partite FINISHED possono ricevere feedback
 * 
 * RELAZIONI:
 * - creator (ManyToOne User): chi ha creato la partita
 * - registrations (OneToMany Registration): lista iscritti
 * - feedbacks (OneToMany Feedback): feedback post-partita
 */
@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Match {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Tipo partita: PROPOSTA o FISSA
     * - PROPOSTA: data/ora flessibile, si decide dopo
     * - FISSA: data/ora già stabilita
     */
    @NotNull(message = "Match type is required")
    @Enumerated(EnumType.STRING)
    private MatchType type;
    
    /**
     * Status partita: WAITING → CONFIRMED → FINISHED
     * - WAITING: in attesa di giocatori (0-3 iscritti)
     * - CONFIRMED: 4 giocatori confermati, partita sicura
     * - FINISHED: partita completata, si possono dare feedback
     * 
     * Default WAITING per nuove partite create
     */
    @NotNull(message = "Match status is required")
    @Enumerated(EnumType.STRING)
    private MatchStatus status = MatchStatus.WAITING;
    
    /**
     * Livello richiesto minimo per partecipare
     * Serve per bilanciare le partite (evita principianti vs professionisti)
     */
    @NotNull(message = "Required level is required")
    @Enumerated(EnumType.STRING)
    private Level requiredLevel;
    
    /**
     * Data e ora della partita
     * Usa LocalDateTime (Java 8+) invece di vecchio java.util.Date
     */
    @NotNull(message = "Match date and time is required")
    private LocalDateTime dateTime;
    
    /**
     * Luogo della partita (es. "Tennis Club Milano")
     */
    @NotBlank(message = "Location is required")
    private String location;
    
    /**
     * Descrizione opzionale (note, indicazioni, ecc.)
     */
    private String description;
    
    /**
     * Relazione MANY-TO-ONE con User (creatore)
     * Molte partite possono essere create dallo stesso utente.
     * 
     * FetchType.LAZY: User creator viene caricato solo se richiesto
     * @JoinColumn: specifica nome colonna FK in DB (creator_id)
     * 
     * NOTA: Se il creatore viene eliminato, cosa succede alla partita?
     * Opzioni: ON DELETE CASCADE, ON DELETE SET NULL, ecc.
     * Qui: nessuna cascata = partite rimangono orfane (da decidere in base al business)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;
    
    /**
     * Relazione ONE-TO-MANY con Registration
     * Una partita ha molte iscrizioni (max 4 JOINED contemporaneamente)
     * 
     * mappedBy="match": Match è il lato "inverse", FK sta in Registration
     * cascade=ALL: se elimino Match, elimino anche tutte le sue Registration
     * orphanRemoval=true: se rimuovo registration dalla lista, viene eliminata dal DB
     */
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Registration> registrations = new ArrayList<>();
    
    /**
     * Relazione ONE-TO-MANY con Feedback
     * Una partita può avere molti feedback (ogni giocatore valuta gli altri 3)
     */
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Feedback> feedbacks = new ArrayList<>();
    
    /**
     * Timestamp creazione partita
     * Utile per ordinamento cronologico e audit
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // ==================== BUSINESS LOGIC METHODS ====================
    
    /**
     * Conta quanti giocatori sono effettivamente iscritti (status JOINED)
     * 
     * NOTA: Questo metodo funziona solo se registrations è già caricata.
     * In WebController usiamo RegistrationRepository.countActiveRegistrationsByMatch()
     * che fa una query COUNT diretta, più efficiente.
     * 
     * @return numero giocatori con status JOINED
     */
    public int getActiveRegistrationsCount() {
        return (int) registrations.stream()
            .filter(r -> r.getStatus() == com.example.padel_app.model.enums.RegistrationStatus.JOINED)
            .count();
    }
    
    /**
     * Verifica se la partita ha raggiunto il massimo di 4 giocatori
     * 
     * Usato per:
     * - Bloccare nuove iscrizioni
     * - Triggerare auto-conferma (WAITING → CONFIRMED)
     * - UI: disabilitare pulsante "Iscriviti"
     * 
     * @return true se 4 o più giocatori JOINED
     */
    public boolean isFull() {
        return getActiveRegistrationsCount() >= 4;
    }
}
