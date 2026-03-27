package com.example.artist.repository;

import com.example.artist.entity.Artwork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArtworkRepository extends JpaRepository<Artwork, Long> {

    List<Artwork> findByPublishedTrue();

    List<Artwork> findByCategoriesSlugAndPublishedTrue(String slug);

    List<Artwork> findByYearAndPublishedTrue(Integer year);

    @Query("SELECT DISTINCT a.year FROM Artwork a WHERE a.published = true ORDER BY a.year DESC")
    List<Integer> findAllPublishedYears();

    @Query("SELECT a FROM Artwork a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Artwork> searchByTitle(@Param("query") String query);
}
