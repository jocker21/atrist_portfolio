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

@Entity
@Table(name = "artworks")
@Getter
@Setter
@NoArgsConstructor
public class Artwork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Назва обов'язкова")
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Рік створення обов'язковий")
    private Integer year;

    private String material;
    private String surface;
    private String genre;

    private Integer widthCm;
    private Integer heightCm;

    @Positive(message = "Ціна має бути більше нуля")
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private boolean available = true;
    private String imageUrl;
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
