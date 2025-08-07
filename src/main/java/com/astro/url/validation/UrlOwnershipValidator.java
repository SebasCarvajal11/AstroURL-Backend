package com.astro.url.validation;

import com.astro.shared.exceptions.UrlAuthorizationException;
import com.astro.url.model.Url;
import com.astro.user.model.User;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class UrlOwnershipValidator {

    public void validate(Url url, User user) {
        if (url.getUser() == null || !Objects.equals(url.getUser().getId(), user.getId())) {
            throw new UrlAuthorizationException("You are not authorized to access this resource.");
        }
    }
}