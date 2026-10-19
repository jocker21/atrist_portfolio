package com.example.artist.service;

import com.example.artist.entity.Artwork;
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
import java.util.HashSet;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final CategoryRepository categoryRepository;
    private final ArtworkMediaStorage mediaStorage;

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

    @Transactional(rollbackFor = IOException.class)
    public Artwork save(Artwork artwork, MultipartFile imageFile, List<Long> categoryIds) throws IOException {
        return save(artwork, imageFile, categoryIds, List.of(), null, List.of(), false);
    }

    @Transactional(rollbackFor = IOException.class)
    public Artwork save(Artwork submitted, MultipartFile imageFile, List<Long> categoryIds,
                        List<MultipartFile> photos, MultipartFile video,
                        List<String> removePhotos, boolean removeVideo) throws IOException {
        Artwork target = submitted.getId() == null ? new Artwork() : artworkRepository.findForUpdate(submitted.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var retained = new java.util.ArrayList<>(target.getAdditionalImages());
        var removed = new java.util.ArrayList<String>();
        for (String url : retained) {
            if (removePhotos != null && removePhotos.contains(url)) removed.add(url);
        }
        retained.removeAll(removed);
        var additions = photos == null ? List.<MultipartFile>of() : photos.stream().filter(f -> !f.isEmpty()).toList();
        if (retained.size() + additions.size() > 6) throw new IllegalArgumentException("media.tooMany");

        target.setTitle(submitted.getTitle());
        target.setTitleEn(submitted.getTitleEn());
        target.setDescription(submitted.getDescription());
        target.setDescriptionEn(submitted.getDescriptionEn());
        target.setYear(submitted.getYear());
        target.setMaterial(submitted.getMaterial());
        target.setSurface(submitted.getSurface());
        target.setGenre(submitted.getGenre());
        target.setTechnique(submitted.getTechnique());
        target.setWidthCm(submitted.getWidthCm());
        target.setHeightCm(submitted.getHeightCm());
        target.setPrice(submitted.getPrice());
        target.setPriceUsd(submitted.getPriceUsd());
        target.setAvailable(submitted.isAvailable());
        target.setPublished(submitted.isPublished());
        target.setCategories(categoryIds == null ? new HashSet<>() : new HashSet<>(categoryRepository.findAllById(categoryIds)));

        if (imageFile != null && !imageFile.isEmpty()) {
            String old = target.getImageUrl();
            target.setImageUrl(mediaStorage.image(imageFile));
            mediaStorage.removeAfterCommit(old);
        }
        for (MultipartFile photo : additions) retained.add(mediaStorage.image(photo));
        target.getAdditionalImages().clear();
        target.getAdditionalImages().addAll(retained);
        removed.forEach(mediaStorage::removeAfterCommit);
        if (video != null && !video.isEmpty()) {
            String old = target.getVideoUrl();
            target.setVideoUrl(mediaStorage.video(video));
            mediaStorage.removeAfterCommit(old);
        } else if (removeVideo) {
            mediaStorage.removeAfterCommit(target.getVideoUrl());
            target.setVideoUrl(null);
        }
        return artworkRepository.saveAndFlush(target);
    }

    @Transactional
    public void delete(Long id) {
        artworkRepository.findById(id).ifPresent(artwork -> {
            String imageUrl = artwork.getImageUrl();
            artwork.getAdditionalImages().forEach(mediaStorage::removeAfterCommit);
            mediaStorage.removeAfterCommit(artwork.getVideoUrl());
            artworkRepository.delete(artwork);
            if (imageUrl != null && !imageUrl.isBlank()) {
                // Keep the image if deleting the database record rolls back.
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        if (artworkRepository.countMediaReferences(imageUrl) == 0) deleteImage(imageUrl);
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

}
