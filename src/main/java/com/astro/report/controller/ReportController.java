package com.astro.report.controller;

import com.astro.report.service.ReportService;
import com.astro.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("isAuthenticated() and hasAuthority('PLAN_SIRIO')")
    public ResponseEntity<byte[]> getPdfReport(Authentication authentication) throws IOException {
        User user = (User) authentication.getPrincipal();
        byte[] pdfContents = reportService.generatePdfReport(user);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"astrourl_report.pdf\"");

        return ResponseEntity.ok().headers(headers).body(pdfContents);
    }
}