package com.example.artist.service;

import com.example.artist.entity.Category;
import com.example.artist.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getAll() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    public Category save(Category category) {
        // Автогенерация slug из названия если не задан
        if (category.getSlug() == null || category.getSlug().isBlank()) {
            category.setSlug(category.getName()
                    .toLowerCase()
                    .replaceAll("\\s+", "-")
                    .replaceAll("[^a-z0-9-]", ""));
        }
        return categoryRepository.save(category);
    }

    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }
}
