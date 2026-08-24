package com.acueducto.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Servicio para gestionar archivos en Supabase Storage: subida y eliminacion.
 * Usa la REST API de Supabase directamente (sin SDK adicional).
 */
@Slf4j
@Service
public class SupabaseStorageService {

    private final String supabaseUrl;
    private final String supabaseServiceKey;
    private final HttpClient httpClient;

    public SupabaseStorageService(
            @Value("${SUPABASE_URL:}") String supabaseUrl,
            @Value("${SUPABASE_SERVICE_KEY:}") String supabaseServiceKey) {
        this.supabaseUrl = supabaseUrl != null ? supabaseUrl.replace("/", "") : "";
        this.supabaseServiceKey = supabaseServiceKey != null ? supabaseServiceKey : "";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Sube un archivo a un bucket de Supabase Storage.
     * @param bucket Nombre del bucket (ej: "institutional", "reportes", "publicaciones")
     * @param file Archivo MultipartFile a subir
     * @param folder Subcarpeta dentro del bucket (ej: "logos", "firmas", "sellos")
     * @return URL pública del archivo subido, o null si falla
     */
    public String subirArchivo(String bucket, MultipartFile file, String folder) {
        if (supabaseUrl.isEmpty() || supabaseServiceKey.isEmpty()) {
            log.debug("Supabase no configurado, saltando subida a {}/{}", bucket, folder);
            return null;
        }

        try {
            String extension = getExtension(file.getOriginalFilename());
            String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String filePath = folder + "/" + fecha + "/" + UUID.randomUUID() + extension;

            String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + filePath;

            byte[] fileBytes = file.getBytes();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .header("Authorization", "Bearer " + supabaseServiceKey)
                    .header("apikey", supabaseServiceKey)
                    .header("Content-Type", file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                    .header("x-upsert", "true")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + filePath;
                log.info("Archivo subido a Supabase: {}/{} -> {}", bucket, filePath, publicUrl);
                return publicUrl;
            } else {
                log.warn("Supabase respondió {} al subir {}/{}: {}", response.statusCode(), bucket, filePath, response.body());
                return null;
            }
        } catch (IOException e) {
            log.error("Error al leer archivo para subir a Supabase: {}", e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Timeout al subir archivo a Supabase: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Elimina un archivo de Supabase Storage a partir de su URL pública.
     */
    public void eliminarPorUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank() || supabaseUrl.isEmpty() || supabaseServiceKey.isEmpty()) {
            return;
        }

        try {
            String marker = "/storage/v1/object/public/";
            int idx = imageUrl.indexOf(marker);
            if (idx < 0) return;

            String afterMarker = imageUrl.substring(idx + marker.length());
            int slashIdx = afterMarker.indexOf('/');
            if (slashIdx < 0) return;

            String bucket = afterMarker.substring(0, slashIdx);
            String path = afterMarker.substring(slashIdx + 1);

            eliminarArchivo(bucket, path);
        } catch (Exception e) {
            log.warn("No se pudo eliminar imagen de Supabase: {}", e.getMessage());
        }
    }

    /**
     * Elimina un archivo específico de un bucket en Supabase Storage.
     */
    public void eliminarArchivo(String bucket, String filePath) {
        if (supabaseUrl.isEmpty() || supabaseServiceKey.isEmpty()) {
            log.debug("Supabase no configurado, saltando eliminación de {}/{}", bucket, filePath);
            return;
        }

        try {
            String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + filePath;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(deleteUrl))
                    .header("Authorization", "Bearer " + supabaseServiceKey)
                    .header("apikey", supabaseServiceKey)
                    .DELETE()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 404) {
                log.info("Imagen eliminada de Supabase: {}/{}", bucket, filePath);
            } else {
                log.warn("Supabase respondió {} al eliminar {}/{}", response.statusCode(), bucket, filePath);
            }
        } catch (Exception e) {
            log.warn("Error al eliminar {}/{} de Supabase: {}", bucket, filePath, e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".jpg";
    }
}
