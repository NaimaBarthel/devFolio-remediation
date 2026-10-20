package com.devfolio.controller;

import com.devfolio.model.User;
import com.devfolio.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/admin/users")
    // Retourne les hashes MD5 des mots de passe
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody User updated,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = userRepository
                .findByEmail(authentication.getName())
                .orElse(null);

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Vérifier que l'utilisateur modifie uniquement son profil
        if (!currentUser.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return userRepository.findById(id)
                .map(user -> {

                    user.setEmail(updated.getEmail());
                    user.setBio(updated.getBio());

                    // Le rôle ne peut pas être modifié ici

                    return ResponseEntity.ok(userRepository.save(user));

                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
