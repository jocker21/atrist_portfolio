package com.example.artist;

import com.example.artist.entity.ArtistProfile;
import com.example.artist.repository.ArtistProfileRepository;
import com.example.artist.service.ArtistProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArtistProfileTests {
    @TempDir static Path portraits;
    @DynamicPropertySource
    static void portraitPath(DynamicPropertyRegistry registry) {
        registry.add("app.artist.upload.dir", () -> portraits.toString());
    }
    @Autowired MockMvc mvc;
    @Autowired ArtistProfileRepository profiles;
    @Autowired ArtistProfileService service;

    @Test void biographyHasProvidedFactsInBothLanguagesWithoutCreatingDatabaseRecord() throws Exception {
        mvc.perform(get("/about")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Тетяна Славінська")))
                .andExpect(content().string(containsString("Головне місце в її творчості посідають квіти")))
                .andExpect(content().string(containsString("Майже всі роботи виконані з натури")))
                .andExpect(content().string(containsString("міські пейзажі")))
                .andExpect(content().string(containsString("сусальним золотом")))
                .andExpect(content().string(containsString("мастихіном")))
                .andExpect(content().string(containsString("ботанічна ілюстрація")));
        mvc.perform(get("/about").param("lang","en")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Tetiana Slavinska")))
                .andExpect(content().string(containsString("Flowers are at the heart")))
                .andExpect(content().string(containsString("Almost all of her works are painted from life")))
                .andExpect(content().string(containsString("gold-leaf gilding")))
                .andExpect(content().string(containsString("palette knife")))
                .andExpect(content().string(containsString("photorealism")))
                .andExpect(content().string(containsString("graphic art")));
        assertThat(profiles.count()).isZero();
    }

    @Test void editingRequiresAdminAndCsrf() throws Exception {
        mvc.perform(get("/admin/profile")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/profile").with(user("reader").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/profile").with(user("admin").roles("ADMIN")).param("lang","en"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("English biography")));
        mvc.perform(post("/admin/profile").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/profile").with(user("reader").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(profiles.count()).isZero();
    }

    @Test void saveIsSingletonIgnoresProtectedFieldsAndEscapesBiography() throws Exception {
        mvc.perform(multipart("/admin/profile").with(user("admin").roles("ADMIN")).with(csrf())
                .param("nameUk","Тетяна Славінська").param("nameEn","Tetiana Slavinska")
                .param("biographyUk","<script>alert(1)</script>").param("biographyEn","English biography")
                .param("id","999").param("portraitUrl","https://untrusted.example/portrait.svg"))
                .andExpect(redirectedUrl("/admin/profile"));
        assertThat(profiles.count()).isEqualTo(1);
        ArtistProfile saved = profiles.findById(1L).orElseThrow();
        assertThat(saved.getPortraitUrl()).isNull();
        assertThat(profiles.findById(999L)).isEmpty();
        mvc.perform(get("/about")).andExpect(content().string(containsString("&lt;script&gt;")));
        mvc.perform(get("/about").param("lang","en"))
                .andExpect(content().string(containsString("English biography")));
    }

    @Test void invalidNameDoesNotSave() throws Exception {
        mvc.perform(multipart("/admin/profile").with(user("admin").roles("ADMIN")).with(csrf())
                .param("nameUk","").param("nameEn","Name"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("profileForm","nameUk"));
        assertThat(profiles.count()).isZero();
    }

    @Test void imageMustBeAnActualPngOrJpeg() throws Exception {
        mvc.perform(multipart("/admin/profile")
                .file(new MockMultipartFile("portrait","fake.png","image/png","not an image".getBytes()))
                .with(user("admin").roles("ADMIN")).with(csrf())
                .param("nameUk","Тетяна").param("nameEn","Tetiana"))
                .andExpect(status().isOk()).andExpect(model().attribute("portraitError","profile.imageType"));
        assertThat(profiles.count()).isZero();
        try (var files = Files.list(portraits)) {
            assertThat(files.count()).isZero();
        }
    }

    @Test void portraitUploadIsServedAndKeptWhenOnlyBiographyChanges() throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",bytes);
        mvc.perform(multipart("/admin/profile")
                .file(new MockMultipartFile("portrait","../../portrait.png","image/png",bytes.toByteArray()))
                .with(user("admin").roles("ADMIN")).with(csrf())
                .param("nameUk","Тетяна").param("nameEn","Tetiana"))
                .andExpect(redirectedUrl("/admin/profile"));
        String imageUrl = profiles.findById(1L).orElseThrow().getPortraitUrl();
        assertThat(imageUrl).startsWith("uploads/artist/").endsWith(".png").doesNotContain("..");
        mvc.perform(get("/" + imageUrl)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(multipart("/admin/profile").with(user("admin").roles("ADMIN")).with(csrf())
                .param("nameUk","Тетяна").param("nameEn","Tetiana")
                .param("biographyUk","Оновлений текст"))
                .andExpect(redirectedUrl("/admin/profile"));
        assertThat(profiles.findById(1L).orElseThrow().getPortraitUrl()).isEqualTo(imageUrl);
        mvc.perform(get("/about").param("lang","en"))
                .andExpect(content().string(containsString("Оновлений текст")));
    }
}
