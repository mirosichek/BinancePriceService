package com.example.BinancePriceService.DTO;

import java.util.List;

public class PricePageResponse {

    private final List<TradingPairResponse> items;
    private final int page;
    private final int size;
    private final long totalElements;
    private final boolean hasNext;

    public PricePageResponse(
            List<TradingPairResponse> items,
            int page,
            int size,
            long totalElements,
            boolean hasNext
    ) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.hasNext = hasNext;
    }

    public List<TradingPairResponse> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public boolean isHasNext() {
        return hasNext;
    }
}
