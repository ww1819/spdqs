package com.qs.repository;

import com.qs.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, String> {

    @Query("SELECT t FROM Ticket t JOIN FETCH t.delivery ORDER BY t.createTime DESC")
    List<Ticket> findAllWithDelivery();

    @Query("SELECT t FROM Ticket t JOIN FETCH t.delivery WHERE t.id = :id")
    Optional<Ticket> findByIdWithDelivery(String id);

    @Query("SELECT t FROM Ticket t JOIN FETCH t.delivery WHERE t.delivery.id = :deliveryId ORDER BY t.createTime DESC")
    List<Ticket> findByDeliveryId(@Param("deliveryId") String deliveryId);

    @Query("SELECT t FROM Ticket t JOIN FETCH t.delivery WHERE t.status <> :completedStatus "
            + "AND t.expectedCompleteDate = :date")
    List<Ticket> findActiveByExpectedCompleteDate(@Param("completedStatus") String completedStatus,
                                                  @Param("date") LocalDate date);
}
