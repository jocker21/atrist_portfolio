package com.example.artist.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "artworks")
@Getter
@Setter
@NoArgsConstructor
public class Artwork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "{validation.title}")
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String titleEn;

    @Column(columnDefinition = "TEXT")
    private String descriptionEn;

    @NotNull(message = "{validation.year}")
    @Column(nullable = false)
    private Integer year;

    private String material;
    private String surface;
    private String genre;
    private String technique;

    @Positive(message = "{measure.positive}")
    @jakarta.validation.constraints.Digits(integer = 7, fraction = 2)
    @Column(precision = 10, scale = 2)
    private BigDecimal widthCm;
    @Positive(message = "{measure.positive}")
    @jakarta.validation.constraints.Digits(integer = 7, fraction = 2)
    @Column(precision = 10, scale = 2)
    private BigDecimal heightCm;

    @Positive(message = "{validation.price}")
    @jakarta.validation.constraints.Digits(integer = 8, fraction = 2)
    @Column(precision = 10, scale = 2)
    private BigDecimal price;
    @Positive(message = "{validation.price}")
    @jakarta.validation.constraints.Digits(integer = 8, fraction = 2)
    @Column(precision = 10, scale = 2)
    private BigDecimal priceUsd;

    public BigDecimal getWidthInches() { return inches(widthCm); }
    public BigDecimal getHeightInches() { return inches(heightCm); }
    private BigDecimal inches(BigDecimal cm) {
        return cm == null ? null : cm.divide(new BigDecimal("2.54"), 2, java.math.RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private boolean available = true;
    private String imageUrl;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "artwork_photos", joinColumns = @JoinColumn(name = "artwork_id"))
    @OrderColumn(name = "photo_order")
    @Column(name = "image_url", nullable = false)
    private List<String> additionalImages = new ArrayList<>();

    private String videoUrl;
    private LocalDate createdAt;
    private boolean published = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "artwork_categories",
        joinColumns = @JoinColumn(name = "artwork_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
    }

    public String getDimensions() {
        if (widthCm != null && heightCm != null) {
            return widthCm + " × " + heightCm + " см";
        }
        return null;
    }
}
