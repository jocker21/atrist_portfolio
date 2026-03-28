
package com.example.artist.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ім'я покупця
    @NotBlank(message = "Ім'я обов'язкове")
    private String customerName;

    // Email для підтвердження
    @Email(message = "Невірний формат email")
    @NotBlank(message = "Email обов'язковий")
    private String customerEmail;

    // Телефон для зв'язку
    private String customerPhone;

    // Повідомлення від покупця
    @Column(columnDefinition = "TEXT")
    private String message;

    // Картина яку замовляють
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", nullable = false)
    private Artwork artwork;

    // Статус замовлення
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.NEW;

    // Дата замовлення
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum OrderStatus {
        NEW,        // нове, ще не переглянуте
        SEEN,       // художник переглянув
        CONFIRMED,  // підтверджено
        CANCELLED   // скасовано
    }
}