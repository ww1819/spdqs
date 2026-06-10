package com.qs.repository;

import com.qs.entity.TicketFollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TicketFollowUpRepository extends JpaRepository<TicketFollowUp, String> {

    @Query("SELECT f FROM TicketFollowUp f WHERE f.ticket.id = :ticketId ORDER BY f.createTime DESC")
    List<TicketFollowUp> findByTicketIdOrderByCreateTimeDesc(@Param("ticketId") String ticketId);

    @Query("SELECT COUNT(f) FROM TicketFollowUp f WHERE f.ticket.id = :ticketId")
    long countByTicketId(@Param("ticketId") String ticketId);

    @Query("SELECT DISTINCT f.ticket.id FROM TicketFollowUp f WHERE f.ticket.id IN :ticketIds")
    List<String> findTicketIdsByTicketIdIn(@Param("ticketIds") Collection<String> ticketIds);

    @Modifying
    @Query("DELETE FROM TicketFollowUp f WHERE f.ticket.id = :ticketId")
    void deleteByTicketId(@Param("ticketId") String ticketId);
}
