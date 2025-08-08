package com.astro.url.validation;

import com.astro.shared.exceptions.UrlAuthorizationException;
import com.astro.user.model.User;
import com.astro.user.plan.model.Plan;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class ExpirationValidationService {

    private final Clock clock;

    public ExpirationValidationService(Clock clock) {
        this.clock = clock;
    }

    public void validateCustomDatePrivilege(User user) {
        if (user.getPlan().getMaxExpirationDays() == user.getPlan().getDefaultExpirationDays()) {
            throw new UrlAuthorizationException("Your current plan does not allow custom expiration dates.");
        }
    }

    public LocalDateTime determineExpirationDate(LocalDateTime requestedDate, User user) {
        Plan userPlan = user.getPlan();
        LocalDateTime defaultExpiration = LocalDateTime.now(clock).plusDays(userPlan.getDefaultExpirationDays());

        if (requestedDate == null) {
            return defaultExpiration;
        }

        validateCustomDatePrivilege(user);
        LocalDateTime maxExpiration = LocalDateTime.now(clock).plusDays(userPlan.getMaxExpirationDays());
        if (requestedDate.isAfter(maxExpiration)) {
            throw new IllegalArgumentException("Expiration date cannot exceed the maximum allowed for your plan.");
        }

        return requestedDate;
    }
}