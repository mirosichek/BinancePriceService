package com.example.BinancePriceService.Controller;

import com.example.BinancePriceService.DTO.TradingPairResponse;
import com.example.BinancePriceService.service.PriceQueryService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class TradingPairController {

    private final PriceQueryService priceQueryService;

    @GetMapping("/latest")
    public TradingPairResponse getLatestPrice(
            @RequestParam(required = false)
            @Pattern(
                    regexp = "[A-Za-z0-9]{5,20}",
                    message = "symbol must contain 5-20 letters or digits"
            )
            String symbol
    ) {
        return priceQueryService.getLatestPrice(symbol);
    }
}
