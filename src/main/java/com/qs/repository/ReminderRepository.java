package com.qs.repository;

import com.qs.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, String> {

    @Query("SELECT COUNT(r) FROM Reminder r WHERE r.ticket.id = :ticketId AND r.targetUser = :targetUser "
            + "AND r.remindDate = :remindDate AND r.remindHour = :remindHour")
    long countExisting(@Param("ticketId") String ticketId,
                       @Param("targetUser") String targetUser,
                       @Param("remindDate") LocalDate remindDate,
                       @Param("remindHour") int remindHour);

    @Query("SELECT r FROM Reminder r JOIN FETCH r.ticket t JOIN FETCH t.archive "
            + "WHERE r.targetUser = :targetUser AND r.isRead = '0' ORDER BY r.createTime DESC")
    List<Reminder> findUnreadByTargetUser(@Param("targetUser") String targetUser);

    @Modifying
    @Query("UPDATE Reminder r SET r.isRead = '1' WHERE r.id = :id")
    void markRead(@Param("id") String id);

    @Modifying
    @Query("DELETE FROM Reminder r WHERE r.ticket.id = :ticketId")
    void deleteByTicketId(@Param("ticketId") String ticketId);
}
