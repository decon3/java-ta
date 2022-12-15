package me.sk.ta.domain;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

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
}
