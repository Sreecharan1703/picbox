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
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class GoogleDriveIntegrationService {

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

    public List<File> getDriveFiles(OAuth2AuthorizedClient client) throws Exception {
        Drive driveService = getDriveService(client);
        FileList result = driveService.files().list()
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name, mimeType)")
                .execute();
        return result.getFiles();
    }

    public String uploadFile(OAuth2AuthorizedClient client, MultipartFile multipartFile) throws Exception {
        Drive driveService = getDriveService(client);
        
        File fileMetadata = new File();
        fileMetadata.setName(multipartFile.getOriginalFilename());

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
        return getDriveService(client).files().get(fileId).setFields("id, name, mimeType").execute();
    }

    public byte[] downloadFile(OAuth2AuthorizedClient client, String fileId) throws Exception {
        Drive driveService = getDriveService(client);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
        return outputStream.toByteArray();
    }
}