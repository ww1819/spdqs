package com.qs.repository;

import com.qs.entity.TicketChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketChangeLogRepository extends JpaRepository<TicketChangeLog, String> {

    @Query("SELECT c FROM TicketChangeLog c WHERE c.ticketId = :ticketId ORDER BY c.changeTime DESC, c.fieldName ASC")
    List<TicketChangeLog> findByTicketIdOrderByChangeTimeDesc(@Param("ticketId") String ticketId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TicketChangeLog c WHERE c.ticketId = :ticketId")
    void deleteByTicketId(@Param("ticketId") String ticketId);
}
