package com.project.bars.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryStorageService {

    private final String cloudinaryUrl;
    private final String cloudinaryFolder;

    public CloudinaryStorageService(@Value("${cloudinary.url:}") String cloudinaryUrl,
                                    @Value("${cloudinary.folder}") String cloudinaryFolder) {
        this.cloudinaryUrl = cloudinaryUrl;
        this.cloudinaryFolder = cloudinaryFolder;
    }

    public Map<String, Object> uploadPdf(MultipartFile file) {
        if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) {
            throw new IllegalStateException("CLOUDINARY_URL is not configured on the backend server.");
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
            throw new IllegalStateException("Failed to upload the PDF to Cloudinary.", exception);
        }
    }
}
