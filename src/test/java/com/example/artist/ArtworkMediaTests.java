package com.example.artist;

import com.example.artist.entity.Artwork;
import com.example.artist.repository.ArtworkRepository;
import com.example.artist.service.ArtworkService;
import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ArtworkMediaTests {
    @TempDir static Path directory;
    @TempDir Path fixtures;
    @Autowired ArtworkService service;
    @Autowired ArtworkRepository repository;
    @Autowired MockMvc mvc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired com.example.artist.service.ArtistProfileService profiles;
    private final List<Long> ids = new ArrayList<>();

    @DynamicPropertySource static void storage(DynamicPropertyRegistry registry) {
        registry.add("app.upload.dir", () -> directory.toString());
    }
    @AfterEach void cleanup() throws Exception {
        ids.forEach(id -> { if (repository.existsById(id)) service.delete(id); });
        try (var files = Files.list(directory)) { for (Path file : files.toList()) Files.deleteIfExists(file); }
    }
    private Artwork draft() {
        Artwork artwork = new Artwork(); artwork.setTitle("Media test"); artwork.setYear(2026); return artwork;
    }
    private MockMultipartFile photo(String part) throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return new MockMultipartFile(part, "../../photo.png", "image/png", bytes.toByteArray());
    }
    private MockMultipartFile video() throws Exception {
        Path path = fixtures.resolve("sample.mp4");
        var encoder = SequenceEncoder.createSequenceEncoder(path.toFile(), 24);
        for (int frame = 0; frame < 24; frame++) {
            Picture picture = Picture.create(32, 32, ColorSpace.RGB);
            picture.fill(frame * 3); encoder.encodeNativeFrame(picture);
        }
        encoder.finish();
        return new MockMultipartFile("videoFile", "../../clip.mp4", "video/mp4", Files.readAllBytes(path));
    }
    private Artwork save(int count, boolean withVideo) throws Exception {
        List<org.springframework.web.multipart.MultipartFile> photos = new ArrayList<>();
        for (int i = 0; i < count; i++) photos.add(photo("additionalPhotos"));
        Artwork result = service.save(draft(), photo("imageFile"), List.of(), photos, withVideo ? video() : null, List.of(), false);
        ids.add(result.getId()); return result;
    }
    private Path file(String url) { return directory.resolve(Path.of(url).getFileName()); }

    @Test void savesSixPhotosAndPlayableVideoAndSupportsRangeRequests() throws Exception {
        Artwork saved = save(6, true);
        assertThat(saved.getAdditionalImages()).hasSize(6);
        assertThat(file(saved.getVideoUrl())).exists();
        for (String url : saved.getAdditionalImages()) assertThat(file(url)).exists();
        mvc.perform(get("/" + saved.getVideoUrl()).header("Range", "bytes=0-31"))
                .andExpect(status().isPartialContent()).andExpect(header().string("Content-Length", "32"));
        for (String lang : List.of("uk", "en")) mvc.perform(get("/gallery/" + saved.getId()).param("lang", lang))
                .andExpect(status().isOk()).andExpect(content().string(containsString(saved.getVideoUrl())))
                .andExpect(content().string(containsString(saved.getAdditionalImages().getFirst())));
    }
    @Test void editingWithoutUploadsKeepsMediaAndSeventhPhotoIsRejected() throws Exception {
        Artwork original = save(6, true); Artwork edit = draft(); edit.setId(original.getId());
        Artwork saved = service.save(edit, null, List.of());
        assertThat(saved.getAdditionalImages()).isEqualTo(original.getAdditionalImages());
        assertThat(saved.getVideoUrl()).isEqualTo(original.getVideoUrl());
        assertThatThrownBy(() -> service.save(edit, null, List.of(), List.of(photo("additionalPhotos")), null, List.of(), false))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("media.tooMany");
        assertThat(repository.findById(edit.getId()).orElseThrow().getAdditionalImages()).hasSize(6);
    }
    @Test void replacementDeletesOnlySelectedMediaAfterCommit() throws Exception {
        Artwork original = save(6, true); Artwork edit = draft(); edit.setId(original.getId());
        String removed = original.getAdditionalImages().getFirst();
        Artwork saved = service.save(edit, null, List.of(), List.of(photo("additionalPhotos")), video(), List.of(removed), false);
        assertThat(saved.getAdditionalImages()).hasSize(6).doesNotContain(removed);
        assertThat(file(removed)).doesNotExist(); assertThat(file(original.getVideoUrl())).doesNotExist();
        assertThat(file(original.getAdditionalImages().getLast())).exists();
        assertThat(file(saved.getVideoUrl())).exists();
    }
    @Test void rollbackPreservesOldFilesAndCleansNewUploads() throws Exception {
        Artwork original = save(1, true); long before;
        try (var files = Files.list(directory)) { before = files.count(); }
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            try {
                Artwork edit = draft(); edit.setId(original.getId());
                service.save(edit, null, List.of(), List.of(photo("additionalPhotos")), null, original.getAdditionalImages(), true);
                status.setRollbackOnly();
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
        assertThat(file(original.getAdditionalImages().getFirst())).exists();
        assertThat(file(original.getVideoUrl())).exists();
        try (var files = Files.list(directory)) { assertThat(files.count()).isEqualTo(before); }
        assertThat(repository.findById(original.getId()).orElseThrow().getVideoUrl()).isEqualTo(original.getVideoUrl());
    }
    @Test void invalidMediaLeavesNeitherDatabaseRecordNorOrphanFiles() throws Exception {
        var fake = new MockMultipartFile("videoFile", "fake.mp4", "video/mp4", "not a video".getBytes());
        assertThatThrownBy(() -> service.save(draft(), photo("imageFile"), List.of(), List.of(photo("additionalPhotos")), fake, List.of(), false))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("media.videoType");
        try (var files = Files.list(directory)) { assertThat(files.count()).isZero(); }
        var fakePhoto = new MockMultipartFile("additionalPhotos", "fake.png", "image/png", "not a photo".getBytes());
        assertThatThrownBy(() -> service.save(draft(), null, List.of(), List.of(fakePhoto), null, List.of(), false))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("media.imageType");
    }
    @Test void deletionRemovesAllMedia() throws Exception {
        Artwork saved = save(2, true); service.delete(saved.getId());
        assertThat(file(saved.getImageUrl())).doesNotExist();
        for (String url : saved.getAdditionalImages()) assertThat(file(url)).doesNotExist();
        assertThat(file(saved.getVideoUrl())).doesNotExist();
    }
    @Test void oversizedFilesAreRejected() throws Exception {
        var huge = new MockMultipartFile("additionalPhotos", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1]);
        assertThatThrownBy(() -> service.save(draft(), null, List.of(), List.of(huge), null, List.of(), false))
                .hasMessage("media.imageSize");
        var hugeVideo = new MockMultipartFile("videoFile", "large.mp4", "video/mp4", new byte[]{1}) {
            @Override public long getSize() { return 100L * 1024 * 1024 + 1; }
        };
        assertThatThrownBy(() -> service.save(draft(), null, List.of(), List.of(), hugeVideo, List.of(), false))
                .hasMessage("media.videoSize");
    }
    @Test @WithMockUser(roles = "ADMIN")
    void adminFormStoresDollarPriceAndConvertsInchesWithoutBindingMediaPaths() throws Exception {
        mvc.perform(multipart("/admin/artworks/new").file(photo("additionalPhotos")).with(csrf())
                        .param("title", "Inch artwork").param("year", "2026").param("price", "4000").param("priceUsd", "100")
                        .param("widthCm", "10").param("heightCm", "20").param("inputUnit", "in")
                        .param("videoUrl", "uploads/artworks/injected.mp4").param("additionalImages[0]", "evil.jpg"))
                .andExpect(redirectedUrl("/admin/artworks"));
        Artwork saved = repository.findAll().stream().filter(a -> a.getTitle().equals("Inch artwork")).findFirst().orElseThrow();
        ids.add(saved.getId()); assertThat(saved.getWidthCm()).isEqualByComparingTo("25.40");
        assertThat(saved.getHeightCm()).isEqualByComparingTo("50.80");
        assertThat(saved.getPriceUsd()).isEqualByComparingTo("100");
        assertThat(saved.getWidthInches()).isEqualByComparingTo("10");
        assertThat(saved.getVideoUrl()).isNull(); assertThat(saved.getAdditionalImages()).hasSize(1);
        mvc.perform(get("/admin/artworks/" + saved.getId() + "/edit"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("additionalPhotos")))
                .andExpect(content().string(containsString("priceUsd")));
    }
    @Test @WithMockUser(roles = "ADMIN")
    void failedEditRetainsEditRouteAndStoredMedia() throws Exception {
        Artwork saved = save(1, true);
        mvc.perform(multipart("/admin/artworks/" + saved.getId() + "/edit").with(csrf())
                        .param("title", "").param("year", "2026"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("/admin/artworks/" + saved.getId() + "/edit")))
                .andExpect(content().string(containsString(saved.getVideoUrl())));
    }
    @Test void uploadsRequireAdminAndCsrf() throws Exception {
        mvc.perform(multipart("/admin/artworks/new").file(photo("additionalPhotos")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(multipart("/admin/artworks/new").with(user("reader").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/admin/artworks/new").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }
    @Test void forgedRemovalCannotDeleteAnotherArtworksFiles() throws Exception {
        Artwork a = save(1, true); Artwork b = save(1, false); Artwork edit = draft(); edit.setId(a.getId());
        service.save(edit, null, List.of(), List.of(), null, b.getAdditionalImages(), false);
        assertThat(file(b.getAdditionalImages().getFirst())).exists();
        assertThat(repository.findById(a.getId()).orElseThrow().getAdditionalImages()).isEqualTo(a.getAdditionalImages());
    }
    @Test void sharedLegacyFilesAreNotDeletedWhileAnotherArtworkUsesThem() throws Exception {
        Artwork a = save(1, false); Artwork b = draft();
        String shared = a.getAdditionalImages().getFirst();
        b.setImageUrl(shared); b = repository.saveAndFlush(b); ids.add(b.getId());
        Artwork edit = draft(); edit.setId(a.getId());
        service.save(edit, null, List.of(), List.of(), null, List.of(shared), false);
        assertThat(file(shared)).exists();
        service.delete(b.getId()); assertThat(file(shared)).doesNotExist();
    }
    @Test void oversizedUploadErrorHasSharedPageDataEvenBeforeControllerBinding() {
        var model = new org.springframework.ui.ExtendedModelMap();
        var advice = new com.example.artist.controller.UploadErrorAdvice(profiles);
        assertThat(advice.oversizedUpload(model)).isEqualTo("upload-error");
        assertThat(model).containsKeys("artistProfile", "languageUkUrl", "languageEnUrl");
        assertThat(model.get("artistProfile")).isNotNull();
    }
}
