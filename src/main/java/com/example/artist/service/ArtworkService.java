package com.example.artist.service;

import com.example.artist.entity.Artwork;
import com.example.artist.entity.Category;
import com.example.artist.repository.ArtworkRepository;
import com.example.artist.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final CategoryRepository categoryRepository;

    @Value("${app.upload.dir:uploads/artworks}")
    private String uploadDir;

    public List<Artwork> getAllPublished() {
        return artworkRepository.findByPublishedTrue();
    }

    public List<Artwork> getAll() {
        return artworkRepository.findAll();
    }

    public List<Artwork> getByCategory(String categorySlug) {
        return artworkRepository.findByCategoriesSlugAndPublishedTrue(categorySlug);
    }

    public List<Artwork> getByYear(Integer year) {
        return artworkRepository.findByYearAndPublishedTrue(year);
    }

    public List<Integer> getAllYears() {
        return artworkRepository.findAllPublishedYears();
    }

    public Artwork getById(Long id) {
        return artworkRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Картину не знайдено: " + id));
    }

    // Зберегти картину з файлом і категоріями
    public Artwork save(Artwork artwork, MultipartFile imageFile, List<Long> categoryIds) throws IOException {

        // Зберігаємо зображення якщо передано
        if (imageFile != null && !imageFile.isEmpty()) {
            validateImageFile(imageFile);
            String imagePath = saveImage(imageFile);
            artwork.setImageUrl(imagePath);
        }

        // Прив'язуємо категорії
        if (categoryIds != null && !categoryIds.isEmpty()) {
            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(categoryIds));
            artwork.setCategories(categories);
        } else {
            artwork.setCategories(new HashSet<>());
        }

        return artworkRepository.save(artwork);
    }

    public void delete(Long id) {
        artworkRepository.deleteById(id);
    }

    // Перевірка що файл є зображенням
    private void validateImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Дозволено лише зображення");
        }
    }

    private String saveImage(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String filename = UUID.randomUUID() + extension;

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        return uploadDir + "/" + filename;
    }
}