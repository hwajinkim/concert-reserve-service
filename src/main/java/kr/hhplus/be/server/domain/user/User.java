package kr.hhplus.be.server.domain.user;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.exception.LackBalanceException;
import kr.hhplus.be.server.domain.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", unique = true, nullable = false)
    private Long id;

    @Column(nullable = false)
    private String userName;

    private BigDecimal pointBalance;

    public static final BigDecimal MAX_BALANCE = BigDecimal.valueOf(1000000.00); // 최대 보유 포인트 1,000,000원

    // 낙관적 락 사용 시 버전 필드 추가
    /*@Version
    private Integer version = 0;*/

    @Builder
    public User(Long id, String userName, BigDecimal pointBalance){
        this.id = id;
        this.userName = userName;
        this.pointBalance = pointBalance;
    }

    public User charge(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        // 기존 잔액 + 추가 금액
        BigDecimal newBalance = this.pointBalance.add(amount);
        if(newBalance.compareTo(MAX_BALANCE) > 0){
            throw new IllegalArgumentException("최대 잔액 100만 포인트를 초과 할 수 없습니다.");
        }

        return User.builder()
                .id(this.id)
                .userName(this.userName)
                .pointBalance(newBalance)
                .build();
    }

    public User use(BigDecimal pointBalance, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }

        // 기존 잔액 < 포인트 사용 금액이면 예외 발생
        if (pointBalance.compareTo(amount) < 0) {
            throw new LackBalanceException("잔액이 부족합니다.");
        }
        // 기존 잔액 - 사용 금액
        BigDecimal newBalance = pointBalance.subtract(amount);

        return User.builder()
                .id(this.id)
                .userName(this.userName)
                .pointBalance(newBalance)
                .build();
    }
}
