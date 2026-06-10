package com.qs.repository;

import com.qs.entity.TicketAttachment;
import com.qs.enums.AttachmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, String> {

    @Query("SELECT a FROM TicketAttachment a WHERE a.ticket.id = :ticketId ORDER BY a.createTime DESC")
    List<TicketAttachment> findByTicketIdOrderByCreateTimeDesc(@Param("ticketId") String ticketId);

    @Query("SELECT a FROM TicketAttachment a WHERE a.ticket.id = :ticketId AND a.attachmentType = :type ORDER BY a.createTime DESC")
    List<TicketAttachment> findByTicketIdAndType(@Param("ticketId") String ticketId,
                                                 @Param("type") AttachmentType type);

    @Modifying
    @Query("DELETE FROM TicketAttachment a WHERE a.ticket.id = :ticketId")
    void deleteByTicketId(@Param("ticketId") String ticketId);

    @Query("SELECT a FROM TicketAttachment a JOIN FETCH a.ticket WHERE a.id = :id")
    Optional<TicketAttachment> findByIdWithTicket(@Param("id") String id);
}
