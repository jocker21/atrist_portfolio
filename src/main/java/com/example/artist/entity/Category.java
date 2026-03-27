package com.example.artist.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Название категории: "Живопись", "Графика", "Портрет"
    @NotBlank(message = "Название категории обязательно")
    @Column(nullable = false, unique = true)
    private String name;

    // Slug — часть URL: "zhivopis", "grafika".
    // Нужен для красивых адресов: /gallery?category=zhivopis
    @Column(unique = true, nullable = false)
    private String slug;

    // Описание категории (необязательно)
    private String description;

    // mappedBy — говорит JPA, что связь уже описана в Artwork.categories.
    // Здесь мы просто "смотрим" на неё с другой стороны.
    @ManyToMany(mappedBy = "categories")
    private Set<Artwork> artworks = new HashSet<>();
}
