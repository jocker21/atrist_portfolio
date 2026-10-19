package com.example.artist.controller;

import jakarta.servlet.http.HttpServletRequest;
import com.example.artist.service.ArtistProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.util.UriComponentsBuilder;

@ControllerAdvice
@RequiredArgsConstructor
public class LanguageAdvice {
    private final ArtistProfileService profiles;

    @ModelAttribute
    public void languageLinks(HttpServletRequest request, Model model) {
        model.addAttribute("artistProfile", profiles.get());
        model.addAttribute("termKinds", java.util.List.of("material","surface","technique","genre"));
        model.addAttribute("socialNames", java.util.List.of("instagram","telegram","facebook","threads"));
        // Preserve gallery filters and pagination when changing language.
        var uri = UriComponentsBuilder.fromPath(request.getRequestURI()).query(request.getQueryString());
        model.addAttribute("languageUkUrl", uri.cloneBuilder().replaceQueryParam("lang", "uk").build().toUriString());
        model.addAttribute("languageEnUrl", uri.cloneBuilder().replaceQueryParam("lang", "en").build().toUriString());
    }
}
