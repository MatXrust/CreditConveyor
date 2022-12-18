package ru.zotov.credit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.zotov.credit.dto.LoanApplicationRequestDTO;
import ru.zotov.credit.dto.ScoringDataDTO;
import ru.zotov.credit.service.requestService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/conveyor")
public class requestController {

    @Autowired
    private final requestService requestService;

    @PostMapping("/offers")
    public ResponseEntity<?> getOffers(LoanApplicationRequestDTO request){
        return ResponseEntity.ok(requestService.getOffers(request));
    }

    @PostMapping("/calculation")
    public ResponseEntity<?> creditProcessing(ScoringDataDTO request){
        return ResponseEntity.ok(requestService.creditProcessing(request));
    }
}
