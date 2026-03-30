package com.founderlink.analyticsservice.service;

import com.founderlink.analyticsservice.entity.FundingRecord;
import com.founderlink.analyticsservice.entity.StartupSnapshot;
import com.founderlink.analyticsservice.entity.StartupTeamMember;
import com.founderlink.analyticsservice.repository.FundingRecordRepository;
import com.founderlink.analyticsservice.repository.StartupSnapshotRepository;
import com.founderlink.analyticsservice.repository.StartupTeamMemberRepository;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEventConsumer {

  private final FundingRecordRepository fundingRecordRepository;
  private final StartupSnapshotRepository snapshotRepository;
  private final StartupTeamMemberRepository teamMemberRepository;

  @Transactional
  @RabbitListener(queues = "analytics.queue")
  public void onDomainEvent(
      Map<String, Object> event, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
    log.info(
        "Analytics event received routingKey={} keys={}",
        routingKey,
        event != null ? event.keySet() : null);
    if (event == null || routingKey == null) {
      log.warn("Analytics skip: null event or routingKey");
      return;
    }
    try {
      switch (routingKey) {
        case "startup.created", "startup.approved" -> handleStartupEvent(routingKey, event);
        case "investment.created" -> handleInvestmentCreated(event);
        case "investment.approved" -> handleInvestmentApproved(event);
        case "team.invite.sent" -> handleTeamInviteSent(event);
        default -> log.warn("Analytics ignored unknown routingKey={}", routingKey);
      }
      log.debug("Analytics processed routingKey={}", routingKey);
    } catch (Exception ex) {
      log.error("Analytics failed routingKey={} error={}", routingKey, ex.toString(), ex);
      throw ex;
    }
  }

  private void handleStartupEvent(String routingKey, Map<String, Object> event) {
    Long startupId = toLong(event.get("startupId"));
    if (startupId == null) {
      log.warn("Analytics {} missing startupId", routingKey);
      return;
    }
    StartupSnapshot snap = snapshotRepository.findById(startupId).orElseGet(StartupSnapshot::new);
    snap.setStartupId(startupId);
    snap.setFounderId(toLong(event.get("founderId")));
    snap.setFundingGoal(toDouble(event.get("fundingGoal")));
    Object industry = event.get("industry");
    if (industry != null) {
      snap.setIndustry(industry.toString());
    }
    snap.setUpdatedAt(Instant.now());
    snapshotRepository.save(snap);
    log.info(
        "Analytics startup snapshot upserted routingKey={} startupId={} fundingGoal={}",
        routingKey,
        startupId,
        snap.getFundingGoal());
  }

  private void handleInvestmentCreated(Map<String, Object> event) {
    Long investmentId = toLong(event.get("investmentId"));
    if (investmentId != null && fundingRecordRepository.existsByInvestmentId(investmentId)) {
      log.info(
          "Analytics idempotent skip investment.created investmentId={} (already processed)",
          investmentId);
      return;
    }
    Long startupId = toLong(event.get("startupId"));
    Long investorId = toLong(event.get("investorId"));
    Double amount = toDouble(event.get("amount"));
    if (startupId == null || investorId == null) {
      log.warn("Analytics investment.created missing startupId or investorId");
      return;
    }
    var existingPair = fundingRecordRepository.findByStartupIdAndInvestorId(startupId, investorId);
    if (existingPair.isPresent()) {
      FundingRecord row = existingPair.get();
      if (investmentId != null && investmentId.equals(row.getInvestmentId())) {
        log.info("Analytics idempotent skip same pair investmentId={}", investmentId);
        return;
      }
      row.setAmount(amount != null ? amount : row.getAmount());
      row.setStatus("PENDING");
      if (investmentId != null) {
        row.setInvestmentId(investmentId);
      }
      fundingRecordRepository.save(row);
      log.info(
          "Analytics funding record updated startupId={} investorId={} investmentId={}",
          startupId,
          investorId,
          investmentId);
      return;
    }
    FundingRecord record = new FundingRecord();
    record.setInvestmentId(investmentId);
    record.setStartupId(startupId);
    record.setInvestorId(investorId);
    record.setAmount(amount);
    record.setStatus("PENDING");
    try {
      fundingRecordRepository.save(record);
      log.info(
          "Analytics funding record created startupId={} investorId={} investmentId={} amount={}",
          startupId,
          investorId,
          investmentId,
          amount);
    } catch (DataIntegrityViolationException ex) {
      log.warn(
          "Analytics investment.created deduped at DB (startupId={} investorId={}): {}",
          startupId,
          investorId,
          ex.getMostSpecificCause().getMessage());
    }
  }

  private void handleInvestmentApproved(Map<String, Object> event) {
    Long investmentId = toLong(event.get("investmentId"));
    Long startupId = toLong(event.get("startupId"));
    Long investorId = toLong(event.get("investorId"));
    FundingRecord row = null;
    if (investmentId != null) {
      row = fundingRecordRepository.findByInvestmentId(investmentId).orElse(null);
    }
    if (row == null && startupId != null && investorId != null) {
      row =
          fundingRecordRepository.findByStartupIdAndInvestorId(startupId, investorId).orElse(null);
    }
    if (row == null) {
      log.warn(
          "Analytics investment.approved no funding record investmentId={} startupId={} investorId={}",
          investmentId,
          startupId,
          investorId);
      return;
    }
    row.setStatus("APPROVED");
    Double amount = toDouble(event.get("amount"));
    if (amount != null) {
      row.setAmount(amount);
    }
    if (investmentId != null && row.getInvestmentId() == null) {
      row.setInvestmentId(investmentId);
    }
    fundingRecordRepository.save(row);
    log.info(
        "Analytics funding record approved investmentId={} startupId={} investorId={}",
        row.getInvestmentId(),
        row.getStartupId(),
        row.getInvestorId());
  }

  private void handleTeamInviteSent(Map<String, Object> event) {
    Long startupId = toLong(event.get("startupId"));
    Long invitedUserId = toLong(event.get("invitedUserId"));
    if (startupId == null || invitedUserId == null) {
      log.warn("Analytics team.invite.sent missing startupId or invitedUserId");
      return;
    }
    if (teamMemberRepository.existsByStartupIdAndUserId(startupId, invitedUserId)) {
      log.info(
          "Analytics idempotent skip team.invite.sent startupId={} userId={}",
          startupId,
          invitedUserId);
      return;
    }
    StartupTeamMember m = new StartupTeamMember();
    m.setStartupId(startupId);
    m.setUserId(invitedUserId);
    try {
      teamMemberRepository.save(m);
      log.info("Analytics team member tracked startupId={} userId={}", startupId, invitedUserId);
    } catch (DataIntegrityViolationException ex) {
      log.warn(
          "Analytics team invite deduped at DB startupId={} userId={}", startupId, invitedUserId);
    }
  }

  private static Long toLong(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number n) {
      return n.longValue();
    }
    return Long.valueOf(value.toString());
  }

  private static Double toDouble(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number n) {
      return n.doubleValue();
    }
    return Double.valueOf(value.toString());
  }
}
