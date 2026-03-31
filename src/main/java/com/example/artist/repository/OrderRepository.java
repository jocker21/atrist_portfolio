package com.example.artist.repository;

import com.example.artist.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findByStatus(Order.OrderStatus status);

    List<Order> findByArtworkId(Long artworkId);
}
