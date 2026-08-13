package com.qs.service;

import com.qs.entity.Ticket;
import com.qs.entity.TicketProcess;
import com.qs.enums.TicketProcessAction;
import com.qs.enums.TicketStatus;
import com.qs.repository.TicketProcessRepository;
import com.qs.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TicketProcessService {

    private final TicketProcessRepository processRepository;
    private final TicketRepository ticketRepository;

    public TicketProcessService(TicketProcessRepository processRepository, TicketRepository ticketRepository) {
        this.processRepository = processRepository;
        this.ticketRepository = ticketRepository;
    }

    public List<TicketProcess> listTreeByTicketId(String ticketId) {
        List<TicketProcess> flat = processRepository.findByTicketIdOrderByCreateTimeAsc(ticketId);
        Map<String, TicketProcess> roots = new LinkedHashMap<>();
        List<TicketProcess> orphans = new ArrayList<>();
        for (TicketProcess p : flat) {
            p.setReplies(new ArrayList<>());
            if (p.getParentId() == null || p.getParentId().isBlank()) {
                roots.put(p.getId(), p);
            } else {
                orphans.add(p);
            }
        }
        for (TicketProcess reply : orphans) {
            TicketProcess parent = roots.get(reply.getParentId());
            if (parent != null) {
                parent.getReplies().add(reply);
            } else {
                roots.put(reply.getId(), reply);
            }
        }
        List<TicketProcess> result = new ArrayList<>(roots.values());
        result.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        return result;
    }

    @Transactional
    public void markHandled(String ticketId, String handleMethod, String content, String createBy) {
        Ticket ticket = requireTicket(ticketId);
        if (TicketStatus.COMPLETED.getLabel().equals(ticket.getStatus())) {
            throw new IllegalArgumentException("已完成的工单不能再标记已处理");
        }
        String method = requireText(handleMethod, "请填写处理方式");
        String note = requireText(content, "请填写处理说明");
        TicketProcess process = newProcess(ticket, null, TicketProcessAction.HANDLED, method, note, createBy);
        processRepository.save(process);
        ticket.setStatus(TicketStatus.HANDLED.getLabel());
        if (ticket.getHandler() == null || ticket.getHandler().isBlank()) {
            ticket.setHandler(createBy);
        }
        ticketRepository.save(ticket);
    }

    @Transactional
    public void reply(String ticketId, String parentId, String content, String createBy) {
        Ticket ticket = requireTicket(ticketId);
        if (TicketStatus.COMPLETED.getLabel().equals(ticket.getStatus())) {
            throw new IllegalArgumentException("已完成的工单不能再回复");
        }
        String note = requireText(content, "请填写核对说明");
        String resolvedParent = parentId;
        if (resolvedParent == null || resolvedParent.isBlank()) {
            resolvedParent = findLatestHandledId(ticketId);
        } else {
            TicketProcess parent = processRepository.findById(resolvedParent)
                    .orElseThrow(() -> new IllegalArgumentException("处理记录不存在"));
            if (!ticketId.equals(parent.getTicket().getId())) {
                throw new IllegalArgumentException("处理记录不属于当前工单");
            }
        }
        processRepository.save(newProcess(ticket, resolvedParent, TicketProcessAction.REPLY, null, note, createBy));
    }

    @Transactional
    public void markNeedFeedback(String ticketId, String content, String createBy) {
        Ticket ticket = requireTicket(ticketId);
        if (!TicketStatus.canConfirmHandled(ticket.getStatus())) {
            throw new IllegalArgumentException("仅「已处理」状态可标记为待反馈调整");
        }
        String note = content != null && !content.isBlank() ? content.trim() : "现场核对后需按反馈继续调整";
        String parentId = findLatestHandledId(ticketId);
        processRepository.save(newProcess(ticket, parentId, TicketProcessAction.NEED_FEEDBACK, null, note, createBy));
        ticket.setStatus(TicketStatus.NEED_FEEDBACK.getLabel());
        ticketRepository.save(ticket);
    }

    @Transactional
    public void markCompletedWithProcess(String ticketId, String content, String createBy) {
        Ticket ticket = requireTicket(ticketId);
        if (TicketStatus.COMPLETED.getLabel().equals(ticket.getStatus())) {
            return;
        }
        String note = content != null && !content.isBlank() ? content.trim() : "核对通过，工单已完成";
        String parentId = findLatestHandledId(ticketId);
        processRepository.save(newProcess(ticket, parentId, TicketProcessAction.COMPLETE, null, note, createBy));
        ticket.setStatus(TicketStatus.COMPLETED.getLabel());
        ticketRepository.save(ticket);
    }

    public boolean hasAny(String ticketId) {
        return processRepository.countByTicketId(ticketId) > 0;
    }

    public Set<String> findTicketIdsWithAnyProcess(Collection<String> ticketIds) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(processRepository.findTicketIdsHavingProcess(ticketIds));
    }

    @Transactional
    public void deleteByTicketId(String ticketId) {
        processRepository.deleteByTicketId(ticketId);
    }

    private String findLatestHandledId(String ticketId) {
        return processRepository.findByTicketIdOrderByCreateTimeAsc(ticketId).stream()
                .filter(p -> p.getActionType() == TicketProcessAction.HANDLED)
                .reduce((a, b) -> b)
                .map(TicketProcess::getId)
                .orElse(null);
    }

    private TicketProcess newProcess(Ticket ticket, String parentId, TicketProcessAction action,
                                     String handleMethod, String content, String createBy) {
        TicketProcess process = new TicketProcess();
        process.setTicket(ticket);
        process.setParentId(parentId);
        process.setActionType(action);
        process.setHandleMethod(handleMethod);
        process.setContent(content);
        process.setCreateBy(createBy);
        return process;
    }

    private Ticket requireTicket(String ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("工单不存在"));
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
