package com.example.artist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "artist_profile")
@Getter
@Setter
@NoArgsConstructor
public class ArtistProfile {
    @Id
    private Long id = 1L;

    @NotBlank(message = "{validation.name}")
    @Size(max = 80, message = "{validation.nameLength}")
    private String nameUk;

    @NotBlank(message = "{validation.name}")
    @Size(max = 80, message = "{validation.nameLength}")
    private String nameEn;

    @Size(max = 800, message = "{validation.introLength}")
    @Column(length = 800)
    private String introUk;

    @Size(max = 800, message = "{validation.introLength}")
    @Column(length = 800)
    private String introEn;

    @Size(max = 12000, message = "{validation.bioLength}")
    @Column(columnDefinition = "TEXT")
    private String biographyUk;

    @Size(max = 12000, message = "{validation.bioLength}")
    @Column(columnDefinition = "TEXT")
    private String biographyEn;

    private String portraitUrl;
}
