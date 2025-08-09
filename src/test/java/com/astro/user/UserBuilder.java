package com.astro.user;

import com.astro.user.model.Email;
import com.astro.user.model.User;
import com.astro.user.plan.model.Plan;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Implementación del patrón Test Data Builder para la entidad User.
 * Permite la creación fluida y legible de objetos User para su uso en tests.
 * Proporciona valores por defecto sensatos que pueden ser sobreescritos
 * según las necesidades de cada prueba.
 */
public class UserBuilder {

    private Long id;
    private String username = "default-user";
    private String email = "default@test.com";
    private String password = "password123";
    private Plan plan;

    public UserBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public UserBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public UserBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public UserBuilder withPlan(Plan plan) {
        this.plan = plan;
        return this;
    }

    /**
     * Construye y devuelve el objeto User.
     * @param passwordEncoder El codificador de contraseñas para hashear la contraseña.
     * @return Una instancia de User.
     */
    public User build(PasswordEncoder passwordEncoder) {
        if (plan == null) {
            throw new IllegalStateException("Un Plan debe ser proporcionado para construir un User.");
        }
        User user = new User();
        user.setId(this.id);
        user.setUsername(this.username);
        user.setEmail(this.email);
        user.setPassword(passwordEncoder.encode(this.password));
        user.setPlan(this.plan);
        return user;
    }
}