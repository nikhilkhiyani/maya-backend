package com.MAYA.studio.controller;

import com.MAYA.studio.dto.ExchangeRequestDto;
import com.MAYA.studio.dto.ExchangeResponse;
import com.MAYA.studio.service.ExchangeService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/exchanges")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class ExchangeController {

    private final ExchangeService exchangeService;

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ExchangeResponse> createExchange(
            @RequestParam UUID orderId,
            @RequestParam UUID productId,
            @RequestParam String reason,
            @RequestParam String description,
            @RequestPart("images") List<MultipartFile> images,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        ExchangeRequestDto dto = new ExchangeRequestDto();
        dto.setOrderId(orderId);
        dto.setProductId(productId);
        dto.setReason(com.MAYA.studio.entity.ExchangeRequest.ExchangeReason.valueOf(reason));
        dto.setDescription(description);
        return ResponseEntity.ok(exchangeService.createExchange(dto, images, files));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<ExchangeResponse>> getMyExchanges() {
        return ResponseEntity.ok(exchangeService.getMyExchanges());
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ExchangeResponse>> getAllExchanges() {
        return ResponseEntity.ok(exchangeService.getAllExchanges());
    }

    @PutMapping("/admin/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExchangeResponse> reviewExchange(
            @PathVariable UUID id,
            @RequestParam boolean approved,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(exchangeService.reviewExchange(id, approved, remarks));
    }
}
