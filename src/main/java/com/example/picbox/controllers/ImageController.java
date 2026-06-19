package com.example.picbox.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/image")
public class ImageController {
    @PostMapping("/upload")
    public String UploadImage(@RequestBody String entity) {
        //TODO: process POST request
        
        return entity;
    }

    @GetMapping("/search")
    public String SearchImage(@RequestParam("query") String query){
        return new String();
    }
}