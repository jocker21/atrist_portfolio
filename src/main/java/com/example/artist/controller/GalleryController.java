package com.example.artist.controller;

import com.example.artist.service.ArtworkService;
import com.example.artist.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// @Controller — этот класс обрабатывает HTTP-запросы и возвращает HTML-страницы.
// (В отличие от @RestController, который возвращает JSON)
@Controller
// @RequestMapping — все методы этого класса обрабатывают URL начинающиеся с /gallery
@RequestMapping("/gallery")
@RequiredArgsConstructor
public class GalleryController {

    private final ArtworkService artworkService;
    private final CategoryService categoryService;

    // @GetMapping — метод вызывается при GET-запросе на /gallery
    // Model — объект для передачи данных в Thymeleaf-шаблон
    @GetMapping
    public String gallery(
            // @RequestParam — параметр из URL: /gallery?category=oil&year=2023
            // required = false — параметр необязателен
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer year,
            Model model) {

        // Передаём все категории в шаблон (для меню фильтров)
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("years", artworkService.getAllYears());

        // Применяем фильтры
        if (category != null && !category.isBlank()) {
            model.addAttribute("artworks", artworkService.getByCategory(category));
            model.addAttribute("activeCategory", category);
        } else if (year != null) {
            model.addAttribute("artworks", artworkService.getByYear(year));
            model.addAttribute("activeYear", year);
        } else {
            model.addAttribute("artworks", artworkService.getAllPublished());
        }

        // Возвращаем имя шаблона.
        // Spring ищет файл: src/main/resources/templates/gallery.html
        return "gallery";
    }

    // Страница отдельной работы: /gallery/42
    @GetMapping("/{id}")
    public String artworkDetail(@PathVariable Long id, Model model) {
        model.addAttribute("artwork", artworkService.getById(id));
        return "artwork-detail";
    }
}
