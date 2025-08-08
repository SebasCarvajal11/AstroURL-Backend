package com.astro.report.service;

import com.astro.report.generator.PdfReportGenerator;
import com.astro.url.model.Url;
import com.astro.url.repository.UrlRepository;
import com.astro.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UrlRepository urlRepository;
    private final PdfReportGenerator pdfGenerator;
    private final Clock clock;

    public byte[] generatePdfReport(User user) throws IOException {
        List<Url> activeUrls = findActiveUrlsForUser(user);
        return pdfGenerator.generate(user, activeUrls);
    }

    private List<Url> findActiveUrlsForUser(User user) {
        return urlRepository.findByUserAndExpirationDateAfter(user, LocalDateTime.now(clock));
    }
}