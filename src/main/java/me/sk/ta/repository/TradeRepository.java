package me.sk.ta.repository;

import me.sk.ta.domain.Trade;
import me.sk.ta.domain.TradingAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class TradeRepository {
    private static final Logger log = LoggerFactory.getLogger(TradingAccount.class);
    // We always need an Env. An Env owns a physical on-disk storage file. One
    // Env can store many different databases (ie sorted maps).
    static final String TRADE_DB = "trade";
    static final String ARCHIVE_DB = "tradeArchive";
    static final String ACCOUNT_DB = "account";

    final String TRADE_KEY_COUNTER = "TRADE_ID_COUNTER";
    final RocksDbIndex<String, Integer> symbolIndex;
    final RocksDbIndex<LocalDate, Integer> dateIndex;
    final KvDb<String, Integer> countersDb;
    final KvDb<Integer, Trade> db;
    @Value("${db.current.trade-conn}")
    String dbPath;

    public TradeRepository() {
        db = new RocksDbRepo<Integer, Trade>(dbPath, "trade");
        symbolIndex = new RocksDbIndex<>(dbPath, "SYMBOL_INDEX", RocksDbIndex.IndexingStrategy.PostFixValue, String.class, Integer.class);
        dateIndex = new RocksDbIndex<>(dbPath, "DATE_INDEX", RocksDbIndex.IndexingStrategy.PostFixValue, LocalDate.class, Integer.class);
        countersDb = new RocksDbRepo<String, Integer>(dbPath, "counters");
    }

    public Optional<Trade> get(int id) {
        if (id < 1) {
            throw new IllegalArgumentException("id");
        }
        return db.find(id);
    }

    public Optional<Trade> getOpenTrade(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol");
        }

        var ids = symbolIndex.find(symbol);
        if (ids.size() == 0) {
            return Optional.empty();
        }
        if (ids.size() > 1) {
            throw new RuntimeException("Found more than one open trade in symbol index for symbol: " + symbol);
        }
        var t = get(ids.get(0));
        if (t.isEmpty()) {
            return t;
        }
        if (t.get().isClosed())
            return Optional.empty();
        else
            return t;
    }

    public List<Trade> getOpenTrades() {
        return db.findAll(x -> {
            return x.isClosed() ? Optional.empty() : Optional.of(x);
        });
    }

    public List<Trade> getClosedTrades(LocalDate from, LocalDate to) {
        return db.findAll(x -> {
            var date = x.getDateOfClosure();
            if (date.isEmpty()) {
                return Optional.empty();
            }
            if (date.get().isBefore(from) || date.get().isAfter(to)) {
                return Optional.empty();
            }
            return Optional.of(x);
        });
    }

    public List<Trade> getClosedTrades() {
        return db.findAll(x -> {
            return x.isClosed() ? Optional.of(x) : Optional.empty();
        });
    }


    // TODO commit write transaction
    // TODO close cursor
    // TODO flip bytebuffer after writing to it
    public int saveOrUpdate(Trade trade) {
        if (trade == null) {
            throw new IllegalArgumentException("trade");
        }

        if (trade.ID <= 0) {
            trade.ID = getNextId();
            log.debug("Assigned id: {}", trade.ID);
        } else {
            if (get(trade.ID) != null) {
                log.warn("Trade with id:{} already present, updating it", trade.ID);
            }
        }
        trade.checkNulls();
        // TODO: Implement validation before saving
        /*
        if (trade.isValid() == false)
        {
            log.warn("Ignoring trade {}-{}. Validation failed:{0}",
                    trade.ID,
                    trade.symbol,
                    String.join("; ", trade.Validate().Errors.Select(x => x.ErrorMessage)));
            return null;
        }
        */
        db.save(trade.ID, trade);
        symbolIndex.index(trade.symbol, trade.ID);
        var closureDate = trade.getDateOfClosure();
        if (closureDate.isPresent()) {
            dateIndex.index(closureDate.get(), trade.ID);
        }
        return trade.ID;
    }

    public boolean delete(int id) {
        if (id < 1) {
            throw new IllegalArgumentException("id");
        }
        return db.delete(id);
    }

    public void close() {
        db.close();
        countersDb.close();
        symbolIndex.close();
        dateIndex.close();
    }

    private int getNextId() {
        var counter = countersDb.find(TRADE_KEY_COUNTER);
        var newValue = 1;
        if (counter.isPresent()) {
            countersDb.save(TRADE_KEY_COUNTER, counter.get() + 1);
            newValue = counter.get() + 1;
        }
        countersDb.save(TRADE_KEY_COUNTER, newValue);
        return newValue;
    }
}