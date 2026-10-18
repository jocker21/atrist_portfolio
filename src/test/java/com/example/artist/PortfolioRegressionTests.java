package com.example.artist;

import com.example.artist.entity.Artwork;
import com.example.artist.entity.BlogPost;
import com.example.artist.entity.Order;
import com.example.artist.repository.ArtworkRepository;
import com.example.artist.repository.OrderRepository;
import com.example.artist.repository.UserRepository;
import com.example.artist.service.ArtworkService;
import com.example.artist.service.BlogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PortfolioRegressionTests {
    @Autowired MockMvc mvc;
    @Autowired ArtworkRepository artworks;
    @Autowired OrderRepository orders;
    @Autowired UserRepository users;
    @Autowired ArtworkService artworkService;
    @Autowired BlogService blogService;

    private Artwork artwork(boolean available) {
        Artwork artwork = new Artwork();
        artwork.setTitle("Regression artwork");
        artwork.setYear(2024);
        artwork.setMaterial("Oil");
        artwork.setAvailable(available);
        artwork.setImageUrl("uploads/artworks/example.jpg");
        return artworks.saveAndFlush(artwork);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void populatedPagesRenderAndUseCorrectImageUrls() throws Exception {
        Artwork artwork = artwork(true);
        for (String url : List.of("/", "/gallery", "/admin/artworks",
                "/admin/artworks/" + artwork.getId() + "/edit")) {
            mvc.perform(get(url)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("/uploads/artworks/example.jpg")));
        }
        for (String url : List.of("/gallery/" + artwork.getId(), "/order/" + artwork.getId())) {
            mvc.perform(get(url)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("/uploads/artworks/example.jpg")));
        }
    }

    @Test
    void editingArtworkWithoutUploadPreservesImageAndCreationDate() throws Exception {
        Artwork existing = artwork(true);
        String imageUrl = existing.getImageUrl();
        var createdAt = existing.getCreatedAt();
        Artwork edit = new Artwork();
        edit.setId(existing.getId());
        edit.setTitle("Edited artwork");
        edit.setYear(2025);
        Artwork saved = artworkService.save(edit, null, List.of());
        artworks.flush();
        assertThat(saved.getImageUrl()).isEqualTo(imageUrl);
        assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
        assertThat(saved.getTitle()).isEqualTo("Edited artwork");
    }

    @Test
    void unavailableArtworkCannotBeOrderedByPostingDirectly() throws Exception {
        Artwork artwork = artwork(false);
        long count = orders.count();
        mvc.perform(post("/order/" + artwork.getId()).with(csrf())
                        .param("customerName", "Customer").param("customerEmail", "customer@example.com"))
                .andExpect(redirectedUrl("/gallery/" + artwork.getId()));
        assertThat(orders.count()).isEqualTo(count);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void customerCannotSetOrderStatusAndAdminCanViewOrder() throws Exception {
        Artwork artwork = artwork(true);
        mvc.perform(post("/order/" + artwork.getId()).with(csrf())
                        .param("customerName", "Customer").param("customerEmail", "customer@example.com")
                        .param("status", "CONFIRMED"))
                .andExpect(redirectedUrl("/order/success"));
        Order order = orders.findAll().getLast();
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.NEW);
        mvc.perform(get("/admin/orders/" + order.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("customer@example.com")));
    }

    @Test
    void uploadedImagesAreServed() throws Exception {
        Artwork artwork = artwork(true);
        Artwork saved = artworkService.save(artwork,
                new MockMultipartFile("imageFile", "test.png", "image/png", new byte[]{1, 2, 3}), List.of());
        Path image = Path.of(saved.getImageUrl());
        try {
            mvc.perform(get("/" + saved.getImageUrl())).andExpect(status().isOk())
                    .andExpect(content().bytes(new byte[]{1, 2, 3}));
        } finally {
            Files.deleteIfExists(image);
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void draftsRemainVisibleAndBlogEditsPreserveDates() throws Exception {
        BlogPost draft = new BlogPost();
        draft.setTitle("Regression draft");
        draft.setContent("Draft content");
        draft = blogService.save(draft);
        var createdAt = draft.getCreatedAt();
        mvc.perform(get("/admin/blog")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Regression draft")));
        BlogPost edit = new BlogPost();
        edit.setId(draft.getId());
        edit.setTitle("Edited draft");
        edit.setContent("Updated content");
        edit.setSlug(draft.getSlug());
        BlogPost saved = blogService.save(edit);
        assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void negativeBlogPageDoesNotFail() throws Exception {
        mvc.perform(get("/blog").param("page", "-1")).andExpect(status().isOk());
    }

    @Test
    void defaultAccountsAreNotCreated() {
        assertThat(users.findByUsername("admin")).isEmpty();
        assertThat(users.findByUsername("user")).isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void publishedBlogCoverUsesItsImageUrl() throws Exception {
        BlogPost post = new BlogPost();
        post.setTitle("Regression published post");
        post.setContent("Published content");
        post.setPublished(true);
        post.setCoverImageUrl("uploads/artworks/cover.jpg");
        post = blogService.save(post);
        for (String url : List.of("/blog", "/blog/" + post.getSlug())) {
            mvc.perform(get(url)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("/uploads/artworks/cover.jpg")));
        }
    }
}
