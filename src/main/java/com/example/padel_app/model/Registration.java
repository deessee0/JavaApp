package com.example.padel_app.model;

import com.example.padel_app.model.enums.RegistrationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity Registration - Rappresenta l'iscrizione di un User a un Match
 * 
 * PATTERN: Association Class (Many-to-Many con attributi)
 * 
 * PERCHÉ ESISTE QUESTA CLASSE?
 * User e Match hanno una relazione many-to-many:
 * - Un utente può iscriversi a molte partite
 * - Una partita può avere molti utenti iscritti
 * 
 * Ma abbiamo bisogno di tracciare informazioni SULL'ISCRIZIONE stessa:
 * - Quando si è iscritto? (registeredAt)
 * - È ancora attivo o si è disiscritto? (status)
 * 
 * SOLUZIONE: Trasformare Many-to-Many in due One-to-Many
 * User 1---N Registration N---1 Match
 * 
 * VINCOLO BUSINESS CRITICAL:
 * @UniqueConstraint(user_id, match_id): un utente può iscriversi UNA SOLA VOLTA
 * alla stessa partita. Previene duplicati nel DB.
 * 
 * STATUS LIFECYCLE:
 * - JOINED: iscritto attivo, conta per raggiungere 4 giocatori
 * - CANCELLED: disiscritto, non conta più (ma record rimane per audit)
 */
@Entity
@Table(name = "registrations", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "match_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Registration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Relazione MANY-TO-ONE con User
     * Molte registrations appartengono allo stesso utente.
     * 
     * Questa è una delle due "gambe" della relazione many-to-many originale.
     * @JoinColumn(user_id): crea foreign key in tabella registrations
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Relazione MANY-TO-ONE con Match
     * Molte registrations appartengono alla stessa partita.
     * 
     * Questa è l'altra "gamba" della relazione many-to-many.
     * @JoinColumn(match_id): crea foreign key in tabella registrations
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;
    
    /**
     * Status iscrizione: JOINED o CANCELLED
     * 
     * JOINED: utente attivo nella partita
     *   - Conta per raggiungere 4 giocatori
     *   - Se sono 4 JOINED → Match status diventa CONFIRMED
     * 
     * CANCELLED: utente si è disiscritto
     *   - Non conta più per i 4 giocatori
     *   - Record rimane nel DB per storico/audit
     *   - La partita torna disponibile (posto libero)
     * 
     * Default: JOINED (quando un utente si iscrive)
     */
    @NotNull(message = "Registration status is required")
    @Enumerated(EnumType.STRING)
    private RegistrationStatus status = RegistrationStatus.JOINED;
    
    /**
     * Timestamp iscrizione
     * Utile per:
     * - Audit: chi si è iscritto quando?
     * - Business logic: iscrizioni in ordine cronologico
     * - UI: mostrare "Iscritto il 15/10/2025"
     */
    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();
}
