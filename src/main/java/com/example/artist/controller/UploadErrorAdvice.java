package com.example.artist.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.ui.Model;
import com.example.artist.service.ArtistProfileService;
import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class UploadErrorAdvice {
    private final ArtistProfileService profiles;
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String oversizedUpload(Model model) {
        // Multipart parsing can fail before the usual shared model attributes are populated.
        model.addAttribute("artistProfile", profiles.get());
        model.addAttribute("languageUkUrl", "/admin/artworks?lang=uk");
        model.addAttribute("languageEnUrl", "/admin/artworks?lang=en");
        return "upload-error";
    }
}
