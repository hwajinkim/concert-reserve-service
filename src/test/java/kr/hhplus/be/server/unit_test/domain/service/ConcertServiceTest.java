package kr.hhplus.be.server.unit_test.domain.service;

import kr.hhplus.be.server.common.exception.AvailableSeatNotFoundException;
import kr.hhplus.be.server.common.exception.ConcertScheduleNotFoundException;
import kr.hhplus.be.server.common.exception.ScheduleNotFoundException;
import kr.hhplus.be.server.common.exception.SeatNotFoundException;
import kr.hhplus.be.server.domain.concert.*;
import kr.hhplus.be.server.domain.concert.Seat;
import kr.hhplus.be.server.domain.concert.SeatRepository;
import kr.hhplus.be.server.domain.concert.SeatStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConcertServiceTest {

    @InjectMocks
    private ConcertService concertService;

    @Mock
    private ConcertRepository concertRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private SeatRepository seatRepository;


    @Test
    void 콘서트_ID가_NULL_이면_IllegalArgumentException_발생(){
        //given
        Long concertId = null;

        when(concertRepository.findByConcertWithSchedule(concertId)).thenThrow(new IllegalArgumentException("콘서트 ID 유효하지 않음."));

        //when & then
        Exception exception = assertThrows(IllegalArgumentException.class,
                ()-> concertService.findByConcertWithSchedule(concertId));

        assertEquals("콘서트 ID 유효하지 않음.", exception.getMessage());
    }

    @Test
    void 콘서트_ID로_조회_시_콘서트_스케줄_정보가_없으면_ConcertScheduleNotFoundException_발생(){
        //given
        Long concertId = 999L;

        when(concertRepository.findByConcertWithSchedule(concertId)).thenReturn(Optional.empty());

        //when & then
        Exception exception = assertThrows(ConcertScheduleNotFoundException.class,
                ()-> concertService.findByConcertWithSchedule(concertId));

        assertEquals("콘서트 스케줄 정보를 찾을 수 없습니다.", exception.getMessage());
    }

    @Test
    void 콘서트_ID로_조회_시_콘서트_스케줄_정보가_있으면_Concert_반환(){
        //given
        Long concertId = 12345L;

        Schedule mockScheduleFirst = Schedule.builder()
                .scheduleId(67890L)
                .concertDateTime(LocalDateTime.of(2025,1,15,20,0,0))
                .bookingStart(LocalDateTime.of(2025,1,1,10,0,0))
                .bookingEnd(LocalDateTime.of(2025,1,10,18,0,0))
                .remainingTicket(50)
                .build();

        Schedule mockScheduleSecond = Schedule.builder()
                .scheduleId(67891L)
                .concertDateTime(LocalDateTime.of(2025,1,20,22,0,0))
                .bookingStart(LocalDateTime.of(2025,1,5,10,0,0))
                .bookingEnd(LocalDateTime.of(2025,1,15,18,0,0))
                .remainingTicket(30)
                .build();
        List<Schedule> mockSchedules = new ArrayList<>();
        mockSchedules.add(mockScheduleFirst);
        mockSchedules.add(mockScheduleSecond);

        Concert mockConcert = Concert.builder()
                .concertId(concertId)
                .concertName("Awesome Concert")
                .schedules(mockSchedules)
                .build();

        when(concertRepository.findByConcertWithSchedule(concertId)).thenReturn(Optional.of(mockConcert));
        //when
        Concert findConcertSchedule = concertService.findByConcertWithSchedule(concertId);

        //then
        assertEquals(findConcertSchedule, mockConcert);
        verify(concertRepository,times(1)).findByConcertWithSchedule(concertId);

    }

    @Test
    void 좌석_예약_시_좌석ID에_해당하는_좌석이_없으면_SeatNotFoundException_발생(){
        //given
        Long seatId = 999L;
        //when & then
        Exception exception = assertThrows(SeatNotFoundException.class,
                ()-> concertService.updateSeatStatus(seatId, SeatStatus.OCCUPIED));

        assertEquals("좌석을 찾을 수 없습니다.", exception.getMessage());
    }

    @Test
    void 좌석_예약_시_좌석ID에_해당하는_좌석이_있으면_좌석상태_변경_후_Seat_반환(){
        //given
        Long seatId = 1L;

        Seat mockSeat = Seat.builder()
                .seatId(seatId)
                .seatNumber(1)
                .seatStatus(SeatStatus.AVAILABLE)
                .seatPrice(BigDecimal.valueOf(10000.00))
                .schedule(null)
                .build();

        Seat updatedMockSeat = Seat.builder()
                .seatId(seatId)
                .seatNumber(1)
                .seatStatus(SeatStatus.OCCUPIED)
                .seatPrice(BigDecimal.valueOf(10000.00))
                .schedule(null)
                .build();

        when(seatRepository.findById(seatId)).thenReturn(Optional.of(mockSeat));
        when(seatRepository.save(any(Seat.class))).thenReturn(updatedMockSeat);
        //when
        Seat updatedSeat = concertService.updateSeatStatus(seatId, SeatStatus.OCCUPIED);
        //then
        assertEquals(SeatStatus.OCCUPIED, updatedSeat.getSeatStatus());
        verify(seatRepository).save(any(Seat.class));
    }
    @Test
    void 스케줄ID로_조회_시_예약_가능한_스케줄_정보가_없으면_AvailableSeatNotFoundException_발생(){
        //given
        Long concertId = 1L;
        Long scheduleId = 999L;

        Schedule mockScheduleFirst = Schedule.builder()
                .scheduleId(67890L)
                .concertDateTime(LocalDateTime.of(2025,1,15,20,0,0))
                .bookingStart(LocalDateTime.of(2025,1,1,10,0,0))
                .bookingEnd(LocalDateTime.of(2025,1,10,18,0,0))
                .remainingTicket(50)
                .build();

        List<Schedule> mockSchedules = new ArrayList<>();
        mockSchedules.add(mockScheduleFirst);

        Concert mockConcert = Concert.builder()
                .concertId(concertId)
                .concertName("Awesome Concert")
                .schedules(mockSchedules)
                .build();

        when(concertRepository.findByConcertWithSchedule(concertId)).thenReturn(Optional.of(mockConcert));
        when(scheduleRepository.findScheduleWithAvailableSeat(scheduleId)).thenThrow(new AvailableSeatNotFoundException("예약 가능한 좌석 정보를 찾을 수 없습니다."));

        //when & then
        Exception exception = assertThrows(AvailableSeatNotFoundException.class,
                ()-> concertService.findByConcertWithScheduleWithSeat(concertId, scheduleId));

        assertEquals("예약 가능한 좌석 정보를 찾을 수 없습니다.", exception.getMessage());
    }

    @Test
    void 스케줄ID로_조회_시_예약_가능한_스케줄_정보가_있으면_반환(){
        //given
        Long concertId = 1L;
        Long scheduleId = 999L;

        Concert mockConcert = Concert.builder()
                .concertId(concertId)
                .concertName("Awesome Concert")
                .schedules(List.of(
                    Schedule.builder()
                        .scheduleId(67890L)
                        .concertDateTime(LocalDateTime.of(2025,1,15,20,0,0))
                        .bookingStart(LocalDateTime.of(2025,1,1,10,0,0))
                        .bookingEnd(LocalDateTime.of(2025,1,10,18,0,0))
                        .remainingTicket(50)
                        .build()
                ))
                .build();

        Schedule mockSchedule = Schedule.builder()
                .scheduleId(67891L)
                .concertDateTime(LocalDateTime.of(2025,1,20,22,0,0))
                .bookingStart(LocalDateTime.of(2025,1,5,10,0,0))
                .bookingEnd(LocalDateTime.of(2025,1,15,18,0,0))
                .remainingTicket(30)
                .seats(List.of(Seat.builder().seatId(1L).build()))
                .build();

        when(concertRepository.findByConcertWithSchedule(concertId)).thenReturn(Optional.of(mockConcert));
        when(scheduleRepository.findScheduleWithAvailableSeat(scheduleId)).thenReturn(Optional.of(mockSchedule));
        //when
        Schedule schedule = concertService.findByConcertWithScheduleWithSeat(concertId, scheduleId);
        //then
        assertEquals(schedule.getId(), mockSchedule.getId());
        assertEquals(schedule.getSeats().get(0).getId(), mockSchedule.getSeats().get(0).getId());
    }

    @Test
    void 스케줄ID로_조회_시_스케줄_정보_없으면_ScheduleNotFoundException_발생(){
        //given
        Long scheduleId = 999L;
        int increseOrDecreseNumber = -1;

        when(scheduleRepository.findById(scheduleId)).thenThrow(new ScheduleNotFoundException("스케줄 정보를 찾을 수 없습니다."));
        //when
        Exception exception = assertThrows(ScheduleNotFoundException.class,
                ()-> concertService.updateScheduleRemainingTicket(scheduleId, increseOrDecreseNumber));
        //then
        assertEquals("스케줄 정보를 찾을 수 없습니다.", exception.getMessage());
    }

    @Test
    void 스케줄ID로_조회_시_정보_있으면_잔여_티켓수_1_증가_후_저장(){
        //given
        Long scheduleId = 1L;
        int increseOrDecreseNumber = -1;

        Schedule mockSchedule = Schedule.builder()
                .scheduleId(67891L)
                .concertDateTime(LocalDateTime.of(2025,1,20,22,0,0))
                .bookingStart(LocalDateTime.of(2025,1,5,10,0,0))
                .bookingEnd(LocalDateTime.of(2025,1,15,18,0,0))
                .remainingTicket(30)
                .build();

        Schedule updatedMockSchedule = Schedule.builder()
                .scheduleId(67891L)
                .concertDateTime(LocalDateTime.of(2025,1,20,22,0,0))
                .bookingStart(LocalDateTime.of(2025,1,5,10,0,0))
                .bookingEnd(LocalDateTime.of(2025,1,15,18,0,0))
                .remainingTicket(mockSchedule.getRemainingTicket() + increseOrDecreseNumber)
                .build();


        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(mockSchedule));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(updatedMockSchedule);
        //when
        Schedule updatedSchedule = concertService.updateScheduleRemainingTicket(scheduleId, increseOrDecreseNumber);
        //then
        assertEquals(29, updatedSchedule.getRemainingTicket());
        verify(scheduleRepository, times(1)).save(any(Schedule.class));
    }
    @Test
    void 결제_시_임시_예약_만료됐을_때_스케줄ID로_조회_시_정보_있으면_잔여_티켓수_1_증가_후_저장(){
        //given
        Long scheduleId = 1L;
        int increseOrDecreseNumber = 1;

        Schedule mockSchedule = Schedule.builder()
                .scheduleId(67891L)
                .concertDateTime(LocalDateTime.of(2025,1,20,22,0,0))
                .bookingStart(LocalDateTime.of(2025,1,5,10,0,0))
                .bookingEnd(LocalDateTime.of(2025,1,15,18,0,0))
                .remainingTicket(30)
                .build();

        Schedule updatedMockSchedule = Schedule.builder()
                .scheduleId(67891L)
                .concertDateTime(LocalDateTime.of(2025,1,20,22,0,0))
                .bookingStart(LocalDateTime.of(2025,1,5,10,0,0))
                .bookingEnd(LocalDateTime.of(2025,1,15,18,0,0))
                .remainingTicket(mockSchedule.getRemainingTicket() + increseOrDecreseNumber)
                .build();


        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(mockSchedule));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(updatedMockSchedule);
        //when
        Schedule updatedSchedule = concertService.updateScheduleRemainingTicket(scheduleId, increseOrDecreseNumber);
        //then
        assertEquals(31, updatedSchedule.getRemainingTicket());
        verify(scheduleRepository, times(1)).save(any(Schedule.class));
    }
}
