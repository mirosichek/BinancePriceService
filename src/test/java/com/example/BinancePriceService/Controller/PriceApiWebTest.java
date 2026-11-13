package com.example.BinancePriceService.Controller;

import com.example.BinancePriceService.DTO.PricePageResponse;
import com.example.BinancePriceService.DTO.TradingPairResponse;
import com.example.BinancePriceService.DTO.TrendResponse;
import com.example.BinancePriceService.doming.TrendCalculator;
import com.example.BinancePriceService.exception.AveragePriceNotFoundException;
import com.example.BinancePriceService.exception.InvalidPeriodException;
import com.example.BinancePriceService.exception.PriceNotFoundException;
import com.example.BinancePriceService.exception.TrendNotFoundException;
import com.example.BinancePriceService.service.PriceQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционный тест web-слоя: контроллеры + сериализация + обработчик ошибок.
 */
@WebMvcTest
class PriceApiWebTest {

    private static final OffsetDateTime FROM = OffsetDateTime.parse("2026-11-12T00:00:00Z");
    private static final OffsetDateTime TO = OffsetDateTime.parse("2026-11-13T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PriceQueryService priceQueryService;

    @Test
    void returnsTrendContract() throws Exception {
        when(priceQueryService.getTrend(FROM, TO)).thenReturn(new TrendResponse(
                "BNBUSDT",
                LocalDate.of(2026, 11, 12),
                LocalDate.of(2026, 11, 13),
                TrendCalculator.Trend.UPWARD
        ));

        mockMvc.perform(get("/api/v1/prices/trend")
                        .param("from", "2026-11-12T00:00:00Z")
                        .param("to", "2026-11-13T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "symbol": "BNBUSDT",
                          "start_date": "2026-11-12",
                          "end_date": "2026-11-13",
                          "trend": "UPWARD"
                        }
                        """, true));
    }

    @Test
    void returnsPricePageContract() throws Exception {
        PricePageResponse page = new PricePageResponse(
                List.of(new TradingPairResponse(
                        "BNBUSDT",
                        new BigDecimal("654.85561546"),
                        OffsetDateTime.parse("2026-11-12T21:04:31Z")
                )),
                0,
                20,
                135,
                true
        );
        when(priceQueryService.getPrices(FROM, TO, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/prices")
                        .param("from", "2026-11-12T00:00:00Z")
                        .param("to", "2026-11-13T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "items": [
                            {
                              "symbol": "BNBUSDT",
                              "price": 654.85561546,
                              "fetchedAt": "2026-11-12T21:04:31Z"
                            }
                          ],
                          "page": 0,
                          "size": 20,
                          "totalElements": 135,
                          "hasNext": true
                        }
                        """, true));
    }

    @Test
    void returnsAverageContract() throws Exception {
        when(priceQueryService.getAveragePrice(null, FROM, TO))
                .thenReturn(new BigDecimal("654.49473022"));

        mockMvc.perform(get("/api/v1/prices/average")
                        .param("from", "2026-11-12T00:00:00Z")
                        .param("to", "2026-11-13T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"averagePrice\":654.49473022}", true));
    }

    @Test
    void returnsLatestPriceContract() throws Exception {
        when(priceQueryService.getLatestPrice(null)).thenReturn(new TradingPairResponse(
                "BNBUSDT",
                new BigDecimal("631.62039181"),
                OffsetDateTime.parse("2026-11-13T05:02:51Z")
        ));

        mockMvc.perform(get("/api/prices/latest"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "symbol": "BNBUSDT",
                          "price": 631.62039181,
                          "fetchedAt": "2026-11-13T05:02:51Z"
                        }
                        """, true));
    }

    @Test
    void mapsMissingParameterToBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/prices/trend").param("to", "2026-11-13T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.detail").value("Required parameter 'from' is missing"));
    }

    @Test
    void rejectsInvalidPeriod() throws Exception {
        when(priceQueryService.getTrend(TO, FROM))
                .thenThrow(new InvalidPeriodException("'from' must be before or equal to 'to'"));

        mockMvc.perform(get("/api/v1/prices/trend")
                        .param("from", "2026-11-13T00:00:00Z")
                        .param("to", "2026-11-12T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid period"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsNegativePageWithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/prices")
                        .param("from", "2026-11-12T00:00:00Z")
                        .param("to", "2026-11-13T00:00:00Z")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));

        verifyNoInteractions(priceQueryService);
    }

    @Test
    void mapsEmptyTrendPeriodToNotFound() throws Exception {
        when(priceQueryService.getTrend(FROM, TO))
                .thenThrow(new TrendNotFoundException(
                        "BNBUSDT",
                        FROM,
                        TO
                ));

        mockMvc.perform(get("/api/v1/prices/trend")
                        .param("from", "2026-11-12T00:00:00Z")
                        .param("to", "2026-11-13T00:00:00Z"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Trend data not found"));
    }

    @Test
    void mapsMissingPriceToNotFound() throws Exception {
        when(priceQueryService.getLatestPrice("BTCUSDT"))
                .thenThrow(new PriceNotFoundException("BTCUSDT"));

        mockMvc.perform(get("/api/prices/latest").param("symbol", "BTCUSDT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Price not found"));
    }

    @Test
    void mapsEmptyAveragePeriodToNotFound() throws Exception {
        when(priceQueryService.getAveragePrice(null, FROM, TO))
                .thenThrow(new AveragePriceNotFoundException("BNBUSDT", FROM, TO));

        mockMvc.perform(get("/api/v1/prices/average")
                        .param("from", "2026-11-12T00:00:00Z")
                        .param("to", "2026-11-13T00:00:00Z"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Average price not found"));
    }
}
