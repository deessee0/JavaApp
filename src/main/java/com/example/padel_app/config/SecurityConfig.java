package com.example.padel_app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configurazione della Sicurezza - Password Hashing con BCrypt.
 * 
 * <h2>Cos'è BCrypt?</h2>
 * BCrypt è un algoritmo di hashing per password che:
 * <ul>
 *   <li><strong>One-way function</strong>: Impossibile decifrare (hash → password originale)</li>
 *   <li><strong>Salted</strong>: Aggiunge salt casuale per prevenire rainbow table attacks</li>
 *   <li><strong>Slow by design</strong>: Rallenta attacchi brute-force (circa 100ms per hash)</li>
 *   <li><strong>Adaptive</strong>: Può aumentare costo computazionale nel tempo (work factor)</li>
 * </ul>
 * 
 * <h2>Perché NON salvare password in chiaro?</h2>
 * <table border="1">
 *   <tr>
 *     <th>Scenario</th>
 *     <th>Password in chiaro</th>
 *     <th>Password hashate con BCrypt</th>
 *   </tr>
 *   <tr>
 *     <td>Database leak</td>
 *     <td>❌ Attacker vede tutte le password</td>
 *     <td>✅ Attacker vede solo hash inutilizzabili</td>
 *   </tr>
 *   <tr>
 *     <td>Admin vede DB</td>
 *     <td>❌ Admin può rubare account utenti</td>
 *     <td>✅ Admin vede solo hash, non può fare login</td>
 *   </tr>
 *   <tr>
 *     <td>Log accidentali</td>
 *     <td>❌ Password finiscono nei log in chiaro</td>
 *     <td>✅ Solo hash nei log (sicuri)</td>
 *   </tr>
 * </table>
 * 
 * <h2>Come funziona BCrypt - Esempio:</h2>
 * <pre>
 * // REGISTRAZIONE:
 * String plainPassword = "password123";
 * String hashedPassword = bcrypt.encode(plainPassword);
 * // Result: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
 * //         ^^^^^ ^^^ ^^^^^^^^^^^^^^^^^^^^^^ ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 * //         algo  work salt (22 chars)      hash (31 chars)
 * //               factor
 * 
 * user.setPassword(hashedPassword); // Salviamo l'hash, NON la password originale
 * userRepository.save(user);
 * 
 * // LOGIN:
 * String inputPassword = "password123"; // Password inserita dall'utente
 * String storedHash = user.getPassword(); // Hash dal database
 * 
 * boolean matches = bcrypt.matches(inputPassword, storedHash);
 * // BCrypt ricalcola hash con stesso salt e confronta:
 * // - Input: "password123" + salt estratto da storedHash → nuovo hash
 * // - Confronta nuovo hash con hash memorizzato
 * // - Se uguali → password corretta ✅
 * </pre>
 * 
 * <h2>Anatomia di un BCrypt hash:</h2>
 * <pre>
 * $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
 * │   │  │                      │
 * │   │  │                      └─ Hash finale (31 chars)
 * │   │  └─────────────────────── Salt casuale (22 chars)
 * │   └──────────────────────────── Work factor (2^10 = 1024 rounds)
 * └──────────────────────────────── Algoritmo BCrypt versione 2a
 * 
 * Totale: 60 caratteri (sempre)
 * </pre>
 * 
 * <h2>Work Factor (costo computazionale):</h2>
 * BCrypt usa parametro "rounds" (default 10 → 2^10 = 1024 iterazioni):
 * <ul>
 *   <li>Rounds 10: ~100ms per hash (buon compromesso)</li>
 *   <li>Rounds 12: ~400ms per hash (più sicuro ma più lento)</li>
 *   <li>Rounds 4: ~1ms per hash (INSICURO - solo per test)</li>
 * </ul>
 * 
 * Più rounds = più sicuro contro brute force, ma login più lento.
 * Raccomandato: 10-12 rounds per produzione.
 * 
 * <h2>Differenza con altri algoritmi:</h2>
 * <table border="1">
 *   <tr>
 *     <th>Algoritmo</th>
 *     <th>Velocità</th>
 *     <th>Sicurezza Password</th>
 *     <th>Uso consigliato</th>
 *   </tr>
 *   <tr>
 *     <td>MD5/SHA1</td>
 *     <td>⚡ Molto veloce</td>
 *     <td>❌ INSICURO (no salt, troppo veloce)</td>
 *     <td>❌ MAI per password!</td>
 *   </tr>
 *   <tr>
 *     <td>SHA-256</td>
 *     <td>⚡ Veloce</td>
 *     <td>⚠️ OK con salt, ma troppo veloce</td>
 *     <td>Checksum file, non password</td>
 *   </tr>
 *   <tr>
 *     <td>BCrypt</td>
 *     <td>🐢 Lento (by design)</td>
 *     <td>✅ Ottimo (salt + slow)</td>
 *     <td>✅ Password hashing</td>
 *   </tr>
 *   <tr>
 *     <td>Argon2</td>
 *     <td>🐢 Lento + memory-hard</td>
 *     <td>✅ Eccellente (migliore di BCrypt)</td>
 *     <td>✅ Alternative moderna</td>
 *   </tr>
 * </table>
 * 
 * <h2>Implementazione in questo progetto:</h2>
 * <pre>
 * // BEAN SPRING:
 * &#64;Bean
 * public PasswordEncoder passwordEncoder() {
 *     return new BCryptPasswordEncoder(); // Default: 10 rounds
 * }
 * 
 * // USAGE IN CONTROLLER:
 * &#64;Autowired
 * private PasswordEncoder passwordEncoder;
 * 
 * // Registrazione:
 * user.setPassword(passwordEncoder.encode(plainPassword));
 * 
 * // Login:
 * if (passwordEncoder.matches(inputPassword, user.getPassword())) {
 *     // Login OK ✅
 * }
 * </pre>
 * 
 * <h2>⚠️ NOTA PER PROGETTO DIDATTICO:</h2>
 * Questo progetto parte con password in chiaro (per semplicità iniziale).
 * Con questa configurazione, nuove registrazioni useranno BCrypt, ma
 * dati seedati potrebbero avere password in chiaro.
 * 
 * Per supportare entrambi (transizione graduale):
 * - Nuove password → sempre BCrypt
 * - Vecchie password → check sia chiaro che BCrypt
 * - Gradualmente tutti gli utenti passeranno a BCrypt al prossimo login
 * 
 * @see BCryptPasswordEncoder Implementazione Spring Security
 * @see PasswordEncoder Interfaccia standard Spring
 * @author Padel App Team
 */
@Configuration
public class SecurityConfig {
    
    /**
     * Bean per BCryptPasswordEncoder.
     * 
     * <p>Spring gestisce questo bean come Singleton:
     * <ul>
     *   <li>Una sola istanza per tutta l'applicazione</li>
     *   <li>Iniettabile in qualsiasi componente con @Autowired</li>
     *   <li>Thread-safe (può essere usato concorrentemente)</li>
     * </ul>
     * 
     * <p><strong>Configurazione default:</strong>
     * <ul>
     *   <li>Strength: 10 (2^10 = 1024 rounds)</li>
     *   <li>SecureRandom: true (salt generati con PRNG sicuro)</li>
     * </ul>
     * 
     * <p><strong>Esempio output hash:</strong>
     * <pre>
     * Input:  "password123"
     * Output: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
     * 
     * Stessa password, secondo hash:
     * Output: "$2a$10$a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6"
     * 
     * Nota: Hash diversi (salt casuale) ma entrambi verificano la stessa password!
     * </pre>
     * 
     * @return PasswordEncoder configurato con BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
