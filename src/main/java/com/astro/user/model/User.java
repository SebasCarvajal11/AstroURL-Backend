package com.astro.user.model;

import com.astro.user.plan.model.Plan;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * CORRECCIÓN: Se reemplaza el campo 'email' de tipo String
     * por nuestro nuevo Value Object 'Email'.
     * La anotación @Embedded le indica a JPA cómo mapear este objeto.
     */
    @Embedded
    private Email email;

    @Column(nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Método de conveniencia para obtener el valor del email como String.
     * @return El email como String.
     */
    public String getEmail() {
        return this.email != null ? this.email.value() : null;
    }

    /**
     * Método de conveniencia para establecer el email a partir de un String.
     * La validación ocurrirá al crear la nueva instancia de Email.
     * @param emailString El email como String a establecer.
     */
    public void setEmail(String emailString) {
        this.email = new Email(emailString);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        SimpleGrantedAuthority userRole = new SimpleGrantedAuthority("ROLE_USER");
        SimpleGrantedAuthority planAuthority = new SimpleGrantedAuthority("PLAN_" + this.plan.getName().toUpperCase());
        return List.of(userRole, planAuthority);
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}