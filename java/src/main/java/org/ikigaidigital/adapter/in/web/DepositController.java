package org.ikigaidigital.adapter.in.web;

import org.ikigaidigital.application.DepositService;
import org.ikigaidigital.application.DepositView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/time-deposits")
public class DepositController {
    private final DepositService service;

    public DepositController(DepositService service) {
        this.service = service;
    }

    @GetMapping
    public List<DepositView> findAll() {
        return service.findAll();
    }

    @PostMapping("/update-balances")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateBalances() {
        service.updateBalances();
    }
}
