package com.example.artist.controller;
import com.example.artist.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.Locale;

@Controller @RequestMapping("/gallery") @RequiredArgsConstructor
public class GalleryController {
    private final ArtworkService artworkService;
    private final CategoryService categoryService;
    private final ArtworkTerms terms;
    private final GallerySearch search;
    @GetMapping
    public String gallery(@ModelAttribute("filters") GalleryFilter filters, BindingResult errors, Locale locale, Model model) {
        if (!"USD".equals(filters.getCurrency())) filters.setCurrency("UAH");
        if (filters.getSort() == null) filters.setSort("newest");
        if (!java.util.Set.of("newest","oldest","priceAsc","priceDesc","title").contains(filters.getSort())) filters.setSort("newest");
        if (!java.util.Set.of("all","available","sold").contains(filters.getAvailability())) filters.setAvailability("all");
        boolean invalid = errors.hasErrors() || !filters.valid();
        model.addAttribute("filterError", invalid);
        model.addAttribute("artworks", invalid ? java.util.List.of() : search.search(filters, locale));
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("years", artworkService.getAllYears());
        java.util.Map<String,java.util.List<String>> values = new java.util.LinkedHashMap<>();
        for (String kind : ArtworkTerms.KINDS) values.put(kind, terms.used(kind, true));
        model.addAttribute("filterValues", values);
        model.addAttribute("activeCategory", filters.getCategory());
        model.addAttribute("activeYear", filters.getYear());
        return "gallery";
    }
    @GetMapping("/{id}")
    public String artworkDetail(@PathVariable Long id, Model model) {
        model.addAttribute("artwork", artworkService.getPublishedById(id));
        return "artwork-detail";
    }
}
