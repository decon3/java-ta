package me.sk.ta.api.interfaces;

public record PortfolioEntry(
        int id,
        String symbol,
        int position,
        double averagePrice,
        int unfilledPosition,
        double currentInvestment,
        double currentInvestmentCharges,
        double realizedPnl,
        double unrealizedPnl) {

}
