package kr.hhplus.be.server.infrastructure.pointhistory;

import kr.hhplus.be.server.domain.user.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryJpaRepository extends JpaRepository<PointHistory, Long> {
}
