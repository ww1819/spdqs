package com.qs.repository;

import com.qs.entity.TicketProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketProcessRepository extends JpaRepository<TicketProcess, String> {

    @Query("SELECT p FROM TicketProcess p WHERE p.ticket.id = :ticketId ORDER BY p.createTime ASC")
    List<TicketProcess> findByTicketIdOrderByCreateTimeAsc(@Param("ticketId") String ticketId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TicketProcess p WHERE p.ticket.id = :ticketId")
    void deleteByTicketId(@Param("ticketId") String ticketId);
}
