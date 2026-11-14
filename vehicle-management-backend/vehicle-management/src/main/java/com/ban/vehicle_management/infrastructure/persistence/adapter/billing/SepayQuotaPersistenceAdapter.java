package com.ban.vehicle_management.infrastructure.persistence.adapter.billing;

import com.ban.vehicle_management.application.billing.payment.port.out.SepayQuotaPortOut;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PaymentRepository;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class SepayQuotaPersistenceAdapter implements SepayQuotaPortOut {
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PaymentRepository paymentRepository;

    public SepayQuotaPersistenceAdapter(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public boolean tryAcquire(YearMonth period, int limit) {
        paymentRepository.acquireSepayQuotaLock("SEPAY:" + period);
        Instant from = period.atDay(1).atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant to = period.plusMonths(1).atDay(1).atStartOfDay(VIETNAM_ZONE).toInstant();
        return paymentRepository.countActiveSepayUsage(from, to) < limit;
    }
}
