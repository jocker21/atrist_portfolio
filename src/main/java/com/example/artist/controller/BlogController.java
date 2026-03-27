package com.example.artist.controller;

import com.example.artist.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/blog")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    // Список статей с пагинацией: /blog?page=0
    @GetMapping
    public String blog(@RequestParam(defaultValue = "0") int page, Model model) {
        // getPublishedPage(page, 6) — 6 статей на странице
        model.addAttribute("postsPage", blogService.getPublishedPage(page, 6));
        return "blog";
    }

    // Отдельная статья: /blog/moya-pervaya-vystavka
    @GetMapping("/{slug}")
    public String post(@PathVariable String slug, Model model) {
        model.addAttribute("post", blogService.getBySlug(slug));
        return "blog-post";
    }
}
