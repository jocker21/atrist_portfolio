package com.example.artist.service;

import com.example.artist.entity.BlogPost;
import com.example.artist.repository.BlogPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogPostRepository blogPostRepository;

    // Получить страницу статей для блога (с пагинацией).
    // pageNumber — номер страницы (начиная с 0)
    // pageSize   — сколько статей на одной странице
    public Page<BlogPost> getPublishedPage(int pageNumber, int pageSize) {
        // PageRequest.of создаёт объект пагинации.
        // Репозиторий вернёт только нужный "срез" данных,
        // не загружая все записи из БД сразу.
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return blogPostRepository.findByPublishedTrueOrderByPublishedAtDesc(pageable);
    }

    // Найти статью по slug для страницы /blog/moya-vystavka
    public BlogPost getBySlug(String slug) {
        return blogPostRepository.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Статья не найдена"));
    }

    // Последние 3 статьи для виджета на главной странице
    public List<BlogPost> getLatestPosts() {
        return blogPostRepository.findTop3ByPublishedTrueOrderByPublishedAtDesc();
    }

    // Сохранить статью (новую или обновлённую)
    public BlogPost save(BlogPost post) {
        if (post.getId() != null) {
            BlogPost existing = getById(post.getId());
            post.setCreatedAt(existing.getCreatedAt());
            post.setPublishedAt(existing.getPublishedAt());
            post.setCoverImageUrl(existing.getCoverImageUrl());
        }
        // Автогенерация slug из заголовка если не задан
        if (post.getSlug() == null || post.getSlug().isBlank()) {
            post.setSlug(generateSlug(post.getTitle()));
        }
        String baseSlug = post.getSlug();
        String candidate = baseSlug;
        int suffix = 2;
        while (slugTakenByAnotherPost(candidate, post.getId())) {
            candidate = baseSlug + "-" + suffix++;
        }
        post.setSlug(candidate);
        return blogPostRepository.save(post);
    }

    private boolean slugTakenByAnotherPost(String slug, Long id) {
        return blogPostRepository.findBySlug(slug)
                .filter(existing -> !existing.getId().equals(id)).isPresent();
    }

    public BlogPost getById(Long id) {
        return blogPostRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Статья не найдена"));
    }

    public List<BlogPost> getAll() {
        return blogPostRepository.findAll();
    }

    public void delete(Long id) {
        blogPostRepository.deleteById(id);
    }

    // Преобразует заголовок в slug: "Моя выставка 2024" → "moya-vystavka-2024"
    // Упрощённая версия — для продакшена используй библиотеку slugify
    private String generateSlug(String title) {
        String slug = title.toLowerCase(Locale.ROOT).trim()
                .replaceAll("[^\\p{L}\\p{N}\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return slug.isBlank() ? "post" : slug;
    }
}
