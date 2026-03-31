package com.example.artist.controller;

import com.example.artist.entity.Artwork;
import com.example.artist.entity.Order;
import com.example.artist.service.ArtworkService;
import com.example.artist.service.OrderService;
import jakarta.validation.Valid;
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
        Artwork artwork = artworkService.getById(id);
        if (!artwork.isAvailable()) {
            return "redirect:/gallery/" + id;
        }
        model.addAttribute("artwork", artwork);
        model.addAttribute("order", new Order());
        return "order";
    }

    @PostMapping("/{id}")
    public String submitOrder(
            @PathVariable Long id,
            @Valid @ModelAttribute("order") Order order,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("artwork", artworkService.getById(id));
            return "order";
        }

        Artwork artwork = artworkService.getById(id);
        order.setArtwork(artwork);
        orderService.save(order);

        return "redirect:/order/success";
    }

    @GetMapping("/success")
    public String success() {
        return "order-success";
    }
}
