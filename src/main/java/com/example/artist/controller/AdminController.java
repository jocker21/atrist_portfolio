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
import org.springframework.web.bind.WebDataBinder;
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

    @InitBinder("artwork")
    public void artworkFields(WebDataBinder binder) {
        binder.setAllowedFields("title", "titleEn", "description", "descriptionEn", "year", "material",
                "surface", "genre", "technique", "widthCm", "heightCm", "price", "priceUsd", "available", "published");
    }

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
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
            @RequestParam(value = "additionalPhotos", required = false) List<MultipartFile> photos,
            @RequestParam(value = "videoFile", required = false) MultipartFile video,
            @RequestParam(value = "inputUnit", defaultValue = "cm") String unit,
            Model model) throws Exception {
        return saveArtworkForm(artwork, bindingResult, imageFile, categoryIds, photos, video, List.of(), false, unit, model);
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
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
            @RequestParam(value = "additionalPhotos", required = false) List<MultipartFile> photos,
            @RequestParam(value = "videoFile", required = false) MultipartFile video,
            @RequestParam(value = "removePhotos", required = false) List<String> removePhotos,
            @RequestParam(value = "removeVideo", defaultValue = "false") boolean removeVideo,
            @RequestParam(value = "inputUnit", defaultValue = "cm") String unit,
            Model model) throws Exception {
        artwork.setId(id);
        return saveArtworkForm(artwork, bindingResult, imageFile, categoryIds, photos, video, removePhotos, removeVideo, unit, model);
    }

    private String saveArtworkForm(Artwork artwork, BindingResult errors, MultipartFile image,
                                   List<Long> categories, List<MultipartFile> photos, MultipartFile video,
                                   List<String> removePhotos, boolean removeVideo, String unit, Model model) {
        var width = artwork.getWidthCm();
        var height = artwork.getHeightCm();
        if (!errors.hasErrors()) {
            try {
                if (!unit.equals("cm") && !unit.equals("in")) throw new IllegalArgumentException("measure.invalid");
                if (unit.equals("in")) {
                    if (width != null) artwork.setWidthCm(width.multiply(new java.math.BigDecimal("2.54")).setScale(2, java.math.RoundingMode.HALF_UP));
                    if (height != null) artwork.setHeightCm(height.multiply(new java.math.BigDecimal("2.54")).setScale(2, java.math.RoundingMode.HALF_UP));
                }
                artworkService.save(artwork, image, categories, photos, video, removePhotos, removeVideo);
                return "redirect:/admin/artworks";
            } catch (IllegalArgumentException ex) {
                model.addAttribute("mediaError", ex.getMessage());
            } catch (java.io.IOException ex) {
                model.addAttribute("mediaError", "media.failure");
            }
        }
        artwork.setWidthCm(width);
        artwork.setHeightCm(height);
        if (artwork.getId() != null) {
            Artwork saved = artworkService.getById(artwork.getId());
            artwork.setImageUrl(saved.getImageUrl());
            artwork.setAdditionalImages(new java.util.ArrayList<>(saved.getAdditionalImages()));
            artwork.setVideoUrl(saved.getVideoUrl());
        }
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("selectedCategoryIds", categories == null ? List.of() : categories);
        model.addAttribute("inputUnit", unit.equals("in") ? "in" : "cm");
        model.addAttribute("uploadsMustReselect", true);
        return "admin/artwork-form";
    }

    @PostMapping("/artworks/{id}/delete")
    public String deleteArtwork(@PathVariable Long id) {
        artworkService.delete(id);
        return "redirect:/admin/artworks";
    }

    // ---- BLOG MANAGEMENT ----

    @GetMapping("/blog")
    public String blogList(Model model) {
        model.addAttribute("posts", blogService.getAll());
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
