package com.example.artist;

import com.example.artist.entity.User;
import com.example.artist.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginNavigationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void loginIsVisibleAndAdministratorCanEnterAndLeavePanel() throws Exception {
        for (String url : List.of("/", "/gallery", "/about", "/blog", "/contact", "/order/success")) {
            mvc.perform(get(url)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("href=\"/login\"")))
                    .andExpect(content().string(not(containsString("href=\"/admin/artworks\""))));
        }
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")));

        User admin = new User();
        admin.setUsername("navigation_regression_admin");
        admin.setPassword(passwordEncoder.encode("regression-test-password"));
        admin.setRole("ROLE_ADMIN");
        admin.setEnabled(true);
        users.saveAndFlush(admin);
        var login = mvc.perform(post("/login").with(csrf())
                        .param("username", admin.getUsername())
                        .param("password", "regression-test-password"))
                .andExpect(status().is3xxRedirection()).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/admin/artworks\"")))
                .andExpect(content().string(not(containsString("href=\"/login\""))));
        for (String url : List.of("/admin/artworks", "/admin/blog")) {
            mvc.perform(get(url).session(session)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("action=\"/logout\"")))
                    .andExpect(content().string(not(containsString("href=\"/logout\""))));
        }
        mvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/artworks").session(session)).andExpect(status().isOk());
        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/"));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/login\"")));
        mvc.perform(get("/admin/artworks"))
                .andExpect(redirectedUrl("http://localhost/login"));
    }
}
