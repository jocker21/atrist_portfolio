package com.example.artist.service;
import com.example.artist.controller.GalleryFilter;
import com.example.artist.entity.Artwork;
import com.example.artist.repository.ArtworkRepository;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service @RequiredArgsConstructor
public class GallerySearch {
    private final ArtworkRepository artworks;
    public List<Artwork> search(GalleryFilter filter, Locale locale) {
        return artworks.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("published")));
            String[] fields = {"material","surface","technique","genre"};
            String[] values = {filter.getMaterial(),filter.getSurface(),filter.getTechnique(),filter.getGenre()};
            for (int i=0;i<fields.length;i++) if(values[i]!=null && !values[i].isBlank()) predicates.add(cb.equal(root.get(fields[i]),values[i]));
            if (filter.getYear()!=null) predicates.add(cb.equal(root.get("year"),filter.getYear()));
            if ("available".equals(filter.getAvailability())) predicates.add(cb.isTrue(root.get("available")));
            if ("sold".equals(filter.getAvailability())) predicates.add(cb.isFalse(root.get("available")));
            if (filter.getCategory()!=null && !filter.getCategory().isBlank()) {
                var categoryQuery = query.subquery(Long.class);
                var category = categoryQuery.correlate(root).join("categories");
                categoryQuery.select(cb.literal(1L)).where(cb.equal(category.get("slug"),filter.getCategory()));
                predicates.add(cb.exists(categoryQuery));
            }
            Path<java.math.BigDecimal> price = root.get("USD".equals(filter.getCurrency()) ? "priceUsd" : "price");
            if (filter.getPriceMin()!=null) predicates.add(cb.greaterThanOrEqualTo(price,filter.getPriceMin()));
            if (filter.getPriceMax()!=null) predicates.add(cb.lessThanOrEqualTo(price,filter.getPriceMax()));
            List<Order> orders = new ArrayList<>();
            switch (filter.getSort()) {
                case "priceAsc", "priceDesc" -> {
                    orders.add(cb.asc(cb.selectCase().when(cb.isNull(price),1).otherwise(0)));
                    orders.add("priceAsc".equals(filter.getSort()) ? cb.asc(price) : cb.desc(price));
                }
                case "oldest" -> orders.add(cb.asc(root.get("year")));
                case "title" -> orders.add(cb.asc("en".equals(locale.getLanguage()) ? cb.coalesce(cb.nullif(root.get("titleEn"),""),root.get("title")) : root.get("title")));
                default -> orders.add(cb.desc(root.get("year")));
            }
            orders.add(cb.desc(root.get("id"))); query.orderBy(orders);
            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }
}
