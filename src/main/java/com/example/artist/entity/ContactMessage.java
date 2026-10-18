package com.example.artist.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "contact_messages")
@Getter @Setter
public class ContactMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    @Column(columnDefinition = "TEXT")
    private String message;
    private LocalDateTime createdAt;
    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
