package com.example.picbox.services;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import com.google.api.services.drive.model.File;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class aiService {

    private final ChatClient chatClient;
    private final GoogleDriveIntegrationService driveService;
    private final ImageService imageService;

    public aiService(ChatClient.Builder chatClientBuilder, GoogleDriveIntegrationService driveService, ImageService imageService) {
        this.chatClient = chatClientBuilder.build();
        this.driveService = driveService;
        this.imageService = imageService;
    }

    public String[] searchfeature(List<File> allFiles, String name) {
        
        List<String> promptInfo = new ArrayList<>();

        for(File file : allFiles) {
            String filename = file.getName();
            promptInfo.add(filename);
        }

        String[] allFilesArray = promptInfo.toArray(new String[0]);
        
        String prompt = "Find files semantically related to the concept of '" + name + "'. " +
        "Use broad semantic matching (e.g., if the search is 'bird', include files named 'peacock', 'eagle', etc.). " +
        "Here is the list of file names: " + String.join(",", allFilesArray) + ". " +
        "Return ONLY a valid matching file names separated by commas. " +
        "Do not include any markdown formatting, conversational text, or explanations.";

        log.info("Sending prompt to AI: " + prompt);

        try {
            String aiResponse = this.chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("Received AI response: " + aiResponse);
            String[] matchingFileNames = aiResponse.split(",");
            for (int i = 0; i < matchingFileNames.length; i++) {
                matchingFileNames[i] = matchingFileNames[i].trim();
            }
            return matchingFileNames;

        } catch (Exception ex) {
            log.error("Error during AI response generation: " + ex.getMessage());
            return new String[0]; 
        }
    }


    public String callAi(String prompt) {
        log.info("Sending prompt to AI: " + prompt);

        try {
            String aiResponse = this.chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("Received AI response: " + aiResponse);
            return aiResponse;

        } catch (Exception ex) {
            log.error("Error during AI response generation: " + ex.getMessage());
            return ""; 
        }
    }

    public String[] generateQuestions(OAuth2AuthorizedClient client, String[] ids,int level) {

        String levelDescription = "Assume user level is " + level + " out of 100.";
        List<String> questions = new ArrayList<>();

        for(String id : ids) {
            try{
                byte[]  imageData = driveService.downloadFile(client, id);
                String imageinBase64 = imageService.encodeImageToBase64(imageData);

                String prompt = levelDescription + "Generate 1 unique, challenging question based on the user level and the following image: " + imageinBase64 + " . " +
                "Do not include any markdown formatting, conversational text, or explanations.";

                String result = callAi(prompt);
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
