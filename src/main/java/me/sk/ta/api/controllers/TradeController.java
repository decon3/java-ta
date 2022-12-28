package me.sk.ta.api.controllers;

import me.sk.ta.api.interfaces.LedgerEntry;
import me.sk.ta.api.interfaces.PortfolioEntry;
import me.sk.ta.domain.*;
import me.sk.ta.api.interfaces.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trade")
public class TradeController {
    private static final Logger log = LoggerFactory.getLogger(TradeController.class);
    @Autowired
    private TradeRepository tradeRepo;
    @Autowired
    private TradingChargesCalculator chargesCalculator;

    @GetMapping("sample/{symbol}")
    public ResponseEntity<Trade> Get(@PathVariable String symbol) {
        var ba = BuyAnalysis.builder()
                .earningsDate(Utils.UtcToday().plusDays(10))
                .build();

        ba.priceLevels(9000000, 0.5, 320, 340, 320 * .12, 340 * 0.93)
                .breakoutDetails("c", BreakoutPattern.DoubleBottom, 3, 290)
                .risk(0.9, 0.3, 100000, MarketTrend.Rally)
                .largePlayers(12, 34, 2.3)
                .scoresByGurus(80, 80, 80, 80)
                .wonScores(80, ADRating.C, 80, 80, 90)
                .onBalanceValue(Trend.Up, true, 1000000)
                .adRating(Trend.Up, true)
                .moneyFlow(Trend.Up, true)
                .movingAverages(340, 330, 300);

        var trade = Trade.initiateTrade(symbol, ba, chargesCalculator);
        var na = trade.generateNewAnalysis();
        na.movingAverages(1, 1, 1)
                .scoresByGurus(1, 1, 1, 1)
                .onBalanceValue(Trend.Neutral, true, 1)
                .adRating(Trend.Neutral, true)
                .moneyFlow(Trend.Neutral, true);
        na.setMarketTrend(MarketTrend.None);
        na.setStopLoss(1);
        na.setPrice(1);
        trade.addOrUpdateAnalysis(na);
        return ResponseEntity.status(HttpStatus.OK).body(trade);
    }

    @GetMapping("{tradeId}")
    public ResponseEntity<Trade> Get(@PathVariable int tradeId) {
        var trade = tradeRepo.get(tradeId);
        if (trade.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(trade.get());
        }
    }

    @GetMapping("find/open/{symbol}")
    public ResponseEntity<Trade> GetOpenTradeBySymbol(@PathVariable String symbol) {
        var trade = tradeRepo.getOpenTrade(symbol);
        if (trade.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(trade.get());
        }
    }

    @GetMapping("find/all")
    public ResponseEntity<List<Trade>> GetAll() {
        var list = tradeRepo.getOpenTrades();
        if (list == null || list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(list);
        }
    }

    @GetMapping("cost/{price}/{size}/{isIntraDay}")
    public ResponseEntity<PositionCalculationResult> EstimateCost(
            @PathVariable("price") double price,
            @PathVariable("size") int size,
            @PathVariable("isIntraDay") boolean isIntraDay) {
        var charges = chargesCalculator.estimateCostOfTrade(price * size, isIntraDay);
        return ResponseEntity.status(HttpStatus.OK).body(new PositionCalculationResult(price * size, charges.total(), size));
    }

    @GetMapping("position/{tradeId}/{price}")
    public ResponseEntity<Integer> CalculatePosition(@PathVariable("tradeId") int tradeId, @PathVariable("price") double price) {
        var trade = tradeRepo.get(tradeId);
        if (trade == null || trade.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(trade.get().buyAnalysis.calculatePosition(price));
        }
    }

    @GetMapping("position/scaleIn/{tradeId}/{price}/{stopLoss}")
    public ResponseEntity<PositionCalculationResult> CalculateScaleInPosition(
            @PathVariable("tradeId") int tradeId,
            @PathVariable("price") double price,
            @PathVariable("stopLoss") double stopLoss) {
        var trade = tradeRepo.get(tradeId);
        if (trade == null || trade.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(trade.get().calculateScaleinPosition(price, stopLoss));
        }
    }

    @GetMapping("position/pyramid/{tradeId}/{price}/{stopLoss}/{percentOfProfitToBeLockedIn}")
    public ResponseEntity<PositionCalculationResult> CalculatePyramidPosition(
            @PathVariable("tradeId") int tradeId,
            @PathVariable("price") double price,
            @PathVariable("stopLoss") double stopLoss,
            @PathVariable("percentOfProfitToBeLockedIn") int percentOfProfitToBeLockedIn) {
        var trade = tradeRepo.get(tradeId);
        if (trade == null || trade.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(trade.get().calculatePyramidPosition(price, stopLoss, percentOfProfitToBeLockedIn));
        }
    }

    @GetMapping("stopLoss/{tradeId}")
    public ResponseEntity<Double> GetCurrentStopLoss(@PathVariable("tradeId") int tradeId) {
        var trade = tradeRepo.get(tradeId);
        if (trade == null || trade.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(trade.get().getStopLoss());
        }
    }

    @PutMapping("stopLoss/{tradeId}/{price}")
    public ResponseEntity<Double> SetTrailingStopLoss(
            @PathVariable("tradeId") int tradeId,
            @PathVariable("price") double newStopLoss) {
        var trade = tradeRepo.get(tradeId);
        if (trade == null || trade.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } else {
            trade.get().setNewStoploss(newStopLoss);
            var id = tradeRepo.saveOrUpdate(trade.get());
            if (id > 0) {
                return ResponseEntity.status(HttpStatus.OK).build();
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }
        }
    }

    @GetMapping("portfolio")
    public ResponseEntity<List<PortfolioEntry>> GetPortfolio() {
        var list = tradeRepo.getOpenTrades();
        if (list == null || list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } else {
            var result = list.stream()
                    .map(x ->
                            new PortfolioEntry(
                                    x.ID,
                                    x.symbol,
                                    x.position,
                                    x.averageBuyPrice(),
                                    x.unfilledPosition,
                                    x.currentInvestment(),
                                    x.currentInvestmentCharges(),
                                    x.realisedPnl(),
                                    x.unrealisedPnl()))
                    .toList();
            return ResponseEntity.status(HttpStatus.OK).body(result);
        }
    }

    @GetMapping("ledger")
    public ResponseEntity<List<LedgerEntry>> GetLedger() {
        var list = tradeRepo.getOpenTrades();
        list.addAll(tradeRepo.getClosedTrades());
        // TODO: include archived trades in ledger
        if (list == null || list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } else {
            var result = list.stream()
                    .map(x -> {
                        var position = x.tradeHistory.stream().filter(y -> y.isSale()).mapToInt(y -> y.size()).sum();
                        var averageSellPrice = position > 0 ? x.totalSalePrice() / position : 0.00;
                        return new LedgerEntry(
                                x.symbol,
                                position,
                                x.averageBuyPrice(),
                                averageSellPrice,
                                x.totalCharges(),
                                x.realisedPnl());
                    })
                    .toList();
            return ResponseEntity.status(HttpStatus.OK).body(result);
        }
    }

    @PostMapping("upload")
    public ResponseEntity<String> PostAll(@RequestBody List<Trade> trades) {
        if (trades == null || trades.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No trades to save");
        } else {
            try {
                for (var t : trades) {
                    tradeRepo.saveOrUpdate(t);
                }
                return ResponseEntity.status(HttpStatus.CREATED).build();
            } catch (Exception e) {
                log.error("Error: {}", e);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }
        }
    }

}
