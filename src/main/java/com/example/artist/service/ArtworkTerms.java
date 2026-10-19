package com.example.artist.service;

import com.example.artist.entity.ArtworkTerm;
import com.example.artist.repository.ArtworkTermRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service("artworkTerms")
@RequiredArgsConstructor
public class ArtworkTerms {
    private final ArtworkTermRepository repository;
    private final PortfolioText text;
    private final EntityManager entityManager;
    public static final Set<String> KINDS = Set.of("material", "surface", "technique", "genre");
    private static final Map<String, List<String>> DEFAULTS = Map.of(
        "material", List.of("Олія", "Акварель", "Акрил", "Пастель", "Графіт", "Вугілля", "Темпера", "Змішана техніка"),
        "surface", List.of("Полотно", "Папір", "Картон", "Дерево", "ДВП", "Оргаліт"),
        "technique", List.of("Кисть", "Мастихін", "Пальці рук", "Змішана техніка"),
        "genre", List.of("Квіти", "Портрет", "Пейзаж", "Натюрморт", "Абстракція", "Міський пейзаж", "Морський пейзаж", "Іконопис", "Інше"));
    private static final Map<String,String> ENGLISH = Map.of("Кисть","Brush", "Мастихін","Palette knife", "Пальці рук","Finger painting", "Квіти","Flowers", "Іконопис","Icon painting");

    public List<ArtworkTerm> options(String kind) {
        checkKind(kind);
        Map<String, ArtworkTerm> result = new TreeMap<>();
        for (String value : DEFAULTS.get(kind)) {
            ArtworkTerm term = new ArtworkTerm(); term.setKind(kind); term.setValue(value);
            term.setEnglish(ENGLISH.getOrDefault(value, text.term(value, Locale.ENGLISH))); result.put(value, term);
        }
        for (ArtworkTerm term : repository.findByKindOrderByValueAsc(kind)) result.put(term.getValue(), term);
        for (String value : used(kind, false)) if (!result.containsKey(value)) {
            ArtworkTerm term = new ArtworkTerm(); term.setKind(kind); term.setValue(value);
            term.setEnglish(text.term(value, Locale.ENGLISH)); result.put(value, term);
        }
        return new ArrayList<>(result.values());
    }
    public String label(String kind, String value, Locale locale) {
        if (value == null) return "";
        return repository.findByKindOrderByValueAsc(kind).stream().filter(t -> t.getValue().equals(value)).findFirst()
            .map(t -> locale.getLanguage().equals("en") && t.getEnglish() != null && !t.getEnglish().isBlank() ? t.getEnglish() : t.getValue())
            .orElseGet(() -> locale.getLanguage().equals("en") ? ENGLISH.getOrDefault(value,text.term(value,locale)) : value);
    }
    @Transactional
    public void save(String kind, String value, String english) {
        checkKind(kind);
        if (value == null || value.isBlank() || value.length() > 80 || english == null || english.isBlank() || english.length() > 80)
            throw new IllegalArgumentException("settings.invalid");
        var existing = repository.findByKindOrderByValueAsc(kind).stream().filter(t -> t.getValue().equals(value.trim())).findFirst();
        ArtworkTerm term = existing.orElseGet(ArtworkTerm::new);
        term.setKind(kind); term.setValue(value.trim()); term.setEnglish(english.trim());
        repository.saveAndFlush(term);
    }
    public List<String> used(String kind, boolean publishedOnly) {
        checkKind(kind);
        var builder = entityManager.getCriteriaBuilder();
        var query = builder.createQuery(String.class);
        var root = query.from(com.example.artist.entity.Artwork.class);
        query.select(root.get(kind)).distinct(true);
        var condition = builder.and(builder.isNotNull(root.get(kind)), builder.notEqual(root.get(kind), ""));
        if (publishedOnly) condition = builder.and(condition, builder.isTrue(root.get("published")));
        query.where(condition).orderBy(builder.asc(root.get(kind)));
        return entityManager.createQuery(query).getResultList();
    }
    private void checkKind(String kind) { if (!KINDS.contains(kind)) throw new IllegalArgumentException("settings.invalid"); }
}
