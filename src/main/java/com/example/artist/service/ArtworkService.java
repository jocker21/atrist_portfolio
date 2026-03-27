package com.example.artist.service;

import com.example.artist.entity.Artwork;
import com.example.artist.repository.ArtworkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

// @Service — помечает класс как сервис (бизнес-логика).
// Spring создаёт один экземпляр этого класса (singleton) и
// внедряет его везде, где он нужен (@Autowired или через конструктор).
@Service
// @RequiredArgsConstructor (Lombok) — генерирует конструктор для всех final-полей.
// Spring видит конструктор с одним параметром и автоматически внедряет ArtworkRepository.
// Это называется Dependency Injection (DI) — внедрение зависимостей.
@RequiredArgsConstructor
public class ArtworkService {

    // final — поле обязательно должно быть задано через конструктор.
    // Spring сам найдёт ArtworkRepository (он тоже управляется Spring).
    private final ArtworkRepository artworkRepository;

    // @Value — Spring читает значение из application.properties
    // и вставляет сюда. "uploads/artworks" — значение по умолчанию.
    @Value("${app.upload.dir:uploads/artworks}")
    private String uploadDir;

    // Получить все опубликованные работы для галереи
    public List<Artwork> getAllPublished() {
        return artworkRepository.findByPublishedTrue();
    }

    // Получить работы по категории
    public List<Artwork> getByCategory(String categorySlug) {
        return artworkRepository.findByCategoriesSlugAndPublishedTrue(categorySlug);
    }

    // Получить работы по году
    public List<Artwork> getByYear(Integer year) {
        return artworkRepository.findByYearAndPublishedTrue(year);
    }

    // Все года, в которые были работы (для фильтра в галерее)
    public List<Integer> getAllYears() {
        return artworkRepository.findAllPublishedYears();
    }

    // Найти работу по id (для страницы детали / редактирования)
    public Artwork getById(Long id) {
        // orElseThrow — если не найдено, бросает исключение.
        // Spring автоматически вернёт 404 страницу.
        return artworkRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Работа не найдена: " + id));
    }

    // Сохранить новую работу с загрузкой изображения
    public Artwork save(Artwork artwork, MultipartFile imageFile) throws IOException {
        // Если файл изображения передан и не пустой — сохраняем его
        if (imageFile != null && !imageFile.isEmpty()) {
            String imagePath = saveImage(imageFile);
            artwork.setImageUrl(imagePath);
        }
        // repository.save() — INSERT если новая запись, UPDATE если уже есть id
        return artworkRepository.save(artwork);
    }

    // Удалить работу
    public void delete(Long id) {
        artworkRepository.deleteById(id);
    }

    // Вспомогательный метод: сохраняет файл на диск и возвращает путь
    private String saveImage(MultipartFile file) throws IOException {
        // Создаём папку если её нет
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Генерируем уникальное имя файла чтобы избежать конфликтов
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String filename = UUID.randomUUID() + extension;

        // Копируем файл в папку uploads
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        // Возвращаем путь для хранения в базе
        return uploadDir + "/" + filename;
    }
}
