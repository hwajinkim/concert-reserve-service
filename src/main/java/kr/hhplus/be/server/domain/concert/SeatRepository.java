package kr.hhplus.be.server.domain.concert;

import kr.hhplus.be.server.domain.concert.Seat;

import java.util.Optional;

public interface SeatRepository {

    Optional<Seat> findById(Long seatId);


    Seat save(Seat updatedSeat);

    Optional<Object> findScheduleIdBySeatId(Long seatId);
}
