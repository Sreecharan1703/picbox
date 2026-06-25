package com.example.picbox.services;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class GoogleDriveIntegrationService {

    aiService aiservice;
    ImageService imageService;

    GoogleDriveIntegrationService(aiService aiservice, ImageService imageService) {
        this.aiservice = aiservice;
        this.imageService = imageService;
    }

    private Drive getDriveService(OAuth2AuthorizedClient client) throws Exception {
        String tokenValue = client.getAccessToken().getTokenValue();
        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(tokenValue, null));
        
        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("Spring Boot Drive Integration")
                .build();
    }

    public String getOrCreatePicboxFolder(OAuth2AuthorizedClient client) throws Exception {
        Drive driveService = getDriveService(client);
        String folderName = "PicBox";
        String query = "mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and trashed=false";
        
        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        if (!result.getFiles().isEmpty()) {
            return result.getFiles().get(0).getId();
        } else {
            File fileMetadata = new File();
            fileMetadata.setName(folderName);
            fileMetadata.setMimeType("application/vnd.google-apps.folder");

            File folder = driveService.files().create(fileMetadata)
                    .setFields("id")
                    .execute();
            return folder.getId();
        }
    }

    public List<File> getDriveFiles(OAuth2AuthorizedClient client) throws Exception {
        Drive driveService = getDriveService(client);
        
        String folderId = getOrCreatePicboxFolder(client);
        String query = "'" + folderId + "' in parents and trashed=false";
        FileList result = driveService.files().list()
                .setQ(query)
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name, mimeType , description)")
                .execute();
        return result.getFiles();
    }

    public String uploadFile(OAuth2AuthorizedClient client,
        MultipartFile multipartFile,String description) throws Exception {

        Drive driveService = getDriveService(client);
        String folderId = getOrCreatePicboxFolder(client);
        
        File fileMetadata = new File();
        fileMetadata.setName(multipartFile.getOriginalFilename());
        fileMetadata.setDescription(description);

        fileMetadata.setParents(Collections.singletonList(folderId));

        InputStreamContent mediaContent = new InputStreamContent(
                multipartFile.getContentType(), 
                multipartFile.getInputStream()
        );

        File file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();
                
        return file.getId();
    }

    public File getFileMetadata(OAuth2AuthorizedClient client, String fileId) throws Exception {
        return getDriveService(client).files().get(fileId).setFields("id, name, mimeType, description").execute();
    }

    public byte[] downloadFile(OAuth2AuthorizedClient client, String fileId) throws Exception {
        Drive driveService = getDriveService(client);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
        return outputStream.toByteArray();
    }

    public List<File> searchFilesByName(OAuth2AuthorizedClient client, String name) throws Exception {
        List<File> allFiles = getDriveFiles(client); 
        String[] filenames = aiservice.searchfeature(allFiles, name);

        List<File> outputFileList = allFiles.stream()
                .filter(file -> file.getName() != null && java.util.Arrays.asList(filenames).contains(file.getName()))
                .toList();

        return outputFileList;
    }

    public String[] generateQuestions(OAuth2AuthorizedClient client, String[] ids,int level) {
        List<String> questions = new ArrayList<>();
        for(String id : ids) {
            try{
                byte[]  imageData = downloadFile(client, id);
                String imageinBase64 = imageService.encodeImageToBase64(imageData);
                String result = aiservice.genQuestion(level, imageinBase64);
                questions.add(result);
            }
            catch(Exception e){
                log.error("Error downloading or encoding image with ID: " + id, e);
                continue; 
            }
        }
        return questions.toArray(new String[0]);
    }

}