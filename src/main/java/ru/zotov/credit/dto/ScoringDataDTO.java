package ru.zotov.credit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.zotov.credit.dto.enums.Gender;
import ru.zotov.credit.dto.enums.MaritalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class ScoringDataDTO {
    private BigDecimal Amount;
    private Integer term;
    private String firstname;
    private String lastname;
    private String middlename;
    private Gender gender;
    private LocalDate birtdate;
    private String passportSeries;
    private String passportNumber;
    private LocalDate passportIssueDate;
    private String passportIssueBranch;
    private MaritalStatus maritalStatus;
    private Integer dependentAmount;
    private EmploymentDTO employment;
    private String account;
    private Boolean isInsuranceEnabled;
    private Boolean isSalaryClient;

}