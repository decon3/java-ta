package me.sk.ta.domain;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Objects;

@Getter
@SuperBuilder
public abstract class AnalysisAbstract {
    protected double alpha;
    protected double beta;
    protected double debt;
    protected double stopLoss;
    protected double dma20;
    protected double dma50;
    protected double dma100;
    protected double obvValue;
    protected Trend obvTrend;
    protected boolean obvPositive;

    protected Trend cmfTrend;
    protected boolean cmfPositive;

    protected Trend adTrend;
    protected boolean adPositive;
    protected String comments;

    protected int wonMasterRating;
    protected int wonRSRating;
    protected int wonEpsRating;
    protected ADRating wonADRating;

    // William O'Neil
    protected int guruScoreWon;
    // Peter Lynch
    protected int guruScorePl;
    // Warren Buffet
    protected int guruScoreWb;
    // Benjamin Graham
    protected int guruScoreBg;
    // James O'Shauganessy
    protected int guruScoreJos;
    protected MarketTrend marketTrend;
    protected double increaseInFunds;
    protected double increaseInFundHoldings;
    protected double floatingShares;

    public AnalysisAbstract() {
        // for deserialization
    }

    public AnalysisAbstract scoresByGurus(int guruScoreBg, int guruScoreJos, int guruScorePl, int guruScoreWb) {
        this.guruScoreBg = guruScoreBg;
        this.guruScoreJos = guruScoreJos;
        this.guruScorePl = guruScorePl;
        this.guruScoreWb = guruScoreWb;
        return this;
    }

    public AnalysisAbstract wonScores(int guruScoreWon, ADRating wonADRating, int epsRating, int masterRating, int rsRating ) {
        this.guruScoreWon = guruScoreWon;
        this.wonADRating = wonADRating;
        this.wonEpsRating = epsRating;
        this.wonMasterRating = masterRating;
        this.wonRSRating = rsRating;
        return this;
    }

    public AnalysisAbstract onBalanceValue(Trend trend, boolean isPositive, int currentValue) {
        this.obvTrend = trend;
        this.obvPositive = isPositive;
        this.obvValue = currentValue;
        return this;
    }
    public AnalysisAbstract adRating(Trend trend, boolean isPositive) {
        this.adTrend = trend;
        this.adPositive = isPositive;
        return this;
    }
    public AnalysisAbstract moneyFlow(Trend trend, boolean isPositive) {
        this.cmfTrend = trend;
        this.cmfPositive = isPositive;
        return this;
    }
    public AnalysisAbstract movingAverages(double dma20, double dma50, double dma100)
    {
        this.dma20 = dma20;
        this.dma50 = dma50;
        this.dma100 = dma100;
        return this;
    }
    public AnalysisAbstract largePlayers(double increaseInFundHoldings, double increaseInFunds, double floatingShares)
    {
        this.increaseInFundHoldings = increaseInFundHoldings;
        this.increaseInFunds = increaseInFunds;
        this.floatingShares = floatingShares;
        return this;
    }
    public AnalysisAbstract risk(double alpha, double beta, double debt, MarketTrend trend) {
        this.alpha = alpha;
        this.beta = beta;
        this.debt = debt;
        this.marketTrend = trend;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalysisAbstract that = (AnalysisAbstract) o;
        return Double.compare(that.alpha, alpha) == 0 &&
                Double.compare(that.beta, beta) == 0 &&
                Double.compare(that.debt, debt) == 0 &&
                Double.compare(that.stopLoss, stopLoss) == 0 &&
                Double.compare(that.dma20, dma20) == 0 &&
                Double.compare(that.dma50, dma50) == 0 &&
                Double.compare(that.dma100, dma100) == 0 &&
                Double.compare(that.obvValue, obvValue) == 0 &&
                obvPositive == that.obvPositive &&
                cmfPositive == that.cmfPositive &&
                adPositive == that.adPositive &&
                wonMasterRating == that.wonMasterRating &&
                wonRSRating == that.wonRSRating &&
                wonEpsRating == that.wonEpsRating &&
                guruScoreWon == that.guruScoreWon &&
                guruScorePl == that.guruScorePl &&
                guruScoreWb == that.guruScoreWb &&
                guruScoreBg == that.guruScoreBg &&
                guruScoreJos == that.guruScoreJos &&
                Double.compare(that.increaseInFunds, increaseInFunds) == 0 &&
                Double.compare(that.increaseInFundHoldings, increaseInFundHoldings) == 0 &&
                Double.compare(that.floatingShares, floatingShares) == 0 &&
                obvTrend == that.obvTrend &&
                cmfTrend == that.cmfTrend &&
                adTrend == that.adTrend &&
                Objects.equals(comments, that.comments) &&
                wonADRating == that.wonADRating &&
                marketTrend == that.marketTrend;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                alpha,
                beta,
                debt,
                stopLoss,
                dma20,
                dma50,
                dma100,
                obvValue,
                obvTrend,
                obvPositive,
                cmfTrend,
                cmfPositive,
                adTrend,
                adPositive,
                comments,
                wonMasterRating,
                wonRSRating,
                wonEpsRating,
                wonADRating,
                guruScoreWon,
                guruScorePl,
                guruScoreWb,
                guruScoreBg,
                guruScoreJos,
                marketTrend,
                increaseInFunds,
                increaseInFundHoldings,
                floatingShares);
    }
}
