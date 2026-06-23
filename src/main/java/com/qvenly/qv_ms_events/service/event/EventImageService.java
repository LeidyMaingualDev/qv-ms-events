package com.qvenly.qv_ms_events.service.event;

import com.qvenly.qv_ms_events.exception.BusinessException;
import com.qvenly.qv_ms_events.model.dto.response.event.EventImageResponse;
import com.qvenly.qv_ms_events.model.entity.event.EventImage;
import com.qvenly.qv_ms_events.repository.event.EventImageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventImageService {

    private static final int MAX_IMAGES_PER_EVENT = 10;

    private final EventImageRepository imageRepository;
    private final SupabaseStorageService storageService;
    private final EventService eventService;

    @Transactional
    public EventImageResponse uploadImage(Long eventId, MultipartFile file,
                                          String performerEmail, String systemRole) {
        eventService.assertIsOrganizer(eventId, performerEmail, systemRole);

        List<EventImage> existing = imageRepository.findByEventId(eventId);
        if (existing.size() >= MAX_IMAGES_PER_EVENT) {
            throw new BusinessException(
                    "Este evento ya tiene el máximo de " + MAX_IMAGES_PER_EVENT + " imágenes permitidas.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        validateImageFile(file);

        String imageUrl = storageService.uploadFile(file, "events/" + eventId);

        EventImage image = new EventImage();
        image.setEventId(eventId);
        image.setImageUrl(imageUrl);
        image.setIsCover(existing.isEmpty()); // la primera imagen subida es portada automáticamente
        EventImage saved = imageRepository.save(image);

        return toResponse(saved);
    }

    @Transactional
    public void deleteImage(Long eventId, Long imageId, String performerEmail, String systemRole) {
        eventService.assertIsOrganizer(eventId, performerEmail, systemRole);

        EventImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException("Imagen no encontrada.", HttpStatus.NOT_FOUND));

        if (!image.getEventId().equals(eventId)) {
            throw new BusinessException("La imagen no pertenece a este evento.", HttpStatus.BAD_REQUEST);
        }

        boolean wasCover = image.getIsCover();
        storageService.deleteFile(image.getImageUrl());
        imageRepository.delete(image);

        // Si se eliminó la portada, asigna la primera imagen restante como nueva portada
        if (wasCover) {
            List<EventImage> remaining = imageRepository.findByEventId(eventId);
            if (!remaining.isEmpty()) {
                EventImage newCover = remaining.get(0);
                newCover.setIsCover(true);
                imageRepository.save(newCover);
            }
        }
    }

    @Transactional
    public EventImageResponse setCoverImage(Long eventId, Long imageId,
                                            String performerEmail, String systemRole) {
        eventService.assertIsOrganizer(eventId, performerEmail, systemRole);

        EventImage target = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException("Imagen no encontrada.", HttpStatus.NOT_FOUND));

        if (!target.getEventId().equals(eventId)) {
            throw new BusinessException("La imagen no pertenece a este evento.", HttpStatus.BAD_REQUEST);
        }

        // Quita la portada actual
        List<EventImage> images = imageRepository.findByEventId(eventId);
        images.forEach(img -> {
            if (img.getIsCover()) {
                img.setIsCover(false);
                imageRepository.save(img);
            }
        });

        target.setIsCover(true);
        EventImage updated = imageRepository.save(target);
        return toResponse(updated);
    }

    public List<EventImageResponse> getImagesByEvent(Long eventId) {
        return imageRepository.findByEventId(eventId).stream().map(this::toResponse).toList();
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("El archivo está vacío.", HttpStatus.BAD_REQUEST);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("El archivo debe ser una imagen (JPG, PNG, WEBP).", HttpStatus.BAD_REQUEST);
        }
        long maxSizeBytes = 5 * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new BusinessException("La imagen no debe superar los 5MB.", HttpStatus.BAD_REQUEST);
        }
    }

    private EventImageResponse toResponse(EventImage img) {
        EventImageResponse r = new EventImageResponse();
        r.setId(img.getId());
        r.setEventId(img.getEventId());
        r.setImageUrl(img.getImageUrl());
        r.setIsCover(img.getIsCover());
        r.setUploadedAt(img.getUploadedAt());
        return r;
    }
}