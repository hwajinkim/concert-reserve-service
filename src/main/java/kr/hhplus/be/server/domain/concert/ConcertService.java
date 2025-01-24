package kr.hhplus.be.server.domain.concert;

import kr.hhplus.be.server.common.exception.AvailableSeatNotFoundException;
import kr.hhplus.be.server.common.exception.ConcertScheduleNotFoundException;
import kr.hhplus.be.server.common.exception.ScheduleNotFoundException;
import kr.hhplus.be.server.common.exception.SeatNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;
    private final ScheduleRepository scheduleRepository;
    private final SeatRepository seatRepository;

    public Concert findByConcertWithSchedule(Long concertId) {
        return concertRepository.findByConcertWithSchedule(concertId)
                .orElseThrow(()-> new ConcertScheduleNotFoundException("콘서트 스케줄 정보를 찾을 수 없습니다."));
    }

    public Schedule findByConcertWithScheduleWithSeat(Long concertId, Long scheduleId){
        Concert concert = concertRepository.findByConcertWithSchedule(concertId)
                .orElseThrow(()-> new ConcertScheduleNotFoundException("콘서트 스케줄 정보를 찾을 수 없습니다."));

        Schedule schedule = scheduleRepository.findScheduleWithAvailableSeat(scheduleId)
                .orElseThrow(()-> new AvailableSeatNotFoundException("예약 가능한 좌석 정보를 찾을 수 없습니다."));

        return schedule;
    }

    public Seat updateSeatStatus(Long seatId, SeatStatus seatStatus) {
        //1. 좌석 정보 조회
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(()-> new SeatNotFoundException("좌석을 찾을 수 없습니다."));
        //2. 좌석 정보 업데이트
        Seat updatedSeat = seat.update(seat, seatStatus);
        //3. 좌석 정보 저장
        return seatRepository.save(updatedSeat);
    }

    public Schedule updateScheduleRemainingTicket(Long scheduleId, int increaseOrDecreaseNumber) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(()-> new ScheduleNotFoundException("스케줄 정보를 찾을 수 없습니다."));

        Schedule updatedSchedule = schedule.update(schedule, increaseOrDecreaseNumber);
        return scheduleRepository.save(updatedSchedule);
    }


    @Transactional
    public Concert saveConcertWithSchedules(String concertName, List<Schedule> schedules) {
        // Concert 객체 생성
        Concert concert = Concert.builder()
                .concertId(null) // ID는 자동 생성
                .concertName(concertName)
                .schedules(new ArrayList<>()) // 빈 리스트 초기화
                .build();

        // 편의 메서드를 사용하여 관계 설정
        for (Schedule schedule : schedules) {
            concert.addSchedule(schedule);
        }

        // Concert와 연관된 Schedule 모두 저장
        return concertRepository.save(concert);
    }

    public Schedule findById(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(()-> new ScheduleNotFoundException("스케줄 정보를 찾을 수 없습니다."));
    }
}
