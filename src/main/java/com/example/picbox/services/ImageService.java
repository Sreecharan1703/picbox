package com.example.picbox.services;

public class ImageService {
    public String encodeImageToBase64(byte[] imageData) {
        return java.util.Base64.getEncoder().encodeToString(imageData);
    }
}
