package ru.zotov.credit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class LoanApplicationRequestDTO {
    private BigDecimal amount;
    private Integer term;
    private String firstname;
    private String lastname;
    private String middlename;
    private String email;
    private LocalDate birtdate;
    private String passportSeries;
    private String passportNumber;

    private ScoringDataDTO scoring;
}