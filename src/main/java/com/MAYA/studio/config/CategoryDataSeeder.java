package com.MAYA.studio.config;

import com.MAYA.studio.entity.Banner;
import com.MAYA.studio.entity.Category;
import com.MAYA.studio.entity.Coupon;
import com.MAYA.studio.repository.BannerRepository;
import com.MAYA.studio.repository.CategoryRepository;
import com.MAYA.studio.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryDataSeeder implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final CouponRepository couponRepository;
    private final BannerRepository bannerRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedCategories();
        seedCoupons();
        seedBanners();
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }

        String[][] defaults = {
                {"Tunics", "tunics", "TUNICS", "Elegant tunics for every occasion"},
                {"Top & Bottoms", "top-and-bottoms", "TOP_AND_BOTTOMS", "Coordinated tops and bottoms"},
                {"Co-ords", "co-ords", "CO_ORDS", "Stylish co-ordinated sets"},
                {"Dresses", "dresses", "DRESSES", "Premium dresses collection"},
        };

        for (int i = 0; i < defaults.length; i++) {
            categoryRepository.save(Category.builder()
                    .name(defaults[i][0])
                    .slug(defaults[i][1])
                    .code(defaults[i][2])
                    .description(defaults[i][3])
                    .displayOrder(i + 1)
                    .enabled(true)
                    .featured(i < 2)
                    .showOnHomepage(true)
                    .build());
        }
        log.info("Seeded {} categories", defaults.length);
    }

    private void seedCoupons() {
        if (couponRepository.count() > 0) {
            return;
        }

        couponRepository.save(Coupon.builder()
                .code("MAYA10")
                .description("10% off on all orders")
                .type(Coupon.CouponType.PERCENTAGE)
                .value(BigDecimal.TEN)
                .minCartAmount(BigDecimal.ZERO)
                .usageLimit(10000)
                .active(true)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusYears(1))
                .build());
        log.info("Seeded default coupon MAYA10");
    }

    private void seedBanners() {
        if (bannerRepository.count() > 0) {
            return;
        }

        String[][] banners = {
                {"HOUSE OF MAYA", "Where Tradition Meets Modern Elegance", "/banners/1.jpeg", "Shop Collection", "/shop"},
                {"New Arrivals", "Fresh Styles for Every Occasion", "/banners/2.jpeg", "Explore Now", "/shop?sort=new-arrivals"},
                {"Summer Collection", "Light, Elegant & Timeless", "/banners/3.jpeg", "View Collection", "/shop"},
        };

        for (int i = 0; i < banners.length; i++) {
            bannerRepository.save(Banner.builder()
                    .title(banners[i][0])
                    .subtitle(banners[i][1])
                    .image(banners[i][2])
                    .buttonText(banners[i][3])
                    .buttonLink(banners[i][4])
                    .type(Banner.BannerType.HERO)
                    .priority(i)
                    .active(true)
                    .build());
        }
        log.info("Seeded {} hero banners", banners.length);
    }
}
