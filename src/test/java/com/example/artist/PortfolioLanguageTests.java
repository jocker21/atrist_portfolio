package com.example.artist;

import com.example.artist.entity.Artwork;
import com.example.artist.entity.BlogPost;
import com.example.artist.repository.ArtworkRepository;
import com.example.artist.service.BlogService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PortfolioLanguageTests {
    @Autowired MockMvc mvc;
    @Autowired ArtworkRepository artworks;
    @Autowired BlogService blogs;

    @Test void defaultIsUkrainianAndChoicePersistsAcrossPages() throws Exception {
        mvc.perform(get("/")).andExpect(content().string(containsString("lang=\"uk\"")))
                .andExpect(content().string(containsString("Переглянути колекцію")));
        var result = mvc.perform(get("/").param("lang","en")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Explore collection"))).andReturn();
        Cookie choice = result.getResponse().getCookie("portfolio-language");
        assertThat(choice).isNotNull();
        assertThat(choice.getMaxAge()).isGreaterThan(86400);
        for (String page : List.of("/gallery","/about","/blog","/contact","/login","/order/success")) {
            mvc.perform(get(page).cookie(choice)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("lang=\"en\"")))
                    .andExpect(content().string(not(containsString("??"))));
        }
        mvc.perform(get("/gallery").cookie(choice).param("lang","uk"))
                .andExpect(content().string(containsString("lang=\"uk\"")));
    }

    @Test void switchingPreservesFiltersAndPaginationAndIgnoresUnsupportedLanguages() throws Exception {
        mvc.perform(get("/gallery").queryParam("year","2026").queryParam("lang","en"))
                .andExpect(content().string(containsString("/gallery?year=2026&amp;lang=uk")));
        mvc.perform(get("/blog").queryParam("page","2"))
                .andExpect(content().string(containsString("/blog?page=2&amp;lang=en")));
        mvc.perform(get("/").param("lang","invalid")).andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"uk\"")));
    }

    @Test void artworkAndBlogUseEnglishFieldsAndFallBackWithoutLosingOriginals() throws Exception {
        Artwork artwork = new Artwork();
        artwork.setTitle("Квіти"); artwork.setTitleEn("Flowers");
        artwork.setDescription("Історія квітів"); artwork.setDescriptionEn("A story of flowers");
        artwork.setMaterial("Олія"); artwork.setYear(2026);
        artwork = artworks.saveAndFlush(artwork);
        mvc.perform(get("/gallery/" + artwork.getId()).param("lang","en"))
                .andExpect(content().string(containsString("Flowers")))
                .andExpect(content().string(containsString("A story of flowers")))
                .andExpect(content().string(containsString("Oil")));
        mvc.perform(get("/gallery/" + artwork.getId()).param("lang","uk"))
                .andExpect(content().string(containsString("Квіти")));
        artwork.setTitleEn(" ");
        artworks.saveAndFlush(artwork);
        mvc.perform(get("/gallery/" + artwork.getId()).param("lang","en"))
                .andExpect(content().string(containsString("Квіти")));
        BlogPost post = new BlogPost();
        post.setTitle("Майстерня"); post.setTitleEn("Studio notes");
        post.setContent("Нотатки"); post.setContentEn("<p>A new painting</p>");
        post.setPublished(true);
        post = blogs.save(post);
        mvc.perform(get("/blog/" + post.getSlug()).param("lang","en"))
                .andExpect(content().string(containsString("Studio notes")))
                .andExpect(content().string(containsString("<p>A new painting</p>")));
    }

    @Test void adminPagesAndValidationAreTranslated() throws Exception {
        for (String page : List.of("/admin/artworks","/admin/artworks/new","/admin/blog",
                "/admin/blog/new","/admin/orders")) {
            mvc.perform(get(page).with(user("admin").roles("ADMIN")).param("lang","en"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("lang=\"en\"")))
                    .andExpect(content().string(not(containsString("??"))));
        }
        mvc.perform(post("/contact").with(csrf()).param("lang","en")
                .param("name","").param("email","bad").param("message",""))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("contactForm","name","email","message"))
                .andExpect(content().string(containsString("Enter your name")))
                .andExpect(content().string(containsString("Enter a valid email address")));
    }

    @Test void englishArtworkFieldsCanBeSavedThroughAdminForm() throws Exception {
        mvc.perform(multipart("/admin/artworks/new").file("imageFile",new byte[0])
                .with(user("admin").roles("ADMIN")).with(csrf())
                .param("title","Нова робота").param("year","2026").param("titleEn","New work")
                .param("descriptionEn","English description").param("published","true"))
                .andExpect(redirectedUrl("/admin/artworks"));
        Artwork saved = artworks.findAll().stream().filter(a -> "New work".equals(a.getTitleEn())).findFirst().orElseThrow();
        mvc.perform(get("/order/" + saved.getId()).param("lang","en"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("New work")));
    }
}
