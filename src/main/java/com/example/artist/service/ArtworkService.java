package com.example.artist.service;

import com.example.artist.entity.Artwork;
import com.example.artist.entity.Category;
import com.example.artist.repository.ArtworkRepository;
import com.example.artist.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Картину не знайдено"));
    }

    public Artwork getPublishedById(Long id) {
        Artwork artwork = getById(id);
        if (!artwork.isPublished()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Картину не знайдено");
        }
        return artwork;
    }

    public Artwork save(Artwork artwork, MultipartFile imageFile, List<Long> categoryIds) throws IOException {
        if (artwork.getId() != null) {
            Artwork existingArtwork = getById(artwork.getId());
            artwork.setCreatedAt(existingArtwork.getCreatedAt());
            artwork.setImageUrl(existingArtwork.getImageUrl());
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            validateImageFile(imageFile);
            String imagePath = saveImage(imageFile);
            artwork.setImageUrl(imagePath);
        }

        if (categoryIds != null && !categoryIds.isEmpty()) {
            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(categoryIds));
            artwork.setCategories(categories);
        } else {
            artwork.setCategories(new HashSet<>());
        }

        return artworkRepository.save(artwork);
    }

    @Transactional
    public void delete(Long id) {
        artworkRepository.findById(id).ifPresent(artwork -> {
            String imageUrl = artwork.getImageUrl();
            artworkRepository.delete(artwork);
            if (imageUrl != null && !imageUrl.isBlank()) {
                // Keep the image if deleting the database record rolls back.
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        deleteImage(imageUrl);
                    }
                });
            }
        });
    }

    private void deleteImage(String imageUrl) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path storedPath = Paths.get(imageUrl);
            if ((storedPath.isAbsolute() && !storedPath.normalize().startsWith(uploadPath))
                    || storedPath.normalize().startsWith("..")) {
                log.warn("Skipping artwork image outside upload directory: {}", imageUrl);
                return;
            }
            Path filename = storedPath.getFileName();
            if (filename == null) {
                return;
            }
            Path filePath = uploadPath.resolve(filename).normalize();
            if (filePath.startsWith(uploadPath) && !filePath.equals(uploadPath)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException | InvalidPathException ex) {
            log.warn("Could not delete artwork image {}", imageUrl, ex);
        }
    }

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
        try (var input = file.getInputStream()) {
            Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        return "uploads/artworks/" + filename;
    }
}
