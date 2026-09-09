package com.flyship.controller;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final Set<String> ALLOWED_PROFILE_PICTURE_TYPES =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private static final long MAX_PROFILE_PICTURE_SIZE_BYTES = 5L * 1024 * 1024;

    @Value("${app.upload.dir:#{null}}")
    private String uploadDir;

    @Value("${app.gcs.bucket:#{null}}")
    private String gcsBucket;

    @PostMapping
    public ResponseEntity<?> uploadFile(
            @RequestParam(name = "photo", required = false) MultipartFile photo,
            @RequestParam(name = "profile_picture", required = false) MultipartFile profilePicture) {
        try {
            Map<String, String> response = new HashMap<>();

            if (photo != null && !photo.isEmpty()) {
                String filename = System.currentTimeMillis() + "-" + sanitize(photo.getOriginalFilename());
                response.put("url", upload(photo, "photos/" + filename));
            }

            if (profilePicture != null && !profilePicture.isEmpty()) {
                String validationError = validateProfilePicture(profilePicture);
                if (validationError != null) {
                    return ResponseEntity.badRequest().body(Map.of("error", validationError));
                }
                String filename = System.currentTimeMillis() + "-" + sanitize(profilePicture.getOriginalFilename());
                response.put("profile_url", upload(profilePicture, "profile/" + filename));
            }

            if (response.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No file uploaded"));
            }

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private String upload(MultipartFile file, String objectName) throws IOException {
        if (gcsBucket != null && !gcsBucket.isEmpty()) {
            String accessToken = GoogleCredentials.getApplicationDefault()
                    .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"))
                    .refreshAccessToken().getTokenValue();

            String uploadUrl = "https://storage.googleapis.com/upload/storage/v1/b/"
                    + gcsBucket + "/o?uploadType=media&name=" + objectName;

            HttpURLConnection conn = (HttpURLConnection) URI.create(uploadUrl).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", file.getContentType());
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(file.getBytes());
            }

            int code = conn.getResponseCode();
            conn.disconnect();

            if (code == 200) {
                return "https://storage.googleapis.com/" + gcsBucket + "/" + objectName;
            }
            throw new IOException("GCS upload failed with status " + code);
        }

        Path dir = Paths.get(uploadDir, objectName).getParent();
        Files.createDirectories(dir);
        file.transferTo(Paths.get(uploadDir, objectName).toFile());
        return "/uploads/" + objectName;
    }

    private String validateProfilePicture(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_PROFILE_PICTURE_TYPES.contains(contentType.toLowerCase())) {
            return "Profile picture must be a JPEG, PNG, GIF, or WEBP image";
        }
        if (file.getSize() > MAX_PROFILE_PICTURE_SIZE_BYTES) {
            return "Profile picture must be smaller than 5MB";
        }
        return null;
    }

    private String sanitize(String name) {
        if (name == null) return "file";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
