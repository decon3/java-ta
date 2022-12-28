package me.sk.ta.api.interfaces;

public record LedgerEntry(
        String symbol,
        int size,
        double buyAveragePrice,
        double sellAveragePrice,
        double charges,
        double pnl) {
}
