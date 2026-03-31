package com.example.artist.service;

import com.example.artist.entity.Order;
import com.example.artist.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    // Зберегти нове замовлення
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    // Всі замовлення для адмінки
    public List<Order> getAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    // Знайти замовлення по id
    public Order getById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Замовлення не знайдено: " + id));
    }

    // Змінити статус замовлення
    public void updateStatus(Long id, Order.OrderStatus status) {
        Order order = getById(id);
        order.setStatus(status);
        orderRepository.save(order);
    }

    // Видалити замовлення
    public void delete(Long id) {
        orderRepository.deleteById(id);
    }
}
