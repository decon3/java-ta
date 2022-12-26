package me.sk.ta.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Objects;

import static me.sk.ta.domain.DelayedFormatterUtility.format;

@Getter
@SuperBuilder
public class BuyAnalysis extends AnalysisAbstract {
    @Autowired
    TradingChargesCalculator tcCalculator;
    private static final Logger log = LoggerFactory.getLogger(BuyAnalysis.class);
    // Applicable to only BuyAnalysis
    private double capital;
    private double percentOfCapitalRisked;
    private double targetPrice;
    private int position;
    private int stage;
    private char breakout;
    private BreakoutPattern pattern;
    private double pivot;
    @Setter(AccessLevel.NONE)
    private double buyRangeLow;
    @Setter(AccessLevel.NONE)
    private double buyRangeHigh;
    private LocalDate earningsDate;

    public BuyAnalysis() {
        // for deserialization
        super();
    }
    public BuyAnalysis priceLevels(double capital, double percentOfCapitalRisked, double low, double high, double target, double stopLoss) {
        this.buyRangeLow = low;
        this.buyRangeHigh = high;
        this.targetPrice = target;
        this.stopLoss = stopLoss;
        this.capital = capital;
        this.percentOfCapitalRisked = percentOfCapitalRisked;
        calculatePosition();
        return this;
    }

    public BuyAnalysis breakoutDetails(String breakout, BreakoutPattern pattern, int stage, double pivot) {
        this.breakout = breakout.charAt(0);
        this.pattern = pattern;
        this.stage = stage;
        this.pivot = pivot;
        return this;
    }

    public int calculatePosition() {
        position = calculatePosition((buyRangeHigh + buyRangeLow) / 2);
        log.trace("Range: {} to {}. MidPoint:{}", format("%.2f", buyRangeHigh), format("%.2f", buyRangeLow), format("%.2f", (buyRangeHigh - buyRangeLow) / 2));
        return position;
    }

    public int calculatePosition(double buyPrice) {
        if (buyPrice == stopLoss)
            buyPrice++;
        log.trace("BuyPrice: {} StopLoss:{}", format("%.2f", buyPrice), format("%.2f", stopLoss));
        log.trace("Risk amount:{}, %risk:{} Risk/Share:{}",
                format("%.2f", capital * (percentOfCapitalRisked / 100)),
                format("%.2f", percentOfCapitalRisked),
                format("%.2f", buyPrice - stopLoss));
        return (int) ((capital * (percentOfCapitalRisked / 100)) / (buyPrice - stopLoss));
    }

    public PositionCalculationResult CalculateAdditionalPosition(double currentCost, int currentSize, double addPrice, double newStoploss) {
        var riskedCapital = (capital * percentOfCapitalRisked) / 100;
        var pnl = (currentSize * addPrice) - currentCost;
        var remainingRiskedCapital = riskedCapital + pnl;
        if (remainingRiskedCapital <= 0) {
            return new PositionCalculationResult(0, 0, 0);
        }
        var risk = addPrice - newStoploss;
        if (risk <= 0) {
            risk = 1;
        }
        var numberOfSharesThatCanBeBought = (int) (remainingRiskedCapital / risk);
        log.trace("Current Cost:{} Size:{}. P&L:{}. RemainingRiskedCapital:{}. AddPrice:{}. Risk:{} CanBuy:{} shares",
                format("%.2f", currentCost),
                currentSize,
                format("%.2f", pnl),
                format("%.2f", remainingRiskedCapital),
                format("%.2f", addPrice),
                format("%.2f", risk),
                numberOfSharesThatCanBeBought);
        var p = new PositionCalculationResult(
                numberOfSharesThatCanBeBought * addPrice,
                tcCalculator.estimateCostOfTrade(numberOfSharesThatCanBeBought * addPrice, false).total(),
                numberOfSharesThatCanBeBought);
        log.trace("Exit - {}", p);
        return p;
    }

    PositionCalculationResult CalculatePyramidPosition(
            double currentCost,
            int currentPosition,
            double averageBuyPrice,
            double pyramidPrice,
            double newStoploss,
            double percentOfProfitToBeLockedIn) {
        log.trace("Entered - currentCost:{}, currentPosition:{} avgBuyPrice:{} pyramidPrice:{} newStopLoss:{} percentOfProfitToBeLockedIn:{}",
                format("%.2f", currentCost),
                format("%.2f", currentPosition),
                format("%.2f", averageBuyPrice),
                format("%.2f", pyramidPrice),
                format("%.2f", newStoploss),
                format("%.2f", percentOfProfitToBeLockedIn));

        var pnl = (pyramidPrice * currentPosition) - currentCost;
        var pnlLockedIn = pnl * (percentOfProfitToBeLockedIn / 100);
        var pnlToBeRisked = pnl - pnlLockedIn;
        var riskPerShareInPyramid = pyramidPrice - newStoploss;
        if (riskPerShareInPyramid <= 0) {
            riskPerShareInPyramid = 1;
        }
        log.trace("PNL:{} (Locked in:{}, risked:{}). PnLToBeRisked:{} PotentialRiskPerShare(Pyramid):{}",
                format("%.2f", pnl),
                percentOfProfitToBeLockedIn,
                format("%.2f", pnlLockedIn),
                format("%.2f", pnlToBeRisked),
                format("%.2f", riskPerShareInPyramid));
        var pyramidPosition = (int) (pnlToBeRisked / riskPerShareInPyramid);
        if (pyramidPosition <= 0) {
            return new PositionCalculationResult(0, 0, 0);
        }

        var totalPosition = currentPosition + pyramidPosition;
        var newAveragePrice = 0.00;
        if (totalPosition > 0) {
            newAveragePrice = ((pyramidPosition * pyramidPrice) + currentCost) / totalPosition;
        }
        log.trace("newAveragePrice:{} TotalPosition:{} + {} (pyramid) = {}",
                format("%.2f", newAveragePrice),
                currentPosition,
                pyramidPosition,
                totalPosition);
        var p = new PositionCalculationResult(
                pyramidPosition * pyramidPrice,
                tcCalculator.estimateCostOfTrade(pyramidPosition * pyramidPrice, false).total(),
                pyramidPosition);
        log.trace("Exit - " + p);
        return p;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BuyAnalysis that = (BuyAnalysis) o;
        return Double.compare(that.capital, capital) == 0 &&
                Double.compare(that.percentOfCapitalRisked, percentOfCapitalRisked) == 0 &&
                Double.compare(that.targetPrice, targetPrice) == 0 &&
                position == that.position &&
                stage == that.stage &&
                breakout == that.breakout &&
                Double.compare(that.pivot, pivot) == 0 &&
                Double.compare(that.buyRangeLow, buyRangeLow) == 0 &&
                Double.compare(that.buyRangeHigh, buyRangeHigh) == 0 &&
                pattern == that.pattern &&
                earningsDate.equals(that.earningsDate);
    }

    @Override
    public int hashCode() {
        return  Objects.hash(super.hashCode(),
                capital,
                percentOfCapitalRisked,
                targetPrice,
                position,
                stage,
                breakout,
                pattern,
                pivot,
                buyRangeLow,
                buyRangeHigh,
                earningsDate);
    }
}
