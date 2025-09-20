package com.example.padel_app.service;

import com.example.padel_app.model.User;
import com.example.padel_app.model.enums.Level;
import com.example.padel_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public List<User> getUsersByDeclaredLevel(Level level) {
        return userRepository.findByDeclaredLevel(level);
    }
    
    public List<User> getUsersByPerceivedLevel(Level level) {
        return userRepository.findByPerceivedLevel(level);
    }
    
    public List<User> getUsersOrderByMatchesPlayed() {
        return userRepository.findAllOrderByMatchesPlayedDesc();
    }
    
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    
    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    @Transactional
    public User incrementMatchesPlayed(User user) {
        user.setMatchesPlayed(user.getMatchesPlayed() + 1);
        return userRepository.save(user);
    }
    
    @Transactional
    public User updatePerceivedLevel(User user, Level newLevel) {
        user.setPerceivedLevel(newLevel);
        return userRepository.save(user);
    }
    
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}