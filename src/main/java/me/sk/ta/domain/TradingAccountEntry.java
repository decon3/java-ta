package me.sk.ta.domain;

import java.time.LocalDate;

public record TradingAccountEntry(AccountTransactionType transType,
                                  String businessTransactionId,
                                  double amount,
                                  LocalDate date,
                                  boolean isTradeClosed) {
    public static TradingAccountEntry Empty() {
        return new TradingAccountEntry(AccountTransactionType.Invalid, "", 0.00, Utils.UtcToday(), false);
    }
    public TradingAccountEntry closeTrade() {
        return new TradingAccountEntry(transType, businessTransactionId, amount, date, true);
    }
}
