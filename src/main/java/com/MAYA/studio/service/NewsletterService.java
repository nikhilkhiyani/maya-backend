package com.MAYA.studio.service;

import com.MAYA.studio.entity.NewsletterSubscriber;
import com.MAYA.studio.exception.BadRequestException;
import com.MAYA.studio.repository.NewsletterSubscriberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsletterService {

    private final NewsletterSubscriberRepository repository;
    private final AuditService auditService;

    @Transactional
    public void subscribe(String email) {
        if (repository.existsByEmail(email)) {
            throw new BadRequestException("Email already subscribed");
        }
        repository.save(NewsletterSubscriber.builder().email(email).build());
        auditService.log("NEWSLETTER_SUBSCRIBE", "Newsletter", email, null);
    }

    @Transactional(readOnly = true)
    public List<NewsletterSubscriber> getAllSubscribers() {
        return repository.findAll();
    }
}
