package com.example.padel_app.model;

import com.example.padel_app.model.enums.RegistrationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;
    
    @NotNull(message = "Registration status is required")
    @Enumerated(EnumType.STRING)
    private RegistrationStatus status = RegistrationStatus.JOINED;
    
    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();
}