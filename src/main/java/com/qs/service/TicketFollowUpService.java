package com.qs.service;

import com.qs.entity.Ticket;
import com.qs.entity.TicketFollowUp;
import com.qs.repository.TicketFollowUpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TicketFollowUpService {

    private final TicketFollowUpRepository followUpRepository;

    public TicketFollowUpService(TicketFollowUpRepository followUpRepository) {
        this.followUpRepository = followUpRepository;
    }

    public List<TicketFollowUp> listByTicketId(String ticketId) {
        return followUpRepository.findByTicketIdOrderByCreateTimeDesc(ticketId);
    }

    public boolean hasFollowUp(String ticketId) {
        return followUpRepository.countByTicketId(ticketId) > 0;
    }

    public Set<String> findTicketIdsWithFollowUp(Collection<String> ticketIds) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(followUpRepository.findTicketIdsByTicketIdIn(ticketIds));
    }

    @Transactional
    public void addFollowUp(Ticket ticket, String content, String createBy) {
        if (content == null || content.isBlank()) {
            return;
        }
        TicketFollowUp followUp = new TicketFollowUp();
        followUp.setTicket(ticket);
        followUp.setContent(content.trim());
        followUp.setCreateBy(createBy);
        followUpRepository.save(followUp);
    }

    @Transactional
    public void deleteByTicketId(String ticketId) {
        followUpRepository.deleteByTicketId(ticketId);
    }
}
