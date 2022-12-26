package me.sk.ta.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.sk.ta.domain.Trade;
import me.sk.ta.domain.TradingChargesCalculator;
import org.h2.mvstore.tx.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Component
public class TradeArchiveRepository {
    private static final Logger log = LoggerFactory.getLogger(TradeArchiveRepository.class);
    final MVStoreIndex<String, Integer> symbolIndex;
    final MVStoreIndex<LocalDate, Integer> dateIndex;
    final KvDb<String, Integer> countersDb;
    final KvDb<Integer, Trade> db;
    String dbPath;
    final TradingChargesCalculator chargesCalculator;

    public TradeArchiveRepository(@Value("${db.conn.archive.trade}") String dbPath, ObjectMapper serializer, TradingChargesCalculator tc) {
        if (dbPath == null) {
            throw new IllegalArgumentException("dbPath has not been initialized");
        }
        this.dbPath = Path.of(dbPath).resolve("archive").toString();
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

    public List<Trade> where(Predicate<Trade> predicate) {
        return db.findAll(x -> {
            return predicate.test(x) ? Optional.of(x) : Optional.empty();
        });
    }

    public Optional<Trade> get(int id) {
        if (id < 1) {
            throw new IllegalArgumentException("id");
        }
        return db.find(id);
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

    // TODO commit write transaction
    public int save(Trade trade) {
        if (trade == null) {
            throw new IllegalArgumentException("trade");
        }

        if (trade.ID <= 0) {
            throw new IllegalArgumentException("Trade.ID has to be greater than zero");
        }

        Transaction tx1 = null, tx2 = null, tx3 = null;
        boolean res1 = true, res2 = true, res3 = true;

        try {
            tx1 = db.beginTransaction();
            tx2 = symbolIndex.beginTransaction();
            tx3 = dateIndex.beginTransaction();

            res1 = db.save(trade.ID, trade);
            res2 = symbolIndex.index(trade.symbol, trade.ID);
            var closureDate = trade.getDateOfClosure();
            if (closureDate.isPresent()) {
                res3 = dateIndex.index(closureDate.get(), trade.ID);
            }
            if (res1 && res2 && res3) {
                tx1.commit();
                tx2.commit();
                tx3.commit();
            } else {
                tx1.rollback();
                tx2.rollback();
                tx3.rollback();
                trade.ID = 0;
            }
        } catch (Exception ex) {
            log.error("An exception occurred: {}", ex);
            if (tx1 != null) tx1.rollback();
            if (tx2 != null) tx2.rollback();
            if (tx3 != null) tx3.rollback();
            trade.ID = 0;
            return 0;
        }
        return trade.ID;
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
}