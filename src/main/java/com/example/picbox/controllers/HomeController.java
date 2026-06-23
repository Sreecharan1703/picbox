package com.example.picbox.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;



@Controller
public class HomeController {

    @RequestMapping("/")
    public String HomePage() {
        return "home";
    }

    @RequestMapping("/main")
    public String MainPage() {
        return "main";
    }

    @RequestMapping("/addpic")
    public String addPicture() {
        return "addpic";
    }
    
}