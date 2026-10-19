package com.example.artist.controller;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Getter @Setter
public class GalleryFilter {
    private String material, surface, technique, genre, category;
    private Integer year;
    private String availability = "all";
    private String sort = "newest";
    private String currency = "UAH";
    private BigDecimal priceMin, priceMax;
    public String selection(String kind) {
        return switch(kind) { case "material" -> material; case "surface" -> surface; case "technique" -> technique; case "genre" -> genre; default -> null; };
    }
    public boolean valid() {
        return (priceMin == null || (priceMin.signum() >= 0 && priceMin.compareTo(new BigDecimal("99999999.99")) <= 0))
            && (priceMax == null || (priceMax.signum() >= 0 && priceMax.compareTo(new BigDecimal("99999999.99")) <= 0))
            && (priceMin == null || priceMax == null || priceMin.compareTo(priceMax) <= 0);
    }
}
