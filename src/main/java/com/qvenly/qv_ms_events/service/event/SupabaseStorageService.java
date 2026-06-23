package com.qvenly.qv_ms_events.service.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
public class SupabaseStorageService {

    private final WebClient webClient;
    private final String supabaseUrl;
    private final String bucketName;

    public SupabaseStorageService(
            WebClient.Builder builder,
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.service-key}") String serviceKey,
            @Value("${supabase.bucket}") String bucketName) {
        this.supabaseUrl = supabaseUrl;
        this.bucketName = bucketName;
        this.webClient = builder
                .baseUrl(supabaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceKey)
                .defaultHeader("apikey", serviceKey)
                .build();
    }

    /**
     * Sube un archivo al bucket de Supabase y retorna la URL pública.
     * @param file archivo a subir
     * @param folder carpeta dentro del bucket (ej: "events/12")
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            String extension = getExtension(file.getOriginalFilename());
            String fileName = folder + "/" + UUID.randomUUID() + extension;

            byte[] fileBytes = file.getBytes();

            webClient.post()
                    .uri("/storage/v1/object/" + bucketName + "/" + fileName)
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .bodyValue(fileBytes)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + fileName;

        } catch (IOException e) {
            log.error("Error al leer el archivo: {}", e.getMessage());
            throw new RuntimeException("No se pudo procesar el archivo de imagen.");
        } catch (Exception e) {
            log.error("Error al subir archivo a Supabase: {}", e.getMessage());
            throw new RuntimeException("Error al subir la imagen a almacenamiento.");
        }
    }

    public void deleteFile(String imageUrl) {
        try {
            String fileName = imageUrl.substring(
                    imageUrl.indexOf("/public/" + bucketName + "/") + ("/public/" + bucketName + "/").length()
            );
            webClient.delete()
                    .uri("/storage/v1/object/" + bucketName + "/" + fileName)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.warn("No se pudo eliminar la imagen de Supabase: {}", e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf("."));
    }
}