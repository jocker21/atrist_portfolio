package com.example.artist.controller;

import com.example.artist.service.ArtworkService;
import com.example.artist.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ArtworkService artworkService;
    private final BlogService blogService;

    // GET / — главная страница
    @GetMapping("/")
    public String home(Model model) {
        // Последние 6 опубликованных работ для главной страницы
        var artworks = artworkService.getAllPublished();
        model.addAttribute("artworks",
                artworks.size() > 6 ? artworks.subList(0, 6) : artworks);

        // Последние 3 статьи блога
        model.addAttribute("latestPosts", blogService.getLatestPosts());

        return "index";
    }

    // GET /login — страница входа (шаблон login.html)
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
