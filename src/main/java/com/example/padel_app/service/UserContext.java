package com.example.padel_app.service;

import com.example.padel_app.model.User;
import com.example.padel_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * UserContext - Simulazione utente loggato (autenticazione semplificata)
 * 
 * SCOPO DIDATTICO:
 * Questo progetto universitario NON implementa un sistema di autenticazione completo
 * (Spring Security, JWT, sessioni, ecc.) perché l'obiettivo è dimostrare:
 * - Design Patterns (Observer, Strategy, Singleton)
 * - Architettura MVC
 * - Business logic
 * 
 * APPROCCIO SEMPLIFICATO:
 * - L'utente loggato è sempre **Margherita Biffi** (username: "margherita")
 * - Ogni operazione (join, leave, feedback) viene fatta da Margherita
 * - NO login form, NO password check, NO session management
 * 
 * COME FUNZIONA:
 * 1. DataSeeder crea utente "margherita" all'avvio
 * 2. WebController chiama userContext.getCurrentUser() per ogni richiesta
 * 3. Ritorna sempre lo stesso utente dal database
 * 
 * IN PRODUZIONE SI USEREBBE:
 * - Spring Security + @AuthenticationPrincipal per utente autenticato
 * - SecurityContext.getAuthentication() per recuperare utente corrente
 * - Session/JWT token per gestire login multipli
 * 
 * Ma per questo progetto didattico, questo semplice Context è sufficiente.
 */
@Component  // Bean Spring (singleton per default)
@RequiredArgsConstructor
public class UserContext {
    
    private final UserRepository userRepository;
    
    /**
     * Restituisce l'utente attualmente "loggato" nell'applicazione.
     * 
     * SIMULAZIONE AUTENTICAZIONE:
     * Invece di controllare sessione o token JWT, ritorna sempre
     * l'utente con username "margherita" dal database.
     * 
     * PERCHÉ orElseThrow?
     * Se "margherita" non esiste nel DB (DataSeeder fallito?),
     * lanciamo eccezione per bloccare app. Meglio fail-fast che NPE ovunque.
     * 
     * USO:
     * ```java
     * // In WebController
     * User currentUser = userContext.getCurrentUser();
     * registrationService.joinMatch(currentUser, match);
     * ```
     * 
     * @return User "margherita" dal database
     * @throws IllegalStateException se "margherita" non esiste
     */
    public User getCurrentUser() {
        return userRepository.findByUsername("margherita")
                .orElseThrow(() -> new IllegalStateException(
                    "Utente Margherita Biffi non trovato nel sistema. " +
                    "Verifica che DataSeeder abbia creato l'utente all'avvio."
                ));
    }
}
