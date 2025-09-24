package com.ai.lawyer.domain.law.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Ho {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @Column(nullable = true, columnDefinition = "TEXT")
    String content;

    @ManyToOne
    @JoinColumn(name = "hang_id")
    @JsonBackReference
    private Hang hang;
}
