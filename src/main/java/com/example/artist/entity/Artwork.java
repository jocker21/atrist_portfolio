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

    // Матеріал: олія, акварель, акрил, пастель, графіт...
    private String material;

    // Основа: полотно, папір, картон, дерево...
    private String surface;

    // Жанр: портрет, пейзаж, натюрморт, абстракція...
    private String genre;

    // Розміри окремо для зручного пошуку і відображення
    private Integer widthCm;
    private Integer heightCm;

    // Ціна
    @Positive(message = "Ціна має бути більше нуля")
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    // Чи доступна для продажу (false = продана)
    private boolean available = true;

    // Головне фото
    private String imageUrl;

    // Дата додавання на сайт
    private LocalDate createdAt;

    // Видима в публічній галереї
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

    // Зручний метод для відображення розмірів
    public String getDimensions() {
        if (widthCm != null && heightCm != null) {
            return widthCm + " × " + heightCm + " см";
        }
        return null;
    }
}