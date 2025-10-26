package com.example.padel_app.repository;

import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository - Interfaccia per accesso dati User
 * 
 * SPRING DATA JPA - CONCETTI FONDAMENTALI:
 * 
 * 1. EXTENDS JpaRepository<User, Long>
 *    - User: tipo entità gestita
 *    - Long: tipo chiave primaria (User.id è Long)
 *    - JpaRepository fornisce GRATIS metodi CRUD:
 *      * save(user): INSERT o UPDATE
 *      * findById(id): SELECT WHERE id = ?
 *      * findAll(): SELECT * FROM users
 *      * deleteById(id): DELETE WHERE id = ?
 *      * count(): SELECT COUNT(*) FROM users
 *    
 * 2. DERIVED QUERY METHODS (Spring genera SQL automaticamente dal nome metodo)
 *    Convenzioni di naming:
 *    - findBy + NomeCampo: SELECT WHERE campo = ?
 *    - existsBy + NomeCampo: SELECT EXISTS WHERE campo = ?
 *    - countBy + NomeCampo: SELECT COUNT WHERE campo = ?
 *    
 * 3. @Query CUSTOM (quando serve JPQL complesso)
 *    - JPQL: query su entità Java, non tabelle DB
 *    - Sintassi: "SELECT u FROM User u WHERE..." (User è la classe, non la tabella)
 *    - Parametri: :nomeparametro invece di ?
 * 
 * NON SERVE IMPLEMENTAZIONE!
 * Spring crea automaticamente l'implementazione a runtime tramite proxy.
 */
@Repository  // Marca come componente Spring di accesso dati
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Trova utente per username (per login)
     * 
     * DERIVED QUERY METHOD:
     * Spring genera automaticamente: SELECT * FROM users WHERE username = ?
     * 
     * Optional<User>: wrapper che può contenere User o essere vuoto
     * Evita NullPointerException e forza gestione caso "not found"
     * 
     * Uso:
     *   Optional<User> user = userRepository.findByUsername("alice");
     *   if (user.isPresent()) { ... } oppure user.orElseThrow(...)
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Trova utente per email
     * 
     * SQL generato: SELECT * FROM users WHERE email = ?
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Verifica se username esiste già (per validazione registrazione)
     * 
     * DERIVED QUERY METHOD - exists:
     * SQL generato: SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)
     * Più efficiente di findByUsername().isPresent() perché non carica l'entità
     * 
     * Uso:
     *   if (userRepository.existsByUsername("alice")) {
     *       throw new Exception("Username già in uso");
     *   }
     */
    boolean existsByUsername(String username);
    
    /**
     * Verifica se email esiste già
     * SQL: SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)
     */
    boolean existsByEmail(String email);
    
    /**
     * Trova tutti gli utenti con uno specifico livello dichiarato
     * 
     * DERIVED QUERY METHOD - enum parameter:
     * SQL: SELECT * FROM users WHERE declared_level = 'INTERMEDIO'
     * Spring converte enum Level.INTERMEDIO → stringa 'INTERMEDIO' automaticamente
     * 
     * Uso:
     *   List<User> intermediPlayers = userRepository.findByDeclaredLevel(Level.INTERMEDIO);
     */
    List<User> findByDeclaredLevel(Level declaredLevel);
    
    /**
     * Trova tutti gli utenti con uno specifico livello percepito
     * 
     * @Query CUSTOM (potremmo usare derived method, ma mostra esempio @Query)
     * JPQL: usa nome classe Java (User) e nome campo (perceivedLevel)
     * :level: named parameter, più leggibile di ?1
     * 
     * SQL generato: SELECT * FROM users WHERE perceived_level = ?
     */
    @Query("SELECT u FROM User u WHERE u.perceivedLevel = :level")
    List<User> findByPerceivedLevel(Level level);
    
    /**
     * Trova tutti gli utenti ordinati per partite giocate (decrescente)
     * 
     * @Query CUSTOM con ORDER BY:
     * Utile per classifiche, statistiche, "top players"
     * 
     * SQL: SELECT * FROM users ORDER BY matches_played DESC
     * 
     * Uso: mostrare classifica giocatori più attivi
     */
    @Query("SELECT u FROM User u ORDER BY u.matchesPlayed DESC")
    List<User> findAllOrderByMatchesPlayedDesc();
}
