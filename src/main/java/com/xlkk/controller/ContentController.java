package com.xlkk.controller;

import com.xlkk.service.ContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContentController {
    @Autowired
    private ContentService contentService;

    @GetMapping("/parse/{keyword}")
    public boolean parse(@PathVariable("keyword") String keyword){
        return contentService.parseContent(keyword);
    }
}
