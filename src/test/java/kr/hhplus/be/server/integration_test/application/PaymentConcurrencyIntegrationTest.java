package kr.hhplus.be.server.integration_test.application;

import kr.hhplus.be.server.application.dto.payment.PaymentParam;
import kr.hhplus.be.server.application.dto.payment.PaymentResult;
import kr.hhplus.be.server.application.dto.reservation.ReservationParam;
import kr.hhplus.be.server.application.dto.reservation.ReservationResult;
import kr.hhplus.be.server.application.payment.PaymentFacade;
import kr.hhplus.be.server.domain.concert.Concert;
import kr.hhplus.be.server.domain.concert.Schedule;
import kr.hhplus.be.server.domain.concert.Seat;
import kr.hhplus.be.server.domain.concert.SeatStatus;
import kr.hhplus.be.server.domain.queue.Queue;
import kr.hhplus.be.server.domain.queue.QueueStatus;
import kr.hhplus.be.server.domain.reservation.Reservation;
import kr.hhplus.be.server.domain.reservation.ReservationState;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.integration_test.application.set_up.*;
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

public class PaymentConcurrencyIntegrationTest extends BaseIntegrationTest{
    @Autowired
    private PaymentFacade paymentFacade;
    @Autowired
    private UserSetUp userSetUp;
    @Autowired
    private QueueSetUp queueSetUp;
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
        for (int i = 0; i < 5; i++) {
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
    void 비관적락_같은_유저가_동시에_20번_결제_신청하면_1번만_결제성공() throws InterruptedException {
        //given
        User user = users.get(0);
        Queue queue = queueSetUp.saveQueue(user.getId(), QueueStatus.WAIT, LocalDateTime.now().plusMinutes(10));

        Concert concert = concertSetUp.saveConcert("Awesome Concert", scheduleList);

        Schedule schedule = scheduleSetUp.saveSchedule(
                BigDecimal.valueOf(50000.00),
                LocalDateTime.of(2025,1,15,20,0,0),
                LocalDateTime.of(2025,1,1, 10,0,0),
                LocalDateTime.of(2025,1,10,18,0,0),
                50, 100, concert, seatList);

        Reservation reservation = reservationSetUp.saveReservation(
                user.getId(),
                schedule.getSeats().get(0).getId(),
                ReservationState.PANDING,
                schedule.getSeats().get(0).getSeatPrice(),
                LocalDateTime.now().plusMinutes(5)
        );
        PaymentParam paymentParam = new PaymentParam(reservation.getId(), schedule.getSeats().get(0).getId(), user.getId(), queue.getId());

        int threadCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        AtomicInteger successfulRequests = new AtomicInteger();
        AtomicInteger failedRequests = new AtomicInteger();

        //when
        for(int i = 0; i < threadCount; i++) {

            executorService.submit(() -> {
                try {
                    PaymentResult paymentResult = paymentFacade.createPayment(paymentParam);
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
        assertEquals(19, failedRequests.get(), "실패한 요청은 19개여야 합니다.");
    }
}
