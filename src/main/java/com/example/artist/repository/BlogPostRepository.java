package com.example.artist.repository;

import com.example.artist.entity.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    Page<BlogPost> findByPublishedTrueOrderByPublishedAtDesc(Pageable pageable);
    Optional<BlogPost> findBySlugAndPublishedTrue(String slug);
    List<BlogPost> findTop3ByPublishedTrueOrderByPublishedAtDesc();
}
