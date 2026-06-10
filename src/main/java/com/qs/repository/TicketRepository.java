package com.qs.repository;

import com.qs.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, String> {

    @Query("SELECT t FROM Ticket t JOIN FETCH t.archive ORDER BY t.createTime DESC")
    List<Ticket> findAllWithArchive();

    @Query("SELECT t FROM Ticket t JOIN FETCH t.archive WHERE t.id = :id")
    Optional<Ticket> findByIdWithArchive(String id);

    @Query("SELECT t FROM Ticket t JOIN FETCH t.archive WHERE t.archive.id = :archiveId ORDER BY t.createTime DESC")
    List<Ticket> findByArchiveId(@Param("archiveId") String archiveId);

    @Query("SELECT t FROM Ticket t JOIN FETCH t.archive WHERE t.status <> :completedStatus "
            + "AND t.expectedCompleteDate = :date")
    List<Ticket> findActiveByExpectedCompleteDate(@Param("completedStatus") String completedStatus,
                                                  @Param("date") LocalDate date);
}
