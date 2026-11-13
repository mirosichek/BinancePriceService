package com.example.BinancePriceService.Controller;

import com.example.BinancePriceService.DTO.AveragePriceResponse;
import com.example.BinancePriceService.service.PriceQueryService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/prices")
@RequiredArgsConstructor
public class AveragePriceController {

    private final PriceQueryService priceQueryService;

    @GetMapping("/average")
    public AveragePriceResponse getAveragePrice(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to,
            @RequestParam(required = false)
            @Pattern(
                    regexp = "[A-Za-z0-9]{5,20}",
                    message = "symbol must contain 5-20 letters or digits"
            )
            String symbol
    ) {
        return new AveragePriceResponse(priceQueryService.getAveragePrice(symbol, from, to));
    }
}
