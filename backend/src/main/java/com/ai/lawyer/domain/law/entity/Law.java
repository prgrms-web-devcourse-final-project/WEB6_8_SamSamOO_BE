package com.ai.lawyer.domain.law.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Law {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    private String lawName; // 법령명

    private String lawField; // 법령분야

    private String ministry; // 소관부처

    private String promulgationNumber; // 공포번호

    private LocalDate promulgationDate; // 공포일자

    private LocalDate enforcementDate; // 시행일자

    @OneToMany(mappedBy = "law")
    @JsonManagedReference
    private List<Jang> jangList = new ArrayList<>();
}
