package com.founderlink.investmentservice.controller;

import com.founderlink.investmentservice.dto.InvestmentRequest;
import com.founderlink.investmentservice.dto.InvestmentResponse;
import com.founderlink.investmentservice.service.InvestmentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/investments")
@RequiredArgsConstructor
public class InvestmentController {

  private final InvestmentService service;

  @PostMapping
  @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
  public InvestmentResponse create(@Valid @RequestBody InvestmentRequest request) {
    return service.createInvestment(request);
  }

  @PutMapping("/{id}/approve")
  @PreAuthorize("hasRole('ADMIN')")
  public InvestmentResponse approve(@PathVariable Long id) {
    return service.approveInvestment(id);
  }

  @GetMapping("/investor/{id}")
  @PreAuthorize("isAuthenticated()")
  public List<InvestmentResponse> getByInvestor(@PathVariable Long id) {
    return service.getByInvestor(id);
  }

  @GetMapping("/startup/{id}")
  @PreAuthorize("isAuthenticated()")
  public List<InvestmentResponse> getByStartup(@PathVariable Long id) {
    return service.getByStartup(id);
  }
}
