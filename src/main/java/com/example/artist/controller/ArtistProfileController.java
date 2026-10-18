package com.example.artist.controller;

import com.example.artist.entity.ArtistProfile;
import com.example.artist.service.ArtistProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class ArtistProfileController {
    private final ArtistProfileService profiles;

    @InitBinder("profileForm")
    public void profileFields(WebDataBinder binder) {
        binder.setAllowedFields("nameUk", "nameEn", "introUk", "introEn", "biographyUk", "biographyEn");
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/admin/profile")
    public String edit(Model model) {
        model.addAttribute("profileForm", profiles.get());
        return "admin/profile";
    }

    @PostMapping("/admin/profile")
    public String save(@Valid @ModelAttribute("profileForm") ArtistProfile profile,
                       BindingResult errors,
                       @RequestParam(value = "portrait", required = false) MultipartFile portrait,
                       Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            return "admin/profile";
        }
        try {
            profiles.save(profile, portrait);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("portraitError", ex.getMessage());
            return "admin/profile";
        } catch (IOException ex) {
            model.addAttribute("portraitError", "profile.imageFailure");
            return "admin/profile";
        }
        redirect.addFlashAttribute("profileSaved", true);
        return "redirect:/admin/profile";
    }
}
