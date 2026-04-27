package com.project.bars.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryStorageService {

    private final String cloudinaryUrl;
    private final String cloudinaryFolder;
    private final Path localUploadDir;

    public CloudinaryStorageService(@Value("${cloudinary.url:}") String cloudinaryUrl,
                                    @Value("${cloudinary.folder}") String cloudinaryFolder,
                                    @Value("${storage.local-dir:uploads}") String localUploadDir) {
        this.cloudinaryUrl = cloudinaryUrl;
        this.cloudinaryFolder = cloudinaryFolder;
        this.localUploadDir = Path.of(localUploadDir).toAbsolutePath().normalize();
    }

    public Map<String, Object> uploadPdf(MultipartFile file) {
        if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) {
            return saveLocally(file, "Cloudinary is not configured. Stored the upload locally instead.");
        }

        try {
            Cloudinary cloudinary = new Cloudinary(cloudinaryUrl);
            cloudinary.config.secure = true;

            String originalName = file.getOriginalFilename() == null ? "report.pdf" : file.getOriginalFilename();
            String publicId = UUID.randomUUID() + "-" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");

            return cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "auto",
                            "folder", cloudinaryFolder,
                            "public_id", publicId,
                            "use_filename", true,
                            "unique_filename", false,
                            "overwrite", false
                    )
            );
        } catch (IOException exception) {
            return saveLocally(file, "Cloudinary upload failed. Stored the upload locally instead.");
        }
    }

    private Map<String, Object> saveLocally(MultipartFile file, String note) {
        try {
            Files.createDirectories(localUploadDir);

            String originalName = file.getOriginalFilename() == null ? "report.pdf" : file.getOriginalFilename();
            String storedName = UUID.randomUUID() + "-" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path destination = localUploadDir.resolve(storedName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("provider", "local");
            result.put("public_id", storedName);
            result.put("asset_id", storedName);
            result.put("secure_url", destination.toUri().toString());
            result.put("format", "pdf");
            result.put("resource_type", "raw");
            result.put("note", note);
            result.put("local_path", destination.toString());
            return result;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store the uploaded PDF locally.", exception);
        }
    }
}
