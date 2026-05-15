package com.axiomai.flowstep.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "flow_steps")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long flowId;

    private Integer stepOrder;

    private String action;

    private String elementName;

    private String locatorType;

    @Column(length = 5000)
    private String locatorValue;

    @Column(length = 5000)
    private String fallbackLocator;

    @Column(length = 5000)
    private String aiSemanticDescription;

    @Column(length = 5000)
    private String inputValue;

    @Column(length = 5000)
    private String expectedValue;

    private Boolean required;

}