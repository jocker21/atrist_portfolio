package com.example.artist.service;

import com.example.artist.entity.Artwork;
import com.example.artist.entity.BlogPost;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component("portfolioText")
public class PortfolioText {
    private String choose(String original, String english, Locale locale) {
        return "en".equals(locale.getLanguage()) && english != null && !english.isBlank() ? english : original;
    }
    public String title(Artwork item, Locale locale) {
        return choose(item.getTitle(), item.getTitleEn(), locale);
    }

    public String description(Artwork item, Locale locale) {
        return choose(item.getDescription(), item.getDescriptionEn(), locale);
    }

    public String title(BlogPost item, Locale locale) {
        return choose(item.getTitle(), item.getTitleEn(), locale);
    }

    public String excerpt(BlogPost item, Locale locale) {
        return choose(item.getExcerpt(), item.getExcerptEn(), locale);
    }

    public String content(BlogPost item, Locale locale) {
        return choose(item.getContent(), item.getContentEn(), locale);
    }

    private static final Map<String,String> TERMS = Map.ofEntries(
        Map.entry("Олія","Oil"), Map.entry("Акварель","Watercolour"), Map.entry("Акрил","Acrylic"),
        Map.entry("Пастель","Pastel"), Map.entry("Графіт","Graphite"), Map.entry("Вугілля","Charcoal"),
        Map.entry("Темпера","Tempera"), Map.entry("Змішана техніка","Mixed media"),
        Map.entry("Полотно","Canvas"), Map.entry("Папір","Paper"), Map.entry("Картон","Cardboard"),
        Map.entry("Дерево","Wood"), Map.entry("ДВП","Fibreboard"), Map.entry("Оргаліт","Hardboard"),
        Map.entry("Портрет","Portrait"), Map.entry("Пейзаж","Landscape"), Map.entry("Натюрморт","Still life"),
        Map.entry("Абстракція","Abstract"), Map.entry("Анімалістика","Animal art"),
        Map.entry("Міський пейзаж","Cityscape"), Map.entry("Морський пейзаж","Seascape"), Map.entry("Інше","Other")
    );
    public String term(String value, Locale locale) {
        if (value == null) {
            return "";
        }
        return "en".equals(locale.getLanguage()) ? TERMS.getOrDefault(value, value) : value;
    }
}
