package com.acueducto.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Servicio para eliminar archivos de Supabase Storage cuando se borran reportes vencidos.
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
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Elimina un archivo de Supabase Storage a partir de su URL pública.
     * Ejemplo de URL: https://xxx.supabase.co/storage/v1/object/public/reportes/reportes-comunidad/123.jpg
     * Extrae el bucket y el path, y envía un DELETE.
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
}
