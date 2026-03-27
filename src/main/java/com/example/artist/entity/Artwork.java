package com.example.artist.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

// @Entity — говорит JPA: этот класс соответствует таблице в базе данных.
// Hibernate автоматически создаст таблицу 'artwork' (название класса в lowercase).
@Entity
// @Table — явно задаём имя таблицы
@Table(name = "artworks")
// Lombok: генерирует геттеры, сеттеры и конструктор без аргументов.
// Без Lombok пришлось бы писать getTitle(), setTitle() и т.д. вручную.
@Getter
@Setter
@NoArgsConstructor
public class Artwork {

    // @Id — это поле является первичным ключом таблицы
    // @GeneratedValue — база данных сама генерирует значение (1, 2, 3...)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @NotBlank — валидация: поле не может быть пустым
    // @Column — настройки столбца в таблице
    @NotBlank(message = "Название обязательно")
    @Column(nullable = false)
    private String title;

    // TEXT в PostgreSQL — для длинных описаний без ограничения длины
    @Column(columnDefinition = "TEXT")
    private String description;

    // Год создания картины
    @NotNull(message = "Год создания обязателен")
    private Integer year;

    // Техника: масло, акварель, графика и т.д.
    private String technique;

    // Размеры: "60x80 см"
    private String dimensions;

    // Путь к файлу изображения: "uploads/artworks/uuid.jpg"
    private String imageUrl;

    // Дата добавления на сайт — устанавливается автоматически
    private LocalDate createdAt;

    // Видна ли работа в публичной галерее
    private boolean published = true;

    // @ManyToMany — одна картина может принадлежать нескольким категориям,
    // и одна категория содержит много картин.
    // @JoinTable — описывает промежуточную таблицу связей artwork_categories
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "artwork_categories",
        joinColumns = @JoinColumn(name = "artwork_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    // @PrePersist — этот метод вызывается автоматически ПЕРЕД сохранением в БД.
    // Устанавливаем дату добавления.
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
    }
}
