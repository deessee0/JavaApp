package com.example.padel_app.model;

import com.example.padel_app.model.enums.Level;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity User - Rappresenta un giocatore di padel nel sistema
 * 
 * CONCETTI JPA DIMOSTRATI:
 * - @Entity: Marca questa classe come entità JPA mappata su tabella DB
 * - @Table: Specifica il nome della tabella (evita conflitti con keyword SQL)
 * - @Id + @GeneratedValue: Chiave primaria auto-incrementale
 * - @Column(unique=true): Vincolo di unicità su colonne
 * - @Enumerated: Mapping enum Java -> colonna stringa in DB
 * - @OneToMany: Relazione uno-a-molti con altre entità
 * - FetchType.LAZY: Caricamento pigro per ottimizzazione performance
 * - CascadeType.ALL: Propagazione operazioni (save, delete) alle entità figlie
 * 
 * BUSINESS LOGIC:
 * - declaredLevel: livello dichiarato dal giocatore stesso (può essere sovrastimato)
 * - perceivedLevel: livello calcolato dalla media dei feedback ricevuti (più affidabile)
 * - matchesPlayed: contatore partite per statistiche utente
 */
@Entity
@Table(name = "users")  // Nome tabella esplicito per evitare conflitti con "user" (keyword PostgreSQL)
@Data                    // Lombok: genera automaticamente getter, setter, equals, hashCode, toString
@NoArgsConstructor       // Lombok: costruttore senza parametri (richiesto da JPA)
@AllArgsConstructor      // Lombok: costruttore con tutti i parametri
public class User {
    
    /**
     * ID - Chiave primaria
     * IDENTITY strategy: database genera automaticamente l'ID (auto-increment)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Username - Identificativo univoco per login
     * Validazioni:
     * - @NotBlank: non può essere null, vuoto o solo spazi
     * - unique=true: vincolo unicità a livello database
     */
    @NotBlank(message = "Username is required")
    @Column(unique = true)
    private String username;
    
    /**
     * Email - Per comunicazioni e recovery password
     * Validazioni:
     * - @Email: verifica formato email valido
     * - unique=true: una email per un solo account
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(unique = true)
    private String email;
    
    /**
     * Password - Credenziale di accesso
     * NOTA: In produzione andrebbe hashata con BCrypt/Argon2
     */
    @NotBlank(message = "Password is required")
    private String password;
    
    /**
     * Nome e Cognome - Dati anagrafici
     */
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    /**
     * Livello Dichiarato - Livello auto-dichiarato dall'utente
     * @Enumerated(EnumType.STRING): salva il nome dell'enum (es. "INTERMEDIO")
     * invece del valore ordinale (es. 1). Più robusto a riordinamenti enum.
     */
    @NotNull(message = "Declared level is required")
    @Enumerated(EnumType.STRING)
    private Level declaredLevel;
    
    /**
     * Livello Percepito - Livello calcolato dai feedback degli altri giocatori
     * Inizialmente null, viene aggiornato dopo i primi feedback ricevuti.
     * Rappresenta una valutazione più oggettiva del livello reale.
     */
    @Enumerated(EnumType.STRING)
    private Level perceivedLevel;
    
    /**
     * Contatore partite giocate - Per statistiche utente
     * Default value = 0 per evitare NullPointerException
     */
    @Column(nullable = false)
    private Integer matchesPlayed = 0;
    
    /**
     * Relazione ONE-TO-MANY con Registration
     * Un utente può avere molte iscrizioni a partite.
     * 
     * mappedBy="user": indica che User è il lato "inverse" della relazione.
     *                  La foreign key sta nella tabella "registrations".
     * 
     * cascade=ALL: quando salvo/elimino User, salvo/elimino anche le sue registrations
     * fetch=LAZY: le registrations vengono caricate solo se esplicitamente richieste
     *            (ottimizzazione performance - evita N+1 query problem)
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Registration> registrations = new ArrayList<>();
    
    /**
     * Feedback DATI da questo utente ad altri
     * mappedBy="author": questo User è l'autore del feedback
     */
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Feedback> givenFeedbacks = new ArrayList<>();
    
    /**
     * Feedback RICEVUTI da altri utenti
     * mappedBy="targetUser": questo User è il destinatario del feedback
     * Usati per calcolare il perceivedLevel (media feedback ricevuti)
     */
    @OneToMany(mappedBy = "targetUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Feedback> receivedFeedbacks = new ArrayList<>();
}
