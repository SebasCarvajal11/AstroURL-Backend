package com.astro.url.creation;

import com.astro.url.dto.UrlResponse;
import com.astro.url.dto.UrlShortenRequest;
import com.astro.url.mapper.UrlMapper;
import com.astro.url.model.Url;
import com.astro.url.repository.UrlRepository;
import com.astro.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UrlCreationService {

    private final UrlRepository urlRepository;
    private final UrlFactory urlFactory;
    private final UrlMapper urlMapper;

    @Transactional
    public UrlResponse createShortUrl(UrlShortenRequest request, User user, String ipAddress, String requestUrl) {
        Url newUrl;
        if (user != null) {
            newUrl = urlFactory.createForAuthenticated(request, user);
        } else {
            newUrl = urlFactory.createForAnonymous(request, ipAddress);
        }

        urlRepository.save(newUrl);
        return urlMapper.toUrlResponse(newUrl, getBaseUrl(requestUrl));
    }

    private String getBaseUrl(String requestUrl) {
        return requestUrl.substring(0, requestUrl.indexOf("/api/"));
    }
}