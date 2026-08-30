package com.flyship.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadControllerTest {

    private UploadController uploadController;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        uploadController = new UploadController();
        ReflectionTestUtils.setField(uploadController, "uploadDir", tempDir.toString());
    }

    @Test
    void uploadFile_acceptsValidProfilePicture() {
        MockMultipartFile file = new MockMultipartFile(
                "profile_picture", "avatar.png", "image/png", new byte[]{1, 2, 3, 4});

        ResponseEntity<?> response = uploadController.uploadFile(null, file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertTrue(body.get("profile_url").contains("profile/"));
    }

    @Test
    void uploadFile_rejectsNonImageProfilePicture() {
        MockMultipartFile file = new MockMultipartFile(
                "profile_picture", "resume.pdf", "application/pdf", new byte[]{1, 2, 3, 4});

        ResponseEntity<?> response = uploadController.uploadFile(null, file);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertTrue(body.get("error").toLowerCase().contains("jpeg"));
    }

    @Test
    void uploadFile_rejectsOversizedProfilePicture() {
        byte[] oversized = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "profile_picture", "avatar.png", "image/png", oversized);

        ResponseEntity<?> response = uploadController.uploadFile(null, file);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertTrue(body.get("error").toLowerCase().contains("5mb"));
    }

    @Test
    void uploadFile_returnsBadRequestWhenNoFileProvided() {
        ResponseEntity<?> response = uploadController.uploadFile(null, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
