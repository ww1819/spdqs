package com.qs.service;

import com.qs.entity.Archive;
import com.qs.entity.Ticket;
import com.qs.entity.TicketFollowUp;
import com.qs.enums.TicketStatus;
import com.qs.repository.ReminderRepository;
import com.qs.repository.TicketRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ArchiveService archiveService;
    private final ReminderRepository reminderRepository;
    private final TicketFollowUpService followUpService;
    private final TicketAttachmentService attachmentService;
    private final JdbcTemplate jdbcTemplate;

    public TicketService(TicketRepository ticketRepository, ArchiveService archiveService,
                         ReminderRepository reminderRepository, TicketFollowUpService followUpService,
                         TicketAttachmentService attachmentService, JdbcTemplate jdbcTemplate) {
        this.ticketRepository = ticketRepository;
        this.archiveService = archiveService;
        this.reminderRepository = reminderRepository;
        this.followUpService = followUpService;
        this.attachmentService = attachmentService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Ticket> listAll() {
        return attachFollowUpFlags(ticketRepository.findAllWithArchive());
    }

    /** 工作台展示：有目标完成时间的靠前，再按目标日期升序 */
    public List<Ticket> listForDashboard() {
        return attachFollowUpFlags(ticketRepository.findAllWithArchive().stream()
                .filter(t -> TicketStatus.isActive(t.getStatus()))
                .sorted(dashboardTicketOrder())
                .toList());
    }

    public static Comparator<Ticket> dashboardTicketOrder() {
        return Comparator
                .comparing((Ticket t) -> t.getTargetCompleteDate() == null)
                .thenComparing(Ticket::getTargetCompleteDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Ticket::getCreateTime, Comparator.reverseOrder());
    }

    public List<Ticket> search(String status, String handler, String submitter, String keyword) {
        return search(status == null || status.isBlank() ? null : List.of(status),
                handler, submitter, keyword, null, null, null);
    }

    public List<Ticket> search(String status, String handler, String submitter, String keyword, String menu) {
        return search(status == null || status.isBlank() ? null : List.of(status),
                handler, submitter, keyword, menu, null, null);
    }

    public List<Ticket> search(List<String> statuses, String handler, String submitter, String keyword,
                               String menu, List<String> menuAliases, String archiveId) {
        String kw = normalize(keyword);
        List<String> aliases = menuAliases;
        if ((aliases == null || aliases.isEmpty()) && menu != null && !menu.isBlank()) {
            aliases = List.of(menu.trim());
        }
        List<String> finalAliases = aliases;
        List<String> statusList = normalizeStatusList(statuses);
        String projectId = archiveId == null || archiveId.isBlank() ? null : archiveId.trim();
        return attachFollowUpFlags(ticketRepository.findAllWithArchive().stream()
                .filter(t -> statusList.isEmpty() || matchesAnyStatus(t, statusList))
                .filter(t -> projectId == null
                        || (t.getArchive() != null && projectId.equals(t.getArchive().getId())))
                .filter(t -> handler == null || handler.isBlank()
                        || (t.getHandler() != null && t.getHandler().contains(handler)))
                .filter(t -> submitter == null || submitter.isBlank()
                        || (t.getSubmitter() != null && t.getSubmitter().contains(submitter)))
                .filter(t -> kw == null || matchesKeyword(t, kw))
                .filter(t -> finalAliases == null || finalAliases.isEmpty() || matchesMenuAliases(t, finalAliases))
                .toList());
    }

    private List<String> normalizeStatusList(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return statuses.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private boolean matchesAnyStatus(Ticket ticket, List<String> statuses) {
        for (String status : statuses) {
            if (matchesStatusFilter(ticket, status)) {
                return true;
            }
        }
        return false;
    }

    public List<Ticket> listByArchiveId(String archiveId) {
        return ticketRepository.findByArchiveId(archiveId);
    }

    public List<Ticket> findMyTodos(String currentUser) {
        if (currentUser == null || currentUser.isBlank()) {
            return List.of();
        }
        return ticketRepository.findAllWithArchive().stream()
                .filter(t -> TicketStatus.COMMUNICATING.getLabel().equals(t.getStatus())
                        || TicketStatus.PROCESSING.getLabel().equals(t.getStatus()))
                .filter(t -> currentUser.equals(t.getHandler()) || currentUser.equals(t.getSubmitter()))
                .toList();
    }

    public List<Ticket> findTodayFollowUps() {
        LocalDate today = LocalDate.now();
        return ticketRepository.findAllWithArchive().stream()
                .filter(t -> TicketStatus.isActive(t.getStatus()))
                .filter(t -> today.equals(t.getExpectedCompleteDate()))
                .toList();
    }

    public Ticket getById(String id) {
        return ticketRepository.findByIdWithArchive(id)
                .orElseThrow(() -> new IllegalArgumentException("工单不存在"));
    }

    public List<TicketFollowUp> listFollowUps(String ticketId) {
        return followUpService.listByTicketId(ticketId);
    }

    @Transactional
    public Ticket save(Ticket ticket, String archiveId, String newFollowUp, String createBy) {
        if (ticket.getId() != null && !ticket.getId().isBlank()) {
            Ticket existing = getById(ticket.getId());
            if (isExpectedDateChanged(existing.getExpectedCompleteDate(), ticket.getExpectedCompleteDate())
                    && (newFollowUp == null || newFollowUp.isBlank())) {
                throw new IllegalArgumentException("修改预计完成时间须填写跟进记录");
            }
        }
        Archive archive = archiveService.getById(archiveId);
        ticket.setArchive(archive);
        if (ticket.getStatus() == null || ticket.getStatus().isBlank()) {
            ticket.setStatus(TicketStatus.SUBMITTED.getLabel());
        }
        ticket.setProcessNote(null);
        if (ticket.getTicketNo() == null) {
            ticket.setTicketNo(allocateTicketNo());
        }
        Ticket saved = ticketRepository.save(ticket);
        followUpService.addFollowUp(saved, newFollowUp, createBy);
        return saved;
    }

    private boolean isExpectedDateChanged(LocalDate oldDate, LocalDate newDate) {
        return !Objects.equals(oldDate, newDate);
    }

    private Long allocateTicketNo() {
        jdbcTemplate.update(
                "UPDATE T_SYS_SEQ SET NEXT_VAL = LAST_INSERT_ID(NEXT_VAL + 1) WHERE NAME = 'TICKET_NO'");
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Transactional
    public void markPendingUpgrade(String id) {
        Ticket ticket = getById(id);
        ticket.setTargetCompleteDate(LocalDate.now());
        ticket.setStatus(TicketStatus.PENDING_UPGRADE.getLabel());
        ticketRepository.save(ticket);
    }

    @Transactional
    public void markUpgraded(String id, String upgradeBy) {
        Ticket ticket = getById(id);
        ticket.setStatus(TicketStatus.COMPLETED.getLabel());
        ticket.setUpgradeBy(upgradeBy);
        ticket.setUpgradeTime(LocalDateTime.now());
        ticketRepository.save(ticket);
    }

    @Transactional
    public void delete(String id) {
        try {
            attachmentService.deleteByTicketId(id);
        } catch (IOException e) {
            throw new IllegalStateException("删除工单附件失败", e);
        }
        followUpService.deleteByTicketId(id);
        reminderRepository.deleteByTicketId(id);
        ticketRepository.deleteById(id);
    }

    private List<Ticket> attachFollowUpFlags(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            return tickets;
        }
        Set<String> withFollowUp = followUpService.findTicketIdsWithFollowUp(
                tickets.stream().map(Ticket::getId).toList());
        tickets.forEach(t -> t.setHasFollowUpRecord(withFollowUp.contains(t.getId())));
        return tickets;
    }

    private boolean matchesStatusFilter(Ticket ticket, String status) {
        if ("进行中".equals(status)) {
            return TicketStatus.isActive(ticket.getStatus());
        }
        return status.equals(ticket.getStatus());
    }

    private boolean matchesKeyword(Ticket t, String kw) {
        if (containsIgnoreCase(t.getContent(), kw)) {
            return true;
        }
        if (containsIgnoreCase(t.getContactInfo(), kw)) {
            return true;
        }
        if (t.getArchive() != null && containsIgnoreCase(t.getArchive().getProjectName(), kw)) {
            return true;
        }
        return false;
    }

    /** 按功能菜单匹配：工单内容命中当前名或曾用名 */
    private boolean matchesMenuAliases(Ticket t, List<String> aliases) {
        if (t.getContent() == null || t.getContent().isBlank()) {
            return false;
        }
        String content = t.getContent().toLowerCase(Locale.ROOT);
        for (String alias : aliases) {
            if (alias != null && !alias.isBlank()
                    && content.contains(alias.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIgnoreCase(String text, String kw) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(kw);
    }

    private String normalize(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }
}
