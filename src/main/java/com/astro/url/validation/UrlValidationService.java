package com.astro.url.validation;

import com.astro.shared.exceptions.SlugAlreadyExistsException;
import com.astro.shared.exceptions.UrlAuthorizationException;
import com.astro.url.repository.UrlRepository;
import com.astro.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UrlValidationService {

    private final UrlRepository urlRepository;

    public void checkSlugAvailability(String slug) {
        if (urlRepository.findBySlug(slug.toLowerCase()).isPresent()) {
            throw new SlugAlreadyExistsException("The slug '" + slug + "' is already in use.");
        }
    }

    public void checkCustomSlugPrivilege(User user) {
        if (!user.getPlan().isCustomSlugEnabled()) {
            throw new UrlAuthorizationException("Your current plan does not allow custom slugs.");
        }
    }

    public boolean isSlugAvailable(String slug) {
        return urlRepository.findBySlug(slug.toLowerCase()).isEmpty();
    }
}