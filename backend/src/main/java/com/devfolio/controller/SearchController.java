package com.devfolio.controller;

import com.devfolio.model.Project;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/projects")
    public ResponseEntity<?> searchProjects(@RequestParam String q) {

        String sql = "SELECT * FROM projects " +
                "WHERE title LIKE :search " +
                "OR description LIKE :search";

        Query query = entityManager.createNativeQuery(sql, Project.class);

        query.setParameter("search", "%" + q + "%");

        @SuppressWarnings("unchecked")
        List<Project> results = query.getResultList();

        return ResponseEntity.ok(results);
    }
}