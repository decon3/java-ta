package me.sk.ta.api.interfaces;

import me.sk.ta.domain.Trade;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface TradeRepository {
    Trade find(int id);

    Optional<Trade> get(int id);

    List<Trade> where(Predicate<Trade> predicate);

    Optional<Trade> getOpenTrade(String symbol);

    List<Trade> getOpenTrades();

    List<Trade> getClosedTrades(LocalDate from, LocalDate to);

    List<Trade> getClosedTrades();

    int saveOrUpdate(Trade trade);

    boolean delete(int id);

    void close();
}
