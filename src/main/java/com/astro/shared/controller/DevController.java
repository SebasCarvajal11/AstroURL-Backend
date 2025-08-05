package com.astro.shared.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dev")
public class DevController {

    @GetMapping("/hello-protected")
    public ResponseEntity<Map<String, String>> sayHello() {
        return ResponseEntity.ok(Map.of("message", "Hello from a protected endpoint!"));
    }
}