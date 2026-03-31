package com.example.artist.controller;

import com.example.artist.entity.Artwork;
import com.example.artist.entity.BlogPost;
import com.example.artist.entity.Order;
import com.example.artist.service.ArtworkService;
import com.example.artist.service.BlogService;
import com.example.artist.service.CategoryService;
import com.example.artist.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ArtworkService artworkService;
    private final BlogService blogService;
    private final CategoryService categoryService;
    private final OrderService orderService;

    // ---- ARTWORK MANAGEMENT ----

    @GetMapping("/artworks")
    public String artworkList(Model model) {
        model.addAttribute("artworks", artworkService.getAll());
        return "admin/artworks";
    }

    @GetMapping("/artworks/new")
    public String newArtworkForm(Model model) {
        model.addAttribute("artwork", new Artwork());
        model.addAttribute("categories", categoryService.getAll());
        return "admin/artwork-form";
    }

    @PostMapping("/artworks/new")
    public String saveArtwork(
            @Valid @ModelAttribute("artwork") Artwork artwork,
            BindingResult bindingResult,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
            Model model) throws Exception {

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.getAll());
            return "admin/artwork-form";
        }

        artworkService.save(artwork, imageFile, categoryIds);
        return "redirect:/admin/artworks";
    }

    @GetMapping("/artworks/{id}/edit")
    public String editArtworkForm(@PathVariable Long id, Model model) {
        model.addAttribute("artwork", artworkService.getById(id));
        model.addAttribute("categories", categoryService.getAll());
        return "admin/artwork-form";
    }

    @PostMapping("/artworks/{id}/edit")
    public String updateArtwork(
            @PathVariable Long id,
            @Valid @ModelAttribute("artwork") Artwork artwork,
            BindingResult bindingResult,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
            Model model) throws Exception {

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.getAll());
            return "admin/artwork-form";
        }

        artwork.setId(id);
        artworkService.save(artwork, imageFile, categoryIds);
        return "redirect:/admin/artworks";
    }

    @PostMapping("/artworks/{id}/delete")
    public String deleteArtwork(@PathVariable Long id) {
        artworkService.delete(id);
        return "redirect:/admin/artworks";
    }

    // ---- BLOG MANAGEMENT ----

    @GetMapping("/blog")
    public String blogList(Model model) {
        model.addAttribute("posts", blogService.getPublishedPage(0, 50).getContent());
        return "admin/blog";
    }

    @GetMapping("/blog/new")
    public String newPostForm(Model model) {
        model.addAttribute("post", new BlogPost());
        return "admin/blog-form";
    }

    @PostMapping("/blog/new")
    public String savePost(
            @Valid @ModelAttribute("post") BlogPost post,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "admin/blog-form";
        }

        blogService.save(post);
        return "redirect:/admin/blog";
    }

    @GetMapping("/blog/{id}/edit")
    public String editPost(@PathVariable Long id, Model model) {
        model.addAttribute("post", blogService.getById(id));
        return "admin/blog-form";
    }

    @PostMapping("/blog/{id}/edit")
    public String updatePost(
            @PathVariable Long id,
            @Valid @ModelAttribute("post") BlogPost post,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "admin/blog-form";
        }

        post.setId(id);
        blogService.save(post);
        return "redirect:/admin/blog";
    }

    @PostMapping("/blog/{id}/delete")
    public String deletePost(@PathVariable Long id) {
        blogService.delete(id);
        return "redirect:/admin/blog";
    }

    // ---- ORDER MANAGEMENT ----

    @GetMapping("/orders")
    public String orderList(Model model) {
        model.addAttribute("orders", orderService.getAll());
        return "admin/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getById(id));
        return "admin/order-detail";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(
            @PathVariable Long id,
            @RequestParam Order.OrderStatus status) {
        orderService.updateStatus(id, status);
        return "redirect:/admin/orders";
    }

    @PostMapping("/orders/{id}/delete")
    public String deleteOrder(@PathVariable Long id) {
        orderService.delete(id);
        return "redirect:/admin/orders";
    }
}