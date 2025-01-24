package kr.hhplus.be.server.integration_test.application;

import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserService;
import kr.hhplus.be.server.infrastructure.user.UserJpaRepository;
import kr.hhplus.be.server.integration_test.application.set_up.UserSetUp;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class UserConcurrencyIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private UserSetUp userSetUp;

    @Autowired
    private UserService userService;

    @AfterEach
    void clear(){
        userJpaRepository.deleteAll();
    }


    @Test
    void 비관락_동일한_유저가_동시에_20번_포인트_충전을_했을_때_예상금액과_같은지_검증() throws InterruptedException {
        //given
        int threadCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        BigDecimal chargePoint = BigDecimal.valueOf(1000);

        User savedUser = userSetUp.saveUser("김화진", BigDecimal.valueOf(10000));

        AtomicInteger successfulRequests = new AtomicInteger();
        AtomicInteger failedRequests = new AtomicInteger();
        //when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try{
                    User user = userService.charge(savedUser.getId(), chargePoint);
                    successfulRequests.incrementAndGet();
                }catch(Exception e){
                    failedRequests.incrementAndGet();
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);

        //then
        BigDecimal pointBalance = userService.findById(savedUser.getId()).getPointBalance();
        assertEquals(BigDecimal.valueOf(10000).add(chargePoint.multiply(BigDecimal.valueOf(successfulRequests.get()))).setScale(2, RoundingMode.DOWN), pointBalance.setScale(2, RoundingMode.DOWN));
    }
}
