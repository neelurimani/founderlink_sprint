package com.founderlink.startupservice.service;

import com.founderlink.startupservice.dto.StartupDto;
import com.founderlink.startupservice.dto.StartupResponse;
import org.springframework.data.domain.Page;

public interface StartupService {

  StartupResponse createStartup(StartupDto dto);

  StartupResponse getStartupById(Long id);

  Page<StartupResponse> getAllStartups(int page, int size);

  Page<StartupResponse> getStartupsByIndustry(String industry, int page, int size);

  StartupResponse updateStartup(Long id, StartupDto dto);

  void deleteStartup(Long id);
}
