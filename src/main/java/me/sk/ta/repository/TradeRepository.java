package me.sk.ta.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.sk.ta.domain.Trade;
import me.sk.ta.domain.TradingAccount;
import me.sk.ta.domain.TradingChargesCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class TradeRepository {
    private static final Logger log = LoggerFactory.getLogger(TradeRepository.class);
    static final String TRADE_DB = "trade";
    static final String ARCHIVE_DB = "tradeArchive";
    static final String ACCOUNT_DB = "account";

    final String TRADE_KEY_COUNTER = "TRADE_ID_COUNTER";
    final MVStoreIndex<String, Integer> symbolIndex;
    final MVStoreIndex<LocalDate, Integer> dateIndex;
    final KvDb<String, Integer> countersDb;
    final KvDb<Integer, Trade> db;
    String dbPath;
    final TradingChargesCalculator chargesCalculator;

    public TradeRepository(@Value("${db.conn.current.trade}") String dbPath, ObjectMapper serializer, TradingChargesCalculator tc) {
        if (dbPath == null) {
            throw new IllegalArgumentException("dbPath has not been initialized");
        }
        this.dbPath = dbPath;
        this.chargesCalculator = tc;
        db = new MVStoreRepo<Integer, Trade>(dbPath, "trade", Integer.class, Trade.class, serializer);
        symbolIndex = new MVStoreIndex<>(dbPath, "SYMBOL_INDEX", MVStoreIndex.IndexingStrategy.PostfixValue, String.class, Integer.class, "~~~", serializer);
        dateIndex = new MVStoreIndex<>(dbPath, "DATE_INDEX", MVStoreIndex.IndexingStrategy.PostfixValue, LocalDate.class, Integer.class, "~~~", serializer);
        countersDb = new MVStoreRepo<String, Integer>(dbPath, "counters", String.class, Integer.class, serializer);
    }

    public Trade find(int id) {
        if (id < 1) {
            throw new IllegalArgumentException("id");
        }
        return db.get(id);
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
        t.get().tcCalculator = this.chargesCalculator;
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

    public void drop() {
        db.drop();
        countersDb.drop();
        symbolIndex.drop();
        dateIndex.drop();
    }

    private int getNextId() {
        var counter = countersDb.find(TRADE_KEY_COUNTER);
        var newValue = 1;
        if (counter.isPresent()) {
            newValue = counter.get();
            newValue += 1;
        }
        countersDb.save(TRADE_KEY_COUNTER, newValue);
        return newValue;
    }
}