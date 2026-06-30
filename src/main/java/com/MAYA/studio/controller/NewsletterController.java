package com.MAYA.studio.controller;

import com.MAYA.studio.service.NewsletterService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterService newsletterService;

    @PostMapping("/api/newsletter/subscribe")
    public ResponseEntity<Void> subscribe(@RequestBody SubscribeRequest request) {
        newsletterService.subscribe(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/admin/newsletter")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getSubscribers() {
        return ResponseEntity.ok(newsletterService.getAllSubscribers());
    }

    @Data
    static class SubscribeRequest {
        private String email;
    }
}
