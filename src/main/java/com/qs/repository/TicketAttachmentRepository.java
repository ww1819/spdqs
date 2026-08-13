package com.qs.repository;

import com.qs.entity.TicketAttachment;
import com.qs.enums.AttachmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, String> {

    @Query("SELECT a FROM TicketAttachment a WHERE a.ticket.id = :ticketId ORDER BY a.createTime DESC")
    List<TicketAttachment> findByTicketIdOrderByCreateTimeDesc(@Param("ticketId") String ticketId);

    @Query("SELECT a FROM TicketAttachment a WHERE a.ticket.id = :ticketId AND a.attachmentType = :type ORDER BY a.createTime DESC")
    List<TicketAttachment> findByTicketIdAndType(@Param("ticketId") String ticketId,
                                                 @Param("type") AttachmentType type);

    @Query("SELECT DISTINCT a.ticket.id FROM TicketAttachment a WHERE a.ticket.id IN :ticketIds AND a.attachmentType = :type")
    List<String> findTicketIdsByType(@Param("ticketIds") Collection<String> ticketIds,
                                     @Param("type") AttachmentType type);

    @Query("SELECT DISTINCT a.ticket.id FROM TicketAttachment a WHERE a.ticket.id IN :ticketIds "
            + "AND a.attachmentType = :type AND a.confirmed = true")
    List<String> findTicketIdsByTypeAndConfirmed(@Param("ticketIds") Collection<String> ticketIds,
                                                 @Param("type") AttachmentType type);

    @Query("SELECT a FROM TicketAttachment a WHERE a.ticket.id IN :ticketIds AND a.attachmentType = :type "
            + "ORDER BY a.createTime DESC")
    List<TicketAttachment> findByTicketIdsAndType(@Param("ticketIds") Collection<String> ticketIds,
                                                  @Param("type") AttachmentType type);

    @Query("SELECT COUNT(a) FROM TicketAttachment a WHERE a.ticket.id = :ticketId AND a.attachmentType IN :types")
    long countByTicketIdAndTypes(@Param("ticketId") String ticketId, @Param("types") Collection<AttachmentType> types);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TicketAttachment a WHERE a.ticket.id = :ticketId")
    void deleteByTicketId(@Param("ticketId") String ticketId);

    @Query("SELECT a FROM TicketAttachment a JOIN FETCH a.ticket WHERE a.id = :id")
    Optional<TicketAttachment> findByIdWithTicket(@Param("id") String id);
}
