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

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Username is required")
    @Column(unique = true)
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(unique = true)
    private String email;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    @NotNull(message = "Declared level is required")
    @Enumerated(EnumType.STRING)
    private Level declaredLevel;
    
    @Enumerated(EnumType.STRING)
    private Level perceivedLevel;
    
    @Column(nullable = false)
    private Integer matchesPlayed = 0;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Registration> registrations = new ArrayList<>();
    
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Feedback> givenFeedbacks = new ArrayList<>();
    
    @OneToMany(mappedBy = "targetUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Feedback> receivedFeedbacks = new ArrayList<>();
}