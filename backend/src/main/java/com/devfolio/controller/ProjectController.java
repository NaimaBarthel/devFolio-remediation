package com.devfolio.controller;

import com.devfolio.model.Project;
import com.devfolio.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.devfolio.model.User;
import com.devfolio.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import java.net.URI;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import java.net.URL;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        return userRepository
                .findByEmail(authentication.getName())
                .orElse(null);
    }

    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects() {
        return ResponseEntity.ok(projectRepository.findByIsPublicTrue());
    }

    @GetMapping("/{id}")
      public ResponseEntity<Project> getProject(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);

        return projectRepository.findById(id)
                .map(project -> {

                    boolean isPublic =
                            Boolean.TRUE.equals(project.getIsPublic());

                    boolean isOwner =
                            currentUser != null
                                    && project.getOwnerId() != null
                                    && project.getOwnerId().equals(currentUser.getId());

                    if (isPublic || isOwner) {
                        return ResponseEntity.ok(project);
                    }

                    return ResponseEntity
                            .status(HttpStatus.FORBIDDEN)
                            .<Project>build();

                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Project> createProject(
            @RequestBody Project project,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        project.setId(null);
        project.setOwnerId(currentUser.getId());

        return ResponseEntity.ok(projectRepository.save(project));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(
            @PathVariable Long id,
            @RequestBody Project updated,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);

        return projectRepository.findById(id)
                .map(project -> {

                    if (currentUser == null
                            || project.getOwnerId() == null
                            || !project.getOwnerId().equals(currentUser.getId())) {

                        return ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .<Project>build();
                    }

                    project.setTitle(updated.getTitle());
                    project.setDescription(updated.getDescription());
                    project.setGithubUrl(updated.getGithubUrl());
                    project.setImageUrl(updated.getImageUrl());

                    return ResponseEntity.ok(projectRepository.save(project));

                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);

        return projectRepository.findById(id)
                .map(project -> {

                    if (currentUser == null
                            || project.getOwnerId() == null
                            || !project.getOwnerId().equals(currentUser.getId())) {

                        return ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .build();
                    }

                    projectRepository.delete(project);

                    return ResponseEntity.ok().build();

                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/import")
    public ResponseEntity<?> importFromGithub(@RequestParam String githubUrl) {

        HttpURLConnection connection = null;

        try {
            URI uri = URI.create(githubUrl);

            // HTTPS obligatoire
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "HTTPS obligatoire"));
            }

            // Autoriser uniquement le domaine GitHub prévu
            if (!"raw.githubusercontent.com".equalsIgnoreCase(uri.getHost())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Domaine non autorisé"));
            }

            // Refuser les ports personnalisés et les identifiants
            if (uri.getPort() != -1 || uri.getRawUserInfo() != null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "URL non autorisée"));
            }

            connection = (HttpURLConnection) uri.toURL().openConnection();

            // Désactiver les redirections
            connection.setInstanceFollowRedirects(false);

            // Limiter les délais
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);

            int status = connection.getResponseCode();

            if (status != 200) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Fichier inaccessible"));
            }

            // Limiter le téléchargement à 1 Mo
            int maxSize = 1024 * 1024;

            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[8192];
                int total = 0;
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    total += bytesRead;

                    if (total > maxSize) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "Fichier trop volumineux"));
                    }

                    output.write(buffer, 0, bytesRead);
                }

                String content = output.toString(StandardCharsets.UTF_8);

                return ResponseEntity.ok(Map.of("content", content));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "URL invalide ou inaccessible"));

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
