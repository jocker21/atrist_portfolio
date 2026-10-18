package com.example.artist;

import com.example.artist.entity.Artwork;
import com.example.artist.entity.Order;
import com.example.artist.repository.ArtworkRepository;
import com.example.artist.repository.OrderRepository;
import com.example.artist.service.ArtworkService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ArtworkDeletionTests {
    @TempDir static Path uploadDirectory;
    @Autowired ArtworkRepository artworks;
    @Autowired OrderRepository orders;
    @Autowired ArtworkService service;
    @Autowired PlatformTransactionManager transactionManager;
    private final List<Long> artworkIds = new ArrayList<>();
    private final List<Long> orderIds = new ArrayList<>();

    @DynamicPropertySource
    static void uploads(DynamicPropertyRegistry registry) {
        registry.add("app.upload.dir", () -> uploadDirectory.toString());
    }

    @AfterEach
    void removeTestRecords() {
        orderIds.forEach(orders::deleteById);
        artworkIds.forEach(artworks::deleteById);
    }

    private Artwork artwork(String imageUrl) {
        Artwork artwork = new Artwork();
        artwork.setTitle("Image deletion regression");
        artwork.setYear(2026);
        artwork.setImageUrl(imageUrl);
        artwork = artworks.saveAndFlush(artwork);
        artworkIds.add(artwork.getId());
        return artwork;
    }

    @Test
    void deletingArtworkRemovesImageFromConfiguredDirectory() throws Exception {
        Path image = Files.writeString(uploadDirectory.resolve("committed.jpg"), "fixture");
        Artwork artwork = artwork("uploads/artworks/committed.jpg");
        service.delete(artwork.getId());
        assertThat(artworks.existsById(artwork.getId())).isFalse();
        assertThat(image).doesNotExist();
    }

    @Test
    void rollbackKeepsArtworkAndImage() throws Exception {
        Path image = Files.writeString(uploadDirectory.resolve("rollback.jpg"), "fixture");
        Artwork artwork = artwork("uploads/artworks/rollback.jpg");
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            service.delete(artwork.getId());
            assertThat(image).exists();
            status.setRollbackOnly();
        });
        assertThat(artworks.existsById(artwork.getId())).isTrue();
        assertThat(image).exists();
    }

    @Test
    void linkedOrderPreventsDeletionWithoutLosingImage() throws Exception {
        Path image = Files.writeString(uploadDirectory.resolve("ordered.jpg"), "fixture");
        Artwork artwork = artwork("uploads/artworks/ordered.jpg");
        Order order = new Order();
        order.setArtwork(artwork);
        order.setCustomerName("Customer");
        order.setCustomerEmail("customer@example.com");
        orderIds.add(orders.saveAndFlush(order).getId());
        assertThatThrownBy(() -> service.delete(artwork.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(artworks.existsById(artwork.getId())).isTrue();
        assertThat(image).exists();
    }

    @Test
    void deletionDoesNotFollowStoredPathsOutsideUploadDirectory(@TempDir Path outside) throws Exception {
        Path outsideImage = Files.writeString(outside.resolve("outside.jpg"), "outside fixture");
        Path insideImage = Files.writeString(uploadDirectory.resolve("outside.jpg"), "inside fixture");
        Artwork absolutePath = artwork(outsideImage.toString());
        service.delete(absolutePath.getId());
        Artwork relativePath = artwork("../../outside.jpg");
        service.delete(relativePath.getId());
        assertThat(outsideImage).exists();
        assertThat(insideImage).exists();
    }
}
