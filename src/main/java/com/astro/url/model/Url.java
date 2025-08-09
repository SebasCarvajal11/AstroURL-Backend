package com.astro.url.model;

import com.astro.shared.exceptions.SlugAlreadyExistsException;
import com.astro.stats.model.Click;
import com.astro.user.model.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@Entity
@Table(name = "urls", indexes = {
        @Index(name = "idx_url_slug", columnList = "slug", unique = true)
})
@Getter
@Setter
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false, unique = true, length = 15)
    @Setter(AccessLevel.PRIVATE)
    private String slug;

    @Column(name = "original_url", nullable = false, length = 2048)
    @Setter(AccessLevel.PRIVATE)
    private String originalUrl;

    @Column(name = "expiration_date", nullable = false)
    @Setter(AccessLevel.PRIVATE)
    private LocalDateTime expirationDate;

    @Column(nullable = true)
    @Setter(AccessLevel.PRIVATE)
    private String password;

    @Column(name = "click_count", nullable = false)
    private long clickCount = 0;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    @Setter(AccessLevel.PRIVATE)
    private User user;

    @OneToMany(
            mappedBy = "url",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    private List<Click> clicks = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Constructor protegido para ser usado por JPA y nuestra Factory.
     */
    protected Url() {}

    /**
     * Método de negocio para cambiar el slug.
     * La lógica de validación de disponibilidad ahora reside aquí.
     * @param newSlug El nuevo slug deseado.
     * @param isSlugAvailable Validador funcional que comprueba la disponibilidad.
     */
    public void changeSlug(String newSlug, Supplier<Boolean> isSlugAvailable) {
        if (!StringUtils.hasText(newSlug) || Objects.equals(this.slug, newSlug.toLowerCase())) {
            return; // No hay cambios o el nuevo slug es vacío
        }
        if (!isSlugAvailable.get()) {
            throw new SlugAlreadyExistsException("The slug '" + newSlug + "' is already in use.");
        }
        this.setSlug(newSlug.toLowerCase());
    }

    /**
     * Método de negocio para actualizar la URL original.
     * @param newOriginalUrl La nueva URL de destino.
     */
    public void updateOriginalUrl(String newOriginalUrl) {
        if (StringUtils.hasText(newOriginalUrl)) {
            this.setOriginalUrl(newOriginalUrl);
        }
    }

    /**
     * Método de negocio para actualizar la fecha de expiración.
     * @param newExpirationDate La nueva fecha de expiración.
     */
    public void updateExpirationDate(LocalDateTime newExpirationDate) {
        if (newExpirationDate != null) {
            // Se podrían añadir validaciones aquí, ej: no puede ser en el pasado.
            this.setExpirationDate(newExpirationDate);
        }
    }

    /**
     * Método de negocio para establecer o eliminar la contraseña.
     * @param newPassword La nueva contraseña en texto plano, o una cadena vacía/nula para eliminarla.
     * @param encodeFunction Función que se usará para codificar la contraseña.
     */
    public void updatePassword(String newPassword, java.util.function.Function<String, String> encodeFunction) {
        if (newPassword == null) {
            return; // No hay cambios
        }
        if (newPassword.isBlank()) {
            this.setPassword(null); // Eliminar contraseña
        } else {
            this.setPassword(encodeFunction.apply(newPassword)); // Establecer nueva contraseña
        }
    }

    // --- Métodos estáticos de fábrica para la creación ---

    public static Url createAnonymousUrl(String originalUrl, String slug, LocalDateTime expirationDate) {
        Url url = new Url();
        url.setOriginalUrl(originalUrl);
        url.setSlug(slug);
        url.setExpirationDate(expirationDate);
        url.setUser(null);
        return url;
    }

    public static Url createAuthenticatedUrl(String originalUrl, String slug, User user, LocalDateTime expirationDate, String password, java.util.function.Function<String, String> encodeFunction) {
        Url url = new Url();
        url.setOriginalUrl(originalUrl);
        url.setSlug(slug);
        url.setUser(user);
        url.setExpirationDate(expirationDate);
        if (StringUtils.hasText(password)) {
            url.setPassword(encodeFunction.apply(password));
        }
        return url;
    }
}