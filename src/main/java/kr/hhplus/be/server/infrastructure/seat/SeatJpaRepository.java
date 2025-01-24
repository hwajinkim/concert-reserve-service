package kr.hhplus.be.server.infrastructure.seat;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.domain.concert.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeatJpaRepository extends JpaRepository<Seat, Long> {

    @Query("SELECT s.schedule.id FROM Seat s WHERE s.id = :seatId")
    Optional<Object> findScheduleIdBySeatId(@Param("seatId") Long seatId);

}
