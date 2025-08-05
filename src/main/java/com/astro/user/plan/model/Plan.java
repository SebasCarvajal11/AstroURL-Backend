package com.astro.user.plan.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "plans")
@Getter
@Setter
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false)
    private int urlLimitPerDay;

    @Column(nullable = false)
    private int apiLimitPerMonth;

    @Column(nullable = false)
    private boolean customSlugEnabled;
}
