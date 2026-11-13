package com.ban.vehicle_management.application.billing.payment.port.out;

import java.time.YearMonth;

public interface SepayQuotaPortOut {

    boolean tryAcquire(YearMonth period, int limit);
}
