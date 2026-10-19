package com.example.artist.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.example.artist.repository.ArtworkRepository;
import org.jcodec.common.Codec;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class ArtworkMediaStorage {
    private final ArtworkRepository artworks;
    @Value("${app.upload.dir:uploads/artworks}")
    private String uploadDir;

    public String image(MultipartFile file) throws IOException {
        if (file.getSize() > 5L * 1024 * 1024) throw new IllegalArgumentException("media.imageSize");
        if (!"image/jpeg".equals(file.getContentType()) && !"image/png".equals(file.getContentType()))
            throw new IllegalArgumentException("media.imageType");
        try (var input = file.getInputStream(); var stream = ImageIO.createImageInputStream(input)) {
            if (stream == null) throw new IllegalArgumentException("media.imageType");
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IllegalArgumentException("media.imageType");
            var reader = readers.next();
            try {
                reader.setInput(stream);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 30_000_000)
                    throw new IllegalArgumentException("media.imageSize");
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("JPEG") && !format.equalsIgnoreCase("PNG"))
                    throw new IllegalArgumentException("media.imageType");
                var image = reader.read(0);
                Path path = newFile(format.equalsIgnoreCase("JPEG") ? ".jpg" : ".png");
                if (!ImageIO.write(image, format, path.toFile())) throw new IOException("Image encoding failed");
                return url(path);
            } finally { reader.dispose(); }
        }
    }

    public String video(MultipartFile file) throws IOException {
        if (file.getSize() > 100L * 1024 * 1024) throw new IllegalArgumentException("media.videoSize");
        if (!"video/mp4".equals(file.getContentType())) throw new IllegalArgumentException("media.videoType");
        Path path = newFile(".mp4");
        try (var input = file.getInputStream()) { Files.copy(input, path); }
        // Inspect the actual MP4 tracks rather than trusting the supplied filename/MIME type.
        try (var channel = NIOUtils.readableChannel(path.toFile())) {
            var demuxer = MP4Demuxer.createMP4Demuxer(channel);
            var track = demuxer.getVideoTrack();
            if (track == null || track.getMeta().getCodec() != Codec.H264 || track.nextFrame() == null)
                throw new IllegalArgumentException("media.videoType");
            for (var audio : demuxer.getAudioTracks()) {
                if (audio.getMeta().getCodec() != Codec.AAC) throw new IllegalArgumentException("media.videoType");
            }
        } catch (IOException | RuntimeException ex) {
            throw new IllegalArgumentException("media.videoType", ex);
        }
        return url(path);
    }

    private Path newFile(String extension) throws IOException {
        Path directory = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        Path path = directory.resolve(UUID.randomUUID() + extension);
        // Registered before writing, so partial writes and failed commits are cleaned up too.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) remove(path);
            }
        });
        return path;
    }

    private String url(Path path) { return "uploads/artworks/" + path.getFileName(); }

    public void removeAfterCommit(String url) {
        if (url == null || !url.startsWith("uploads/artworks/")) return;
        String filename = url.substring("uploads/artworks/".length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\")) return;
        Path directory = Path.of(uploadDir).toAbsolutePath().normalize();
        Path path = directory.resolve(filename).normalize();
        if (!path.startsWith(directory) || path.equals(directory)) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                if (artworks.countMediaReferences(url) == 0) remove(path);
            }
        });
    }

    private void remove(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException ex) { log.warn("Unable to remove artwork media {}", path, ex); }
    }
}
