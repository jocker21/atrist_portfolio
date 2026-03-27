package com.example.artist.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "blog_posts")
@Getter
@Setter
@NoArgsConstructor
public class BlogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Заголовок статьи
    @NotBlank(message = "Заголовок обязателен")
    @Column(nullable = false)
    private String title;

    // Slug для URL: "moya-pervaya-vystavka-2024"
    @Column(unique = true, nullable = false)
    private String slug;

    // Краткое описание для карточки в списке блога
    @Column(columnDefinition = "TEXT")
    private String excerpt;

    // Полный текст статьи (может быть очень длинным)
    @NotBlank(message = "Текст статьи обязателен")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // Обложка статьи
    private String coverImageUrl;

    // Дата публикации — null если статья ещё черновик
    private LocalDateTime publishedAt;

    // Черновик или опубликована
    private boolean published = false;

    // Дата создания (всегда заполняется)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        // Если статья сразу публикуется — ставим дату публикации
        if (this.published && this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // При обновлении: если только что опубликовали — фиксируем дату
        if (this.published && this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
    }
}
