package com.ai.lawyer.domain.law.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Jo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @Column(nullable = true, columnDefinition = "TEXT")
    String content;

    @ManyToOne
    @JoinColumn(name = "jang_id")
    @JsonBackReference
    private Jang jang;

    @OneToMany(mappedBy = "jo")
    @JsonManagedReference
    private List<Hang> hangList = new ArrayList<>();
}
