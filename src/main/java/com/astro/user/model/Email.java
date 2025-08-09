package com.astro.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Un Value Object que representa una dirección de correo electrónico.
 * Es inmutable y se autovalida en su construcción.
 * La anotación @Embeddable permite que JPA lo trate como un componente
 * incrustado dentro de otra entidad (como User).
 */
@Embeddable
public final class Email implements Serializable {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$");

    @Column(name = "email", nullable = false, unique = true, length = 100)
    @NotBlank
    private final String value;

    /**
     * Constructor protegido requerido por JPA.
     */
    protected Email() {
        this.value = null;
    }

    /**
     * Constructor público que valida el formato del email.
     * @param value La dirección de correo a encapsular.
     * @throws IllegalArgumentException si el formato del email es inválido.
     */
    public Email(String value) {
        if (value == null || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Formato de email inválido: " + value);
        }
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return Objects.equals(value, email.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}