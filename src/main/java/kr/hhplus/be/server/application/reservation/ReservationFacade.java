package kr.hhplus.be.server.application.reservation;

import kr.hhplus.be.server.application.dto.reservation.ReservationParam;
import kr.hhplus.be.server.application.dto.reservation.ReservationResult;
import kr.hhplus.be.server.domain.concert.ConcertService;
import kr.hhplus.be.server.domain.concert.Schedule;
import kr.hhplus.be.server.domain.reservation.Reservation;
import kr.hhplus.be.server.domain.reservation.ReservationService;
import kr.hhplus.be.server.domain.concert.Seat;
import kr.hhplus.be.server.domain.concert.SeatStatus;
import kr.hhplus.be.server.domain.reservation.ReservationState;
import kr.hhplus.be.server.interfaces.aop.DistributedLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReservationFacade {

    private final ConcertService concertService;
    private final ReservationService reservationService;

    @DistributedLock(key = "#reservationParam.scheduleId")
    public ReservationResult createSeatReservation(ReservationParam reservationParam) {
        //1. 좌석(seat) 상태 업데이트 (점유)
        Seat updatedSeat = concertService.updateSeatStatus(reservationParam.seatId(), SeatStatus.OCCUPIED);
        //2. 스케줄(schedule) 잔여 티켓 수 업데이트(-1)
        Schedule updatedSchedule = concertService.updateScheduleRemainingTicket(reservationParam.scheduleId(), -1);
        //3. 예약(reservation) 신청
        Reservation savedReservation = reservationService.creatSeatReservation(updatedSeat, reservationParam.userId());

        return new ReservationResult(savedReservation.getId(), updatedSchedule.getId(),
                savedReservation.getSeatId(), savedReservation.getUserId(), savedReservation.getReservationState(), savedReservation.getCreatedAt());
    }

    @Transactional
    public void checkReservationExpiration(){
        List<Reservation> reservations = reservationService.checkReservationExpiration();

        for (Reservation reservation : reservations) {
            // *좌석* 상태 'AVAILABLE'으로 변경
            Seat updatedSeat = concertService.updateSeatStatus(reservation.getSeatId(), SeatStatus.AVAILABLE);
            // *스케줄* 잔여 티켓 업데이트 +1
            Schedule updatedSchedule = concertService.updateScheduleRemainingTicket(updatedSeat.getSchedule().getId(), 1);
        }
    }
}
