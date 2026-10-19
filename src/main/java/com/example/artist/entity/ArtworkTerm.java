package com.example.artist.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "artwork_terms", uniqueConstraints = @UniqueConstraint(columnNames = {"kind", "value_uk"}))
@Getter @Setter
public class ArtworkTerm {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 16)
    private String kind;
    @Column(name = "value_uk", nullable = false, length = 80)
    private String value;
    @Column(length = 80)
    private String english;
}
