package com.example.picbox.controllers;

import com.example.picbox.services.GoogleDriveIntegrationService;
import com.example.picbox.services.aiService;
import com.google.api.services.drive.model.File;

import lombok.var;

import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;




@Controller
public class DriveWebController {

    private final aiService aiservice;
    private final GoogleDriveIntegrationService driveService;

    DriveWebController(GoogleDriveIntegrationService driveService, aiService aiservice) {
        this.aiservice = aiservice;
        this.driveService = driveService;
    }

    @GetMapping("/login")
    public String index() {
        return "index_view";
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<Resource> getImage(
            @PathVariable String id,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) {
        try {
            byte[] imageData = driveService.downloadFile(authorizedClient, id);
            ByteArrayResource resource = new ByteArrayResource(imageData);
            
            String fileName = driveService.getFileMetadata(authorizedClient, id).getName();
            String imageType = fileName.endsWith(".png") ? "png" : "jpeg";

            if(imageType.equals("png")) {
                return ResponseEntity.ok().contentType(org.springframework.http.MediaType.IMAGE_PNG).body(resource);
            } else{
                return ResponseEntity.ok().contentType(org.springframework.http.MediaType.IMAGE_JPEG).body(resource);
            } 
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/gallery")
    public String gallery(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            Model model) {
        
        model.addAttribute("name", oauth2User.getAttribute("name"));
        
        try {
            var files = driveService.getDriveFiles(authorizedClient);
            model.addAttribute("files", files);
        } catch (Exception e) {
            model.addAttribute("error", "Could not fetch Drive files: " + e.getMessage());
        }
        
        return "gallery_view";
    }

    @PostMapping("/upload")
    public String uploadFile(
            @RequestParam("file") MultipartFile file,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            @RequestParam("description") String description,
            Model model) {
        try {
            driveService.uploadFile(authorizedClient, file, description);
        } catch (Exception e) {
            model.addAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "redirect:/gallery";
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String id,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) {
        try {
            com.google.api.services.drive.model.File fileMeta = driveService.getFileMetadata(authorizedClient, id);
            byte[] fileData = driveService.downloadFile(authorizedClient, id);
            ByteArrayResource resource = new ByteArrayResource(fileData);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileMeta.getName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/searchfiles")
    public String search(@RequestParam String name, Model model,
        @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient
    ) {
        try {
            List<File> files = driveService.searchFilesByName(authorizedClient, name);
            model.addAttribute("resultfiles", files);
            String resultMessage = files.size() + " files found with name: " + name;
            model.addAttribute("searchresults", resultMessage);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "forward:/gallery";
    }


    @GetMapping("/quiz")
    public String makeQuiz(@RequestParam String imageIds,Model model,
        @AuthenticationPrincipal OAuth2User oauth2User,
        @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
        @RequestParam int level
    ) {
        String[] ids = imageIds.split("&");
        for(String id : ids) {
            id = id.replace("imageId=", "");
        }
        
        String[] questions = aiservice.generateQuestions(authorizedClient, ids,level);
        model.addAttribute("questions", questions);
        return "quiz_view";
    }
    
    
}