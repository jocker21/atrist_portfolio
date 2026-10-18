package com.example.artist;

import com.example.artist.entity.Artwork;
import com.example.artist.entity.BlogPost;
import com.example.artist.repository.ArtworkRepository;
import com.example.artist.repository.ContactMessageRepository;
import com.example.artist.repository.OrderRepository;
import com.example.artist.service.BlogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class PortfolioAdditionalRegressionTests {
    @Autowired MockMvc mvc;
    @Autowired ArtworkRepository artworks;
    @Autowired OrderRepository orders;
    @Autowired ContactMessageRepository messages;
    @Autowired BlogService blogs;

    @Test void draftsCannotBeViewedOrOrdered() throws Exception {
        Artwork draft = new Artwork();
        draft.setTitle("Private draft"); draft.setYear(2026); draft.setPublished(false);
        draft = artworks.saveAndFlush(draft);
        long count = orders.count();
        mvc.perform(get("/gallery/" + draft.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/order/" + draft.getId())).andExpect(status().isNotFound());
        mvc.perform(post("/order/" + draft.getId()).with(csrf())
                .param("customerName", "Customer").param("customerEmail", "test@example.com"))
                .andExpect(status().isNotFound());
        assertThat(orders.count()).isEqualTo(count);
    }

    @Test void missingPublicPagesReturn404() throws Exception {
        for (String url : new String[]{"/gallery/999999", "/order/999999", "/blog/missing"}) {
            mvc.perform(get(url)).andExpect(status().isNotFound());
        }
    }

    @Test void sameTitlesReceiveDistinctStableSlugs() {
        BlogPost first = createBlogPost("Same title");
        BlogPost second = createBlogPost("Same title");
        assertThat(first.getSlug()).isEqualTo("same-title");
        assertThat(second.getSlug()).isEqualTo("same-title-2");
        assertThat(blogs.save(second).getSlug()).isEqualTo("same-title-2");
    }

    @Test void ukrainianLettersAndSymbolOnlyTitlesProduceUsableSlugs() {
        assertThat(createBlogPost("Її літо").getSlug()).isEqualTo("її-літо");
        assertThat(createBlogPost("!!!").getSlug()).isEqualTo("post");
        assertThat(createBlogPost("???").getSlug()).isEqualTo("post-2");
    }

    @Test void contactFormPersistsMessageAndInvalidInputDoesNot() throws Exception {
        long count = messages.count();
        mvc.perform(post("/contact").with(csrf()).param("name", "Customer")
                .param("email", "test@example.com").param("message", "Question about painting"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Ваше сообщение сохранено")));
        assertThat(messages.count()).isEqualTo(count + 1);
        assertThat(messages.findAll().getLast().getMessage()).isEqualTo("Question about painting");
        mvc.perform(post("/contact").with(csrf()).param("name", "").param("email", "bad")
                .param("message", "")).andExpect(status().isOk());
        assertThat(messages.count()).isEqualTo(count + 1);
    }

    private BlogPost createBlogPost(String title) {
        BlogPost post = new BlogPost(); post.setTitle(title); post.setContent("Content");
        return blogs.save(post);
    }
}
