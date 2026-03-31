package com.example.artist.repository;

import com.example.artist.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Всі замовлення відсортовані від нових до старих
    List<Order> findAllByOrderByCreatedAtDesc();

    // Замовлення по статусу (наприклад всі нові)
    List<Order> findByStatus(Order.OrderStatus status);

    // Замовлення по конкретній картині
    List<Order> findByArtworkId(Long artworkId);
}
