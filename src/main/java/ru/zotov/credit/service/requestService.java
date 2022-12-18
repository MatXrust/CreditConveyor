package ru.zotov.credit.service;

import org.springframework.http.ResponseEntity;
import ru.zotov.credit.dto.LoanApplicationRequestDTO;
import ru.zotov.credit.dto.ScoringDataDTO;

public interface requestService {
     ResponseEntity<?> getOffers( LoanApplicationRequestDTO request);
     ResponseEntity<?> creditProcessing( ScoringDataDTO request);
}
