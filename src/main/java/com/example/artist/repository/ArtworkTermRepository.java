package com.example.artist.repository;
import com.example.artist.entity.ArtworkTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ArtworkTermRepository extends JpaRepository<ArtworkTerm, Long> {
    List<ArtworkTerm> findByKindOrderByValueAsc(String kind);
    boolean existsByKindAndValue(String kind, String value);
}
