package me.sk.ta.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static me.sk.ta.domain.DelayedFormatterUtility.format;

public class Trade {
    private static final Logger log = LoggerFactory.getLogger(Trade.class);
    private final TradingChargesCalculator tcCalculator;
    public int ID;
    public String symbol;
    public BuyAnalysis buyAnalysis;

    public double getStopLoss() {
        return getCurrentAnalysis().getStopLoss();
    }

    public List<CurrentAnalysis> analysisHistory;
    public List<TradeContract> tradeHistory;
    public int position;
    public int unfilledPosition;

    public boolean isClosed() {
        return position == 0 && tradeHistory.size() > 0;
    }

    public double averageBuyPrice() {
        if (tradeHistory.stream().anyMatch(x -> x.isSale() == false)) {
            return totalBuyPrice() / tradeHistory.stream()
                    .filter(x -> x.isSale() == false)
                    .mapToInt(x -> x.size())
                    .sum();
        } else {
            return 0.00;
        }
    }

    public double currentInvestment() {
        return Utils.round(getHoldingSize() * averageBuyPrice(), 2);
    }

    public double totalCharges() {
        return Utils.round(tradeHistory.stream().mapToDouble(x -> x.charges()).sum(), 2);
    }


    public double currentInvestmentCharges() {
        if (tradeHistory.stream().anyMatch(x -> x.isSale() == false)) {
            var avg = totalCharges() / tradeHistory.stream().filter(x -> x.isSale() == false).mapToInt(x -> x.size()).sum();
            return Utils.round(getHoldingSize() * avg, 2);
        } else {
            return 0.00;
        }
    }

    public double totalBuyPrice() {
        var total = tradeHistory.stream()
                .filter(x -> x.isSale() == false)
                .mapToDouble(x -> x.totalPrice())
                .sum();
        return Utils.round(total, 2);
    }

    public double totalSalePrice() {
        var total = tradeHistory.stream()
                .filter(x -> x.isSale())
                .mapToDouble(x -> x.totalPrice())
                .sum();
        return Utils.round(total, 2);
    }

    public double realisedPnl() {
        if (tradeHistory.stream().anyMatch(x -> x.isSale()) == false) {
            return 0.00;
        }

        return Utils.round(grossPnl() - totalCharges(), 2);
    }

    public double grossPnl() {
        if (tradeHistory.stream().anyMatch(x -> x.isSale()) == false) {
            return 0.00;
        }

        var soldSize = tradeHistory.stream().filter(x -> x.isSale()).mapToInt(x -> x.size()).sum();
        var totalBuyPrice = averageBuyPrice() * soldSize;
        return Utils.round(totalSalePrice() - totalBuyPrice, 2);
    }

    public double unrealisedPnl() {
        if (position == 0) {
            return 0.00;
        }

        var totalBuyPrice = averageBuyPrice() * position;
        var currentValue = totalBuyPrice();
        log.trace("avgBuyPrice:{} totalBuyPrice:{}", format("%.2f", averageBuyPrice()), format("%.2f", totalBuyPrice()));
        if (analysisHistory.size() > 0) {
            var hist = analysisHistory.get(analysisHistory.size() - 1);
            currentValue = hist.getPrice() * position;
            log.trace("currentValue: {}", format("%.2f", currentValue));
        }
        return Utils.round(currentValue - totalBuyPrice(), 2);
    }

    public int getHoldingSize() {
        return tradeHistory.stream().filter(x -> x.isSale() == false).mapToInt(x -> x.size()).sum() -
                tradeHistory.stream().filter(x -> x.isSale()).mapToInt(x -> x.size()).sum();
    }

    public Trade(TradingChargesCalculator tcCalculator) {
        analysisHistory = new ArrayList<>();
        tradeHistory = new ArrayList<>();
        this.tcCalculator = tcCalculator;
    }

    public void checkNulls() {
        if (analysisHistory == null) {
            log.warn("{}-{}: AnalysisHistory was null.", ID, symbol);
            analysisHistory = new ArrayList<>();
        }
        if (analysisHistory.stream().anyMatch(x -> x == null)) {
            log.warn("Found {} null entries out of {} in AnalysisHistory",
                    analysisHistory.stream().filter(x -> x == null).count(),
                    analysisHistory.stream());
            analysisHistory.removeAll(analysisHistory.stream().filter(x -> x == null).toList());
        }
        if (tradeHistory == null) {
            log.warn("{}-{}: TradeHistory was null.", ID, symbol);
            tradeHistory = new ArrayList<>();
        }
        if (tradeHistory.stream().anyMatch(x -> x == null)) {
            log.warn("Found {} null entries out of {} in tradeHistory",
                    tradeHistory.stream().filter(x -> x == null).count(),
                    tradeHistory.size());
            tradeHistory.removeAll(tradeHistory.stream().filter(x -> x == null).toList());
        }
        if (symbol == null) {
            log.warn("Found trade id {} with a null symbol", ID);
        }
        if (buyAnalysis == null) {
            log.warn("Found trade {}-{} with null BuyAnalysis", ID, symbol);
        }
    }

    public static Trade initiateTrade(String symbol, BuyAnalysis analysis, TradingChargesCalculator calculator) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol");
        }
        if (analysis == null) {
            throw new IllegalArgumentException("analysis");
        }
        if (calculator == null) {
            throw new IllegalArgumentException("calculator");
        }
        var trade = new Trade(calculator);
        trade.symbol = symbol;
        trade.buyAnalysis = analysis;
        trade.calculatePosition();
        return trade;
    }

    public void updatePlan(BuyAnalysis plan) {
        plan.calculatePosition();
        buyAnalysis = plan;
        calculatePosition();
    }

    public PositionCalculationResult calculatePyramidPosition(double pyramidPrice, double newStoploss, int percentOfProfitToBeLockedIn) {
        var currentSize = getHoldingSize();
        var remainingSize = buyAnalysis.getPosition() - currentSize;
        var currentCost = totalBuyPrice() + totalCharges();
        return buyAnalysis.CalculatePyramidPosition(currentCost, currentSize, averageBuyPrice(), pyramidPrice, newStoploss, percentOfProfitToBeLockedIn);
    }

    public PositionCalculationResult calculateScaleinPosition(double newPrice, double newStoploss) {
        if (newStoploss <= 0) {
            newStoploss = buyAnalysis.getStopLoss();
        } else {
            log.trace("New stoploss:{}", newStoploss);
        }
        var currentSize = getHoldingSize();
        var currentCost = totalBuyPrice() + totalCharges();
        return buyAnalysis.CalculateAdditionalPosition(currentCost, currentSize, newPrice, newStoploss);
    }

    public CurrentAnalysis getCurrentAnalysis() {
        if (analysisHistory.size() == 0) {
            return CurrentAnalysis.from(buyAnalysis);
        }
        return analysisHistory.get(analysisHistory.size() - 1);
    }


    public void addOrUpdateAnalysis(CurrentAnalysis analysis) {
        var existing = analysisHistory.stream().filter(x -> x.getDate() == analysis.getDate()).findFirst();
        existing.ifPresent(x -> analysisHistory.remove(x));
        analysisHistory.add(analysis);
    }

    public void setNewStoploss(double price) {
        if (analysisHistory.size() == 0) {
            throw new RuntimeException("Set stoploss in today's analysis");
        }
        var a = getCurrentAnalysis();
        a.setPrice(price);
        if (analysisHistory.size() > 0) {
            a.setDate(Utils.UtcToday());
        } else if (a.getDate().isBefore(Utils.UtcToday())) {
            a.setDate(Utils.UtcToday());
        }
        analysisHistory.add(a);
    }

    public TradeContract Buy(int id, int size, double averagePrice, LocalDate date, boolean isIntraDay) {
        log.trace("Purchased {}", size);
        var contract = new TradeContract(id, date, size, averagePrice, isIntraDay, this.tcCalculator);
        saveOrUpdateOrder(contract);
        return contract;
    }

    public TradeContract Sell(int orderId, int size, double averagePrice, LocalDate date, boolean isIntraDay) {
        log.trace("Sold {}", size);
        var contract = new TradeContract(orderId, date, size, averagePrice, isIntraDay, this.tcCalculator);
        contract.isSale(true);
        saveOrUpdateOrder(contract);
        return contract;
    }

    private void saveOrUpdateOrder(TradeContract contract) {
        if (contract.isValid()) {
            if (contract.id() != 0) {
                if (tradeHistory.size() > 0) {
                    tradeHistory.removeAll(tradeHistory.stream().filter(x -> x.id() == contract.id() && x.isSale() == contract.isSale()).toList());
                }
            } else {
                if (tradeHistory.size() > 0) {
                    contract.id(tradeHistory.stream().mapToInt(TradeContract::id).max().getAsInt() + 1);
                } else {
                    contract.id(1);
                }
            }
            tradeHistory.add(contract);
            tradeHistory.sort(Comparator.comparingInt(TradeContract::id));
            calculatePosition();
        } else {
            throw new RuntimeException("Invalid contract");
        }
    }

    public CurrentAnalysis generateNewAnalysis() {
        var analysis = CurrentAnalysis.builder()
                .date(Utils.UtcToday())
                .build();

        var prev = (AnalysisAbstract) buyAnalysis;

        if (analysisHistory.size() > 0) {
            // latest entry
            prev = analysisHistory.stream()
                    .sorted(Comparator.comparing(CurrentAnalysis::getDate))
                    .skip(analysisHistory.size() - 1)
                    .findFirst()
                    .get();
        }

        return CurrentAnalysis.builder()
                .date(Utils.UtcToday())
                .guruScoreBg(prev.guruScoreBg)
                .guruScoreJos(prev.guruScoreJos)
                .guruScorePl(prev.guruScorePl)
                .guruScoreWon(prev.guruScoreWon)
                .guruScoreWb(prev.guruScoreWb)
                .increaseInFundHoldings(prev.increaseInFundHoldings)
                .increaseInFunds(prev.increaseInFunds)
                .cmfTrend(prev.cmfTrend)
                .adTrend(prev.adTrend)
                .obvTrend(prev.obvTrend)
                .stopLoss(prev.stopLoss)
                .adPositive(prev.adPositive)
                .obvPositive(prev.obvPositive)
                .cmfPositive(prev.cmfPositive)
                .build();
    }

    public Optional<LocalDate> getDateOfClosure() {
        var tc = tradeHistory.stream()
                .filter(x -> x.isSale())
                .sorted(Comparator.comparing(TradeContract::date))
                .reduce((first, second) -> second);
        if (tc.isPresent()) {
            return Optional.of(tc.get().date());
        } else {
            return Optional.empty();
        }
    }

    public static boolean IsValidDate(int year, int month, int day) {
        if (year < 2000)
            return false;

        try {
            LocalDate.of(year, month, day);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

    private void calculatePosition() {
        position = tradeHistory.stream().filter(x -> x.isSale() == false).mapToInt(TradeContract::size).sum();
        position -= tradeHistory.stream().filter(x -> x.isSale()).mapToInt(TradeContract::size).sum();
        unfilledPosition = buyAnalysis.calculatePosition() - position;
    }
}
