package kr.hhplus.be.server.integration_test.application;

import kr.hhplus.be.server.application.dto.reservation.ReservationParam;
import kr.hhplus.be.server.application.dto.reservation.ReservationResult;
import kr.hhplus.be.server.application.reservation.ReservationFacade;
import kr.hhplus.be.server.domain.concert.Concert;
import kr.hhplus.be.server.domain.concert.Schedule;
import kr.hhplus.be.server.domain.concert.Seat;
import kr.hhplus.be.server.domain.concert.SeatStatus;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.integration_test.application.set_up.ConcertSetUp;
import kr.hhplus.be.server.integration_test.application.set_up.ReservationSetUp;
import kr.hhplus.be.server.integration_test.application.set_up.ScheduleSetUp;
import kr.hhplus.be.server.integration_test.application.set_up.UserSetUp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReservationConcurrencyIntegrationTest extends BaseIntegrationTest{
    @Autowired
    private ReservationFacade reservationFacade;
    @Autowired
    private UserSetUp userSetUp;
    @Autowired
    private ConcertSetUp concertSetUp;
    @Autowired
    private ScheduleSetUp scheduleSetUp;
    @Autowired
    private ReservationSetUp reservationSetUp;
    private List<Schedule> scheduleList;
    private List<Seat> seatList;

    private List<User> users = new ArrayList<>();
    @BeforeEach
    void setup() {
        for (int i = 0; i < 100; i++) {
            users.add(userSetUp.saveUser("user" + i, BigDecimal.valueOf(50000.00))); // 사용자 미리 생성
        }
        scheduleList = List.of(
                Schedule.builder()
                        .price(BigDecimal.valueOf(10000.00))
                        .concertDateTime(LocalDateTime.of(2025,1,15,20,0,0))
                        .bookingStart(LocalDateTime.of(2025,1,1, 10,0,0))
                        .bookingEnd(LocalDateTime.of(2025,1,10,18,0,0))
                        .remainingTicket(50)
                        .build(),
                Schedule.builder()
                        .price(BigDecimal.valueOf(15000.00))
                        .concertDateTime(LocalDateTime.of(2025,1,20,18,0,0))
                        .bookingStart(LocalDateTime.of(2025,1,5, 10,0,0))
                        .bookingEnd(LocalDateTime.of(2025,1,14,18,0,0))
                        .remainingTicket(30)
                        .build()
        );

        seatList = List.of(
                Seat.builder()
                        .seatNumber(1)
                        .seatStatus(SeatStatus.AVAILABLE)
                        .seatPrice(BigDecimal.valueOf(10000.00))
                        .build(),
                Seat.builder()
                        .seatNumber(2)
                        .seatStatus(SeatStatus.OCCUPIED)
                        .seatPrice(BigDecimal.valueOf(15000.00))
                        .build()
        );
    }

    @Test
    void 동일한_유저가_5번_예약을_신청하면_1번만_성공() throws InterruptedException {
        //given
        Concert concert = concertSetUp.saveConcert("Awesome Concert", scheduleList);
        Schedule schedule = scheduleSetUp.saveSchedule(
                BigDecimal.valueOf(50000.00),
                LocalDateTime.of(2025,1,15,20,0,0),
                LocalDateTime.of(2025,1,1, 10,0,0),
                LocalDateTime.of(2025,1,10,18,0,0),
                50, 100, concert, seatList);

        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        AtomicInteger successfulRequests = new AtomicInteger();
        AtomicInteger failedRequests = new AtomicInteger();
        //when
        for(int i = 0; i < threadCount; i++) {
            User user = users.get(0);
            ReservationParam reservationParam = new ReservationParam(
                    schedule.getId(), schedule.getSeats().get(0).getId(), user.getId()
            );
            executorService.submit(() -> {
                try {
                    ReservationResult reservationResult = reservationFacade.createSeatReservation(reservationParam);
                    successfulRequests.incrementAndGet();
                } catch (Exception e) {
                    failedRequests.incrementAndGet();
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);

        //then
        assertEquals(1, successfulRequests.get(), "성공한 요청은 1개여야 합니다.");
        assertEquals(4, failedRequests.get(), "실패한 요청은 4개여야 합니다.");
    }

    @Test
    void 분산락_사용하여_다른_유저_100명이_같은_좌석에_대해_예약해도_1건만_예약() throws InterruptedException {
        //given
        Concert concert = concertSetUp.saveConcert("Awesome Concert", scheduleList);
        Schedule schedule = scheduleSetUp.saveSchedule(
                BigDecimal.valueOf(50000.00),
                LocalDateTime.of(2025,1,15,20,0,0),
                LocalDateTime.of(2025,1,1, 10,0,0),
                LocalDateTime.of(2025,1,10,18,0,0),
                50, 100, concert, seatList);

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        AtomicInteger successfulRequests = new AtomicInteger();
        AtomicInteger failedRequests = new AtomicInteger();

        //when
        for(int i = 0; i < threadCount; i++){
            User user = users.get(i);

            ReservationParam reservationParam = new ReservationParam(
                    schedule.getId(), schedule.getSeats().get(0).getId(), user.getId()
            );

            executorService.submit(() -> {
                try {
                    ReservationResult reservationResult = reservationFacade.createSeatReservation(reservationParam);
                    successfulRequests.incrementAndGet();
                } catch (Exception e) {
                    failedRequests.incrementAndGet();
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);
        //then
        assertEquals(1, successfulRequests.get(), "성공한 요청은 1개여야 합니다.");
        //assertEquals(99, failedRequests.get(), "실패한 요청은 99개여야 합니다.");
    }
}
