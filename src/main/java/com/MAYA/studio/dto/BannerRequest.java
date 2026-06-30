package com.MAYA.studio.dto;

import com.MAYA.studio.entity.Banner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerRequest {
    private String title;
    private String subtitle;
    private String image;
    private String buttonText;
    private String buttonLink;
    private Banner.BannerType type;
    private Integer priority;
    private Boolean active;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
}
