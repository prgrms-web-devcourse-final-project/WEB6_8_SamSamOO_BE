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
@Table(name = "hang")
public class Hang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @Column(nullable = true, columnDefinition = "TEXT")
    String content;

    @ManyToOne
    @JoinColumn(name = "jo_id")
    @JsonBackReference
    private Jo jo;

    @OneToMany(mappedBy = "hang")
    @JsonManagedReference
    private List<Ho> hoList = new ArrayList<>();
}
