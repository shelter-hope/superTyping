package com.typinggame.lab.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lab_algorithm")
public class LabAlgorithm {
    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "default_params", nullable = false, columnDefinition = "json")
    private String defaultParams;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion = 1;
}
