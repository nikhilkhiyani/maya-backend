package com.MAYA.studio.service;

import com.MAYA.studio.dto.BannerRequest;
import com.MAYA.studio.dto.BannerResponse;
import com.MAYA.studio.entity.Banner;
import com.MAYA.studio.exception.ResourceNotFoundException;
import com.MAYA.studio.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;

    public List<BannerResponse> getActiveHeroBanners() {
        return bannerRepository.findByActiveTrueAndTypeOrderByPriorityAsc(Banner.BannerType.HERO)
                .stream()
                .filter(this::isVisible)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<BannerResponse> getAll() {
        return bannerRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BannerResponse create(BannerRequest request) {
        Banner banner = Banner.builder()
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .image(request.getImage())
                .buttonText(request.getButtonText())
                .buttonLink(request.getButtonLink())
                .type(request.getType() != null ? request.getType() : Banner.BannerType.HERO)
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .active(request.getActive() != null ? request.getActive() : true)
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .build();
        return toResponse(bannerRepository.save(banner));
    }

    @Transactional
    public BannerResponse update(UUID id, BannerRequest request) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found"));

        if (request.getTitle() != null) banner.setTitle(request.getTitle());
        if (request.getSubtitle() != null) banner.setSubtitle(request.getSubtitle());
        if (request.getImage() != null) banner.setImage(request.getImage());
        if (request.getButtonText() != null) banner.setButtonText(request.getButtonText());
        if (request.getButtonLink() != null) banner.setButtonLink(request.getButtonLink());
        if (request.getType() != null) banner.setType(request.getType());
        if (request.getPriority() != null) banner.setPriority(request.getPriority());
        if (request.getActive() != null) banner.setActive(request.getActive());
        if (request.getStartsAt() != null) banner.setStartsAt(request.getStartsAt());
        if (request.getEndsAt() != null) banner.setEndsAt(request.getEndsAt());

        return toResponse(bannerRepository.save(banner));
    }

    @Transactional
    public void delete(UUID id) {
        if (!bannerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Banner not found");
        }
        bannerRepository.deleteById(id);
    }

    private boolean isVisible(Banner banner) {
        LocalDateTime now = LocalDateTime.now();
        if (banner.getStartsAt() != null && now.isBefore(banner.getStartsAt())) return false;
        if (banner.getEndsAt() != null && now.isAfter(banner.getEndsAt())) return false;
        return Boolean.TRUE.equals(banner.getActive());
    }

    private BannerResponse toResponse(Banner banner) {
        return BannerResponse.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .subtitle(banner.getSubtitle())
                .image(banner.getImage())
                .buttonText(banner.getButtonText())
                .buttonLink(banner.getButtonLink())
                .type(banner.getType())
                .priority(banner.getPriority())
                .active(banner.getActive())
                .startsAt(banner.getStartsAt())
                .endsAt(banner.getEndsAt())
                .createdAt(banner.getCreatedAt())
                .build();
    }
}
