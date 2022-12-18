package ru.zotov.credit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.zotov.credit.dto.enums.EmploymentStatus;
import ru.zotov.credit.dto.enums.Position;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class EmploymentDTO {
    private EmploymentStatus employmentStatus;
    private String employerINN;
    private BigDecimal salary;
    private Position position;
    private Integer workExperienceTotal;
    private Integer workExperienceCurrent;
}