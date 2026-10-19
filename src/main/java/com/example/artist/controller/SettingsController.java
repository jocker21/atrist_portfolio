package com.example.artist.controller;
import com.example.artist.service.ArtworkTerms;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/admin/settings") @RequiredArgsConstructor
public class SettingsController {
    private final ArtworkTerms terms;
    @GetMapping public String settings(@RequestParam(defaultValue="material") String kind, Model model) {
        if (!ArtworkTerms.KINDS.contains(kind)) kind = "material";
        model.addAttribute("kind", kind);
        model.addAttribute("terms", terms.options(kind));
        return "admin/settings";
    }
    @PostMapping public String save(@RequestParam String kind, @RequestParam String value,
                                    @RequestParam String english, RedirectAttributes redirect) {
        try { terms.save(kind, value, english); redirect.addFlashAttribute("saved", true); }
        catch (IllegalArgumentException ex) { redirect.addFlashAttribute("settingsError", true); }
        return "redirect:/admin/settings?kind=" + (ArtworkTerms.KINDS.contains(kind) ? kind : "material");
    }
}
