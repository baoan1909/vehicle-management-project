package com.ban.vehicle_management.entrypoint.converter;

import org.springframework.stereotype.Component;

@Component
public class PaginationRequestConverter {

    public PageParameters resolve(Integer page, Integer size) {
        return new PageParameters(page == null ? 0 : page, size == null ? 10 : size);
    }

    public int resolveLimit(Integer limit) {
        return limit == null ? 0 : limit;
    }

    public record PageParameters(int page, int size) {
    }
}
