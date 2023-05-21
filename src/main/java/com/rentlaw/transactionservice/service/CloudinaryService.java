package com.rentlaw.transactionservice.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class CloudinaryService {
    @Autowired
    private Cloudinary cloudinary;

    public String uploadImage(MultipartFile imageFile) {
        try {
            var uploader = cloudinary.uploader();
            Map<String, Object> uploadResult = uploader.upload(imageFile.getBytes(), ObjectUtils.emptyMap());
            return uploadResult.get("public_id").toString();
        } catch (Exception e) {
            return null;
        }
    }

    public String getImageUrl(String imageId) {
        try {
            return cloudinary.url().secure(true).format("jpg")
                    .publicId(imageId)
                    .generate();
        } catch (Exception e) {
            return null;
        }
    }

    public String deleteImage(String imageId) {
        try {
            var uploader = cloudinary.uploader();
            return uploader.destroy(imageId, ObjectUtils.emptyMap()).toString();
        } catch (Exception e) {
            return null;
        }
    }
}
