package com.founderlink.startupservice.controller;

import com.founderlink.startupservice.dto.StartupDto;
import com.founderlink.startupservice.dto.StartupResponse;
import com.founderlink.startupservice.service.StartupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/startups")
@RequiredArgsConstructor
public class StartupController {

  private final StartupService startupService;

  @PostMapping
  @PreAuthorize("hasRole('FOUNDER')")
  public ResponseEntity<StartupResponse> create(@RequestBody StartupDto dto) {
    return ResponseEntity.status(201).body(startupService.createStartup(dto));
  }

  @GetMapping("/industry/{industry}")
  public Page<StartupResponse> byIndustry(
      @PathVariable String industry,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {
    return startupService.getStartupsByIndustry(industry, page, size);
  }

  @GetMapping("/{id}")
  public StartupResponse getById(@PathVariable Long id) {
    return startupService.getStartupById(id);
  }

  @GetMapping
  public Page<StartupResponse> getAll(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
    return startupService.getAllStartups(page, size);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('FOUNDER') or hasRole('ADMIN')")
  public StartupResponse update(@PathVariable Long id, @RequestBody StartupDto dto) {
    return startupService.updateStartup(id, dto);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('FOUNDER')")
  public void delete(@PathVariable Long id) {
    startupService.deleteStartup(id);
  }
}
