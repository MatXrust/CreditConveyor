package ru.zotov.credit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.zotov.credit.dto.*;
import ru.zotov.credit.dto.enums.EmploymentStatus;
import ru.zotov.credit.dto.enums.Gender;
import ru.zotov.credit.dto.enums.MaritalStatus;
import ru.zotov.credit.dto.enums.Position;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.math.BigDecimal.ROUND_HALF_UP;

@Service
@RequiredArgsConstructor
@ConfigurationProperties
public class requestServiceImpl implements requestService {

    private final double mainRate = 20;

    private boolean prescoring(LoanApplicationRequestDTO request){

        String firstName = request.getFirstname();
        if(!firstName.matches("^[a-zA-Z]{2,30}")) return false;

        String lastName = request.getLastname();
        if(!lastName.matches("^[a-zA-Z]{2,30}")) return false;

        String middleName = request.getMiddlename();
        if(!middleName.matches("^[a-zA-Z]{2,30}")) return false;

        BigDecimal requestAmount = request.getAmount();
        BigDecimal minAmount = BigDecimal.valueOf(10000.0);
        if(requestAmount.compareTo(minAmount)==-1) return false;

        Integer term = request.getTerm();
        if(term<6)return false;

        LocalDate birthDate = request.getBirtdate();
        int age = birthDate.until(LocalDate.now()).getYears();
        if (age < 18) return false;

        String email = request.getEmail();
        if (!email.matches("[\\w\\.]{2,50}@[\\w\\.]{2,20}"))return false;

        String passportSeries = request.getPassportSeries();
        if (!passportSeries.matches("[0-9]{4}")) return false;

        String passportNumber = request.getPassportNumber();
        if (!passportNumber.matches("[0-9]{6}")) return false;

        return true;
    }

    private boolean scoring(LoanApplicationRequestDTO request){

        EmploymentDTO employment = request.getScoring().getEmployment();

        if (employment.getEmploymentStatus() == EmploymentStatus.UNEMPLOYED) return false;

        BigDecimal limitRequestedAmount = employment.getSalary().multiply(BigDecimal.valueOf(20));
        if (limitRequestedAmount.compareTo(request.getAmount()) == -1) return false;

        LocalDate birthDate = request.getBirtdate();
        int age = birthDate.until(LocalDate.now()).getYears();
        if (age < 20 || age > 60) return false;

        int workExperienceTotal = employment.getWorkExperienceTotal();
        if (workExperienceTotal < 12) return false;

        int workExperienceCurrent = employment.getWorkExperienceCurrent();
        if (workExperienceCurrent < 3) return false;

        return true;
    }

    @Override
    public ResponseEntity<?> getOffers( LoanApplicationRequestDTO request) {

        prescoring(request);

        long chiefId = createChiefAppId(request);
        double personalAmount = request.getAmount().doubleValue();
        Integer personalTerm = request.getTerm();
        List<LoanOfferDTO> listLoanOffers = new ArrayList<>();

        boolean isInsurance = true, isSalary = false;
        if(scoring(request)){
            for (int i = 0; i < 4; i++) {
                if (i % 2 == 0) isInsurance = !isInsurance;
                else isSalary = !isSalary;

                double personalRate = mainRate, amountFromBank = personalAmount;
                if (isSalary) personalRate--;
                if (isInsurance) {
                    personalRate -= 2;
                    amountFromBank += сalculateInsurance(personalAmount);
                }

                BigDecimal personalMonthlyPayment = calculateMonthlyPayment(amountFromBank, personalTerm, personalRate);
                long personalAppId = chiefId + (long) (personalMonthlyPayment.doubleValue() * personalRate);
                BigDecimal personalTotalAmount = personalMonthlyPayment.multiply(BigDecimal.valueOf(personalTerm));

                listLoanOffers.add(new LoanOfferDTO().builder()
                        .applicationId(personalAppId)
                        .requestedAmount(BigDecimal.valueOf(personalAmount))
                        .term(personalTerm)
                        .monthlyPayment(personalMonthlyPayment)
                        .totalAmount(personalTotalAmount)
                        .rate(BigDecimal.valueOf(personalRate))
                        .isInsuranceEnabled(isInsurance)
                        .isSalaryClient(isSalary)
                        .build());
            }
            Collections.sort(listLoanOffers, (o2, o1) -> o1.getRate().subtract(o2.getRate()).intValue());

            return ResponseEntity.ok(listLoanOffers);
        }

        return new ResponseEntity<>("Вам отказано в кредите!", HttpStatus.BAD_REQUEST);

    }

    public ResponseEntity<?> creditProcessing( ScoringDataDTO request){

        List<CreditDTO> listCredit = new ArrayList<>();

        double personalRate = calculationRate(request),
                requestAmount = request.getAmount().doubleValue();
        boolean personIsInsurance = request.getIsInsuranceEnabled(),
                personIsSalary = request.getIsSalaryClient();
        Integer personalTerm = request.getTerm();

        if (personIsSalary) personalRate--;
        if (personIsInsurance) {
            personalRate -= 2;
            requestAmount += сalculateInsurance(request.getAmount().doubleValue());
        }

        BigDecimal personalMonthlyPayment = calculateMonthlyPayment(requestAmount, personalTerm, personalRate);
        List<PaymentScheduleElement> personPaymentSchedule = createListPaymentSchedule(personalMonthlyPayment.doubleValue(),
                requestAmount, personalTerm, personalRate);

        double personTotalAmount = personPaymentSchedule.stream().mapToDouble(x -> x.getTotalPayment().doubleValue()).sum();
        BigDecimal personalPsk = BigDecimal.valueOf((personTotalAmount / requestAmount - 1) / (personalTerm / 12.0) * 100)
                .setScale(3, ROUND_HALF_UP);

        listCredit.add(new CreditDTO().builder()
                .Amount(BigDecimal.valueOf(personTotalAmount).setScale(3, ROUND_HALF_UP))
                .term(personalTerm)
                .rate(BigDecimal.valueOf(personalRate))
                .psk(personalPsk)
                .monthlyPayment(personalMonthlyPayment)
                .isInsuranceEnabled(personIsInsurance)
                .isSalaryClient(personIsSalary)
                .paymentSchedule(personPaymentSchedule).build());

        return ResponseEntity.ok(listCredit);
    }


    private double calculationRate(ScoringDataDTO request){
        double newPersonalRate = mainRate;
        EmploymentDTO employment = request.getEmployment();
        MaritalStatus maritalStatus = request.getMaritalStatus();
        Gender gender = request.getGender();

        if (employment.getEmploymentStatus() == EmploymentStatus.EMPLOYED) newPersonalRate++;
        else if (employment.getEmploymentStatus() == EmploymentStatus.BUSINESS_OWNER) newPersonalRate += 3;
        if (employment.getPosition() == Position.SIMPLE_MANAGER) newPersonalRate -= 4;
        else if (employment.getPosition() == Position.CIVIL_SERVANT) newPersonalRate -= 3;
        else if (employment.getPosition() == Position.MIDDLE_MANAGER) newPersonalRate -= 2;
        if (maritalStatus == MaritalStatus.MARRIED) newPersonalRate -= 3;
        else if (maritalStatus == MaritalStatus.NOT_MARRIED) newPersonalRate++;

        LocalDate birthDate = request.getBirtdate();
        int age = birthDate.until(LocalDate.now()).getYears();
        if (gender == Gender.FEMALE && age > 35) newPersonalRate -= 3;
        else if (gender == Gender.MALE && age > 30 && age < 55) newPersonalRate -= 3;
        else if (gender == Gender.NON_BINARY) newPersonalRate += 3;
        if (request.getDependentAmount() > 1) newPersonalRate++;
        return newPersonalRate;
    }

    private long createChiefAppId(LoanApplicationRequestDTO loanApplicationRequest) {
        String all_str_fields_request = loanApplicationRequest.getFirstname() + loanApplicationRequest.getMiddlename() +
                loanApplicationRequest.getLastname() + loanApplicationRequest.getEmail() +
                loanApplicationRequest.getPassportNumber() + loanApplicationRequest.getPassportSeries() +
                loanApplicationRequest.getBirtdate();
        long appId = (all_str_fields_request).chars().sum();
        appId += loanApplicationRequest.getTerm();
        appId += loanApplicationRequest.getAmount().intValue();
        return appId;
    }
    private double сalculateInsurance(double amount) {

        double insurance = amount / 4;
        int intAmount = (int) amount, amountForCount = (int) amount, countDigitalAmount = 0;
        while (amountForCount != 0) {
            amountForCount /= 10;
            countDigitalAmount++;
        }
        Integer firstDigitAmount = (int) (intAmount / Math.pow(10, (int) Math.log10(intAmount)));
        insurance += amount / (countDigitalAmount + firstDigitAmount);
        return Math.ceil(insurance * 100) / 100;
    }
    private BigDecimal calculateMonthlyPayment(double amount, Integer term, double rate) {
        double rateMonth = rate / 12 / 100;
        double ratioAnnuity = (rateMonth * Math.pow((1 + rateMonth), term)) / (Math.pow((1 + rateMonth), term) - 1);
        BigDecimal monthlyPayment = BigDecimal.valueOf(amount * ratioAnnuity);
        return monthlyPayment.setScale(2, ROUND_HALF_UP);
    }
    private List createListPaymentSchedule(double monthlyPayment, double amount, Integer term, double rate) {
        List<PaymentScheduleElement> paymentSchedule = new ArrayList<>();
        LocalDate now = LocalDate.now();
        LocalDate paymentDay = LocalDate.of(now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        double bodyCredit, percentageAmount, rateInDigital = rate / 100;
        long yearDays, monthDays;

        for (int i = 1; i <= term; i++) {

            monthDays = ChronoUnit.DAYS.between(paymentDay, paymentDay.plusMonths(1));
            paymentDay = paymentDay.plusMonths(1);

            yearDays = paymentDay.lengthOfYear();
            percentageAmount = amount * rateInDigital * monthDays / yearDays;
            if (i == term) monthlyPayment = amount + percentageAmount;

            bodyCredit = monthlyPayment - percentageAmount;
            amount -= bodyCredit;

            paymentSchedule.add(new PaymentScheduleElement().builder()
                    .number(Integer.valueOf(i))
                    .date(paymentDay)
                    .totalPayment(BigDecimal.valueOf(monthlyPayment).setScale(2, ROUND_HALF_UP))
                    .interestPayment(BigDecimal.valueOf(percentageAmount).setScale(2, ROUND_HALF_UP))
                    .debtPayment(BigDecimal.valueOf(bodyCredit).setScale(2, ROUND_HALF_UP))
                    .remainingDebt(BigDecimal.valueOf(amount).setScale(2, ROUND_HALF_UP))
                    .build());
        }
        return paymentSchedule;
    }

}
