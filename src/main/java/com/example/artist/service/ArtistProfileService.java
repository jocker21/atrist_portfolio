package com.example.artist.service;

import com.example.artist.entity.ArtistProfile;
import com.example.artist.repository.ArtistProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtistProfileService {
    private final ArtistProfileRepository repository;

    @Value("${app.artist.upload.dir:uploads/artist}")
    private String uploadDir;

    public ArtistProfile get() {
        // Reading the public site must not create or overwrite profile data.
        return repository.findById(1L).orElseGet(this::draft);
    }

    public ArtistProfile draft() {
        ArtistProfile profile = new ArtistProfile();
        profile.setNameUk("Тетяна Славінська");
        profile.setNameEn("Tetiana Slavinska");
        profile.setIntroUk("Олійний живопис, акварель, акрил, графіка та іконопис у візантійському стилі.");
        profile.setIntroEn("Oil painting, watercolour, acrylics, graphic art and Byzantine-style icon painting.");
        profile.setBiographyUk("""
                Тетяна Славінська — художниця, яка працює з олійними фарбами, аквареллю й акрилом, а також займається графікою та іконописом у візантійському стилі.

                Головне місце в її творчості посідають квіти. Майже всі роботи виконані з натури: живе спостереження допомагає передати характер рослин, тонкі відтінки кольору та мінливість світла.

                Серед інших тем — міські пейзажі, пейзажні етюди, вода та яхти. Окремий напрям становить абстрактний живопис акрилом.

                В іконописі використовує натуральні пігменти та золочення сусальним золотом. У живописі працює кистю, мастихіном і пальцями рук, поєднуючи різні способи нанесення фарби.

                Серед напрямів її творчості — імпресіонізм, абстракція, фотореалізм і ботанічна ілюстрація. Різноманіття матеріалів і технік дозволяє звертатися як до виразної фактури, так і до тонкої деталізації образу.
                """.strip());
        profile.setBiographyEn("""
                Tetiana Slavinska is an artist working with oils, watercolours and acrylics, as well as graphic art and Byzantine-style icon painting.

                Flowers are at the heart of her practice. Almost all of her works are painted from life: direct observation helps convey the character of plants, subtle shifts of colour and changing light.

                Other subjects include cityscapes, landscape studies, water and yachts. Abstract painting in acrylic is another distinct strand of her work.

                Her icon painting uses natural pigments and gold-leaf gilding. In her painting practice, she works with brushes, a palette knife and her fingers, combining different ways of applying paint.

                Her artistic approaches include Impressionism, abstraction, photorealism and botanical illustration. This range of materials and techniques allows her to explore both expressive textures and finely detailed imagery.
                """.strip());
        return profile;
    }

    @Transactional(rollbackFor = IOException.class)
    public void save(ArtistProfile submitted, MultipartFile portrait) throws IOException {
        ArtistProfile profile = get();
        profile.setId(1L);
        profile.setNameUk(submitted.getNameUk().trim());
        profile.setNameEn(submitted.getNameEn().trim());
        profile.setIntroUk(submitted.getIntroUk());
        profile.setIntroEn(submitted.getIntroEn());
        profile.setBiographyUk(submitted.getBiographyUk());
        profile.setBiographyEn(submitted.getBiographyEn());
        if (portrait != null && !portrait.isEmpty()) {
            Path newImage = savePortrait(portrait);
            profile.setPortraitUrl("uploads/artist/" + newImage.getFileName());
            // A failed database save must not leave a new orphaned portrait.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        try {
                            Files.deleteIfExists(newImage);
                        } catch (IOException ex) {
                            log.warn("Unable to remove rolled-back artist portrait {}", newImage, ex);
                        }
                    }
                }
            });
        }
        repository.saveAndFlush(profile);
    }

    private Path savePortrait(MultipartFile file) throws IOException {
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("profile.imageSize");
        }
        if (!"image/png".equals(file.getContentType()) && !"image/jpeg".equals(file.getContentType())) {
            throw new IllegalArgumentException("profile.imageType");
        }
        try (var input = file.getInputStream(); var imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) {
                throw new IllegalArgumentException("profile.imageType");
            }
            var readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("profile.imageType");
            }
            var reader = readers.next();
            try {
                reader.setInput(imageInput);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 30_000_000) {
                    throw new IllegalArgumentException("profile.imageSize");
                }
                var image = reader.read(0);
                Path directory = Path.of(uploadDir).toAbsolutePath().normalize();
                Files.createDirectories(directory);
                Path target = directory.resolve(UUID.randomUUID() + ".png");
                try {
                    ImageIO.write(image, "png", target.toFile());
                } catch (IOException ex) {
                    Files.deleteIfExists(target);
                    throw ex;
                }
                return target;
            } finally {
                reader.dispose();
            }
        }
    }

    public String name(ArtistProfile profile, Locale locale) {
        return choose(profile.getNameUk(), profile.getNameEn(), locale);
    }

    public String intro(ArtistProfile profile, Locale locale) {
        return choose(profile.getIntroUk(), profile.getIntroEn(), locale);
    }

    public String biography(ArtistProfile profile, Locale locale) {
        return choose(profile.getBiographyUk(), profile.getBiographyEn(), locale);
    }

    private String choose(String ukrainian, String english, Locale locale) {
        return "en".equals(locale.getLanguage()) && english != null && !english.isBlank() ? english : ukrainian;
    }
}
