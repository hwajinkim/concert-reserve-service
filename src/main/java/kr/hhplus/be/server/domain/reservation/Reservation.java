package kr.hhplus.be.server.domain.reservation;

import jakarta.persistence.*;
import kr.hhplus.be.server.domain.common.BaseEntity;
import kr.hhplus.be.server.domain.concert.Seat;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "reservation")
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id", unique = true, nullable = false)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long seatId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationState reservationState;

    @Column(nullable = false)
    private BigDecimal seatPrice;

    private LocalDateTime expiredAt;

    // 낙관적 락 사용 시 버전 필드 추가
    /*@Version
    private Integer version = 0;*/

    @Builder
    public Reservation(Long reservationId, Long userId, Long seatId, ReservationState reservationState,
                       BigDecimal seatPrice, LocalDateTime expiredAt){
        this.id = reservationId;
        this.userId = userId;
        this.seatId = seatId;
        this.reservationState = reservationState;
        this.seatPrice = seatPrice;
        this.expiredAt = expiredAt;
    }

    public Reservation create(Seat seat, Long userId) {
        // 현재 시간에 5분 추가(임시 배정 목적)
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5);

        return Reservation.builder()
                .userId(userId)
                .seatId(seat.getId())
                .reservationState(ReservationState.PANDING)
                .seatPrice(seat.getSeatPrice())
                .expiredAt(expiryTime)
                .build();
    }

    public Reservation update(Long reservationId, Long seatId, Long userId, ReservationState reservationState, BigDecimal seatPrice) {

        return Reservation.builder()
                .reservationId(reservationId)
                .userId(userId)
                .seatId(seatId)
                .reservationState(reservationState)
                .seatPrice(seatPrice)
                .expiredAt(null) // 예약 생성 시에만 임시배정 목적으로 현재시간 + 5분으로 들어가고, 상태 변경이 일어나면(임시 배정 시간 만료, 결제완료)시에는 null로 들어감.
                .build();
    }
}
