package com.example.artist.controller;

import com.example.artist.entity.Artwork;
import com.example.artist.entity.Order;
import com.example.artist.service.ArtworkService;
import com.example.artist.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ArtworkService artworkService;

    @GetMapping("/{id}")
    public String orderForm(@PathVariable Long id, Model model) {
        Artwork artwork = artworkService.getPublishedById(id);
        if (!artwork.isAvailable()) {
            return "redirect:/gallery/" + id;
        }
        model.addAttribute("artwork", artwork);
        model.addAttribute("order", new OrderForm());
        return "order";
    }

    @PostMapping("/{id}")
    public String submitOrder(
            @PathVariable Long id,
            @Valid @ModelAttribute("order") OrderForm form,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("artwork", artworkService.getPublishedById(id));
            return "order";
        }

        Artwork artwork = artworkService.getPublishedById(id);
        if (!artwork.isAvailable()) {
            return "redirect:/gallery/" + id;
        }
        Order order = new Order();
        order.setCustomerName(form.getCustomerName());
        order.setCustomerEmail(form.getCustomerEmail());
        order.setCustomerPhone(form.getCustomerPhone());
        order.setMessage(form.getMessage());
        order.setArtwork(artwork);
        orderService.save(order);

        return "redirect:/order/success";
    }

    @GetMapping("/success")
    public String success() {
        return "order-success";
    }

    @Data
    public static class OrderForm {
        @NotBlank(message = "Ім'я обов'язкове")
        private String customerName;

        @Email(message = "Невірний формат email")
        @NotBlank(message = "Email обов'язковий")
        private String customerEmail;

        private String customerPhone;
        private String message;
    }
}
