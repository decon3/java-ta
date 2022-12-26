package me.sk.ta.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.sk.ta.domain.Trade;
import me.sk.ta.domain.TradeContract;
import me.sk.ta.domain.TradingAccount;
import me.sk.ta.domain.TradingChargesCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

@Component
public class TradingAccountRepository {
    private static final Logger log = LoggerFactory.getLogger(TradingAccountRepository.class);

    final String ACCOUNT_ID_COUNTER = "ACCOUNT_ID_COUNTER";
    final MVStoreIndex<LocalDate, Integer> dateIndex;
    final KvDb<String, Integer> countersDb;
    final KvDb<Integer, TradingAccount> db;
    String dbPath;
    final TradingChargesCalculator chargesCalculator;

    public TradingAccountRepository(@Value("${db.conn.current.account}") String dbPath, ObjectMapper serializer, TradingChargesCalculator tc) {
        if (dbPath == null) {
            throw new IllegalArgumentException("dbPath has not been initialized");
        }
        dbPath = Path.of(dbPath).resolve("live").toString();
        this.dbPath = dbPath;
        this.chargesCalculator = tc;
        db = new MVStoreRepo<Integer, TradingAccount>(dbPath, "trading_account", Integer.class, Trade.class, serializer);
        dateIndex = new MVStoreIndex<>(dbPath, "DATE_INDEX", MVStoreIndex.IndexingStrategy.PostfixValue, LocalDate.class, Integer.class, "~~~", serializer);
        countersDb = new MVStoreRepo<String, Integer>(dbPath, "counters", String.class, Integer.class, serializer);
    }

    public void postSale(int tradeId, TradeContract contract)
    {
        if (contract.id() <= 0)
        {
            throw new IllegalArgumentException("contract should contain a valid ID");
        }
        if (contract.isSale() == false) {
            throw new IllegalArgumentException("not a sale");
        }
        log.trace("Entered - tradeId:{} amt:{} charges:{}", tradeId, contract.totalPrice(), contract.charges());
        var account = getAccount();
        log.trace("Balance:{}", account.cashBalance);
        account.recordSale(contract.totalPrice(), contract.charges(), contract.date(), tradeId, contract.id());
        saveOrUpdate(account);
        log.trace("Exit - balance:{}", account.cashBalance);
    }
    public void postPurchase(int tradeId, TradeContract contract)
    {
        if (contract.id() <= 0)
        {
            throw new IllegalArgumentException("contract should contain a valid ID");
        }
        if (contract.isSale()) {
            throw new IllegalArgumentException("not a purchase");
        }
        log.trace("Entered - tradeId:{} amt:{} charges:{}", tradeId, contract.totalPrice(), contract.charges());
        var account = getAccount();
        log.trace("Balance:{}", account.cashBalance);
        account.recordPurchase(contract.totalPrice(), contract.charges(), contract.date(), tradeId, contract.id());
        saveOrUpdate(account);
        log.trace("Exit - balance:{}", account.cashBalance);
    }

    public void deleteTrade(int tradeId, Collection<Integer> contractIds)
    {
        if (contractIds == null) {
            throw new IllegalArgumentException("contractIds");
        }
        log.trace("Entered - tradeId:{} ", tradeId);
        var account = getAccount();
        log.trace("Balance:{}", account.cashBalance);
        account.deleteTrade(tradeId, contractIds);
        saveOrUpdate(account);
        log.trace("Exit - balance:{}", account.cashBalance);
    }
    public void closeTrade(int tradeId, Collection<Integer> contractIds)
    {
        if (contractIds == null) {
            throw new IllegalArgumentException("contractIds");
        }
        log.trace("Entered - tradeId:{} ", tradeId);
        var account = getAccount();
        log.trace("Balance:{}", account.cashBalance);
        account.closeTrade(tradeId, contractIds);
        saveOrUpdate(account);
        log.trace("Exit - balance:{}", account.cashBalance);
    }

    public TradingAccount getAccount() {
        var account = get(1);
        if (account.isEmpty())
            account = Optional.of(new TradingAccount());
        return account.get();
    }

    public Optional<TradingAccount> get(int id) {
        if (id < 1) {
            throw new IllegalArgumentException("id");
        }
        return db.find(id);
    }


    // TODO commit write transaction
    public void saveOrUpdate(TradingAccount account) {
        if (account == null) {
            throw new IllegalArgumentException("account");
        }

        db.save(1, account);
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
        dateIndex.close();
    }

    public void drop() {
        db.drop();
        countersDb.drop();
        dateIndex.drop();
    }

    // TODO Account keyed by year? or number
    private int getNextId() {
        var counter = countersDb.find(ACCOUNT_ID_COUNTER);
        var newValue = 1;
        if (counter.isPresent()) {
            newValue = counter.get();
            newValue += 1;
        }
        countersDb.save(ACCOUNT_ID_COUNTER, newValue);
        return newValue;
    }
}