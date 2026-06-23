package com.example.picbox.services;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import com.google.api.services.drive.model.File;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class aiService {

    private final ChatClient chatClient;

    public aiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String[] searchfeature(List<File> allFiles, String name) {
        
        String prompt = "Find files semantically related to the concept of '" + name + "'. " +
        "Use broad semantic matching (e.g., if the search is 'bird', include files named 'peacock', 'eagle', etc.). " +
        "Here is the list of file metadata: " + allFiles.toString() + ". " +
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
}
