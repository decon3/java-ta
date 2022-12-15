package me.sk.ta.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TradingAccount {
    private static final Logger log = LoggerFactory.getLogger(TradingAccount.class);
    public double cashBalance;
    public List<TradingAccountEntry> History;

    public TradingAccount() {
        initializeHistoryIfNecessary();
    }

    public AccountPeriodSummary reportSummary(LocalDate from, LocalDate to) {
        initializeHistoryIfNecessary();
        return new AccountPeriodSummary()
                .from(from)
                .to(to)
                .opening(getClosingBalanceOn(from.minusDays(-1)))
                .closing(getClosingBalanceOn(to))
                .invested(History.stream()
                        .filter(x -> x.date().isAfter(to) == false && x.transType() == AccountTransactionType.Trade && x.isTradeClosed() == false)
                        .mapToDouble(TradingAccountEntry::amount)
                        .sum())
                .capitalInfused(History.stream()
                        .filter(x -> Utils.IsWithinRange(x.date(), from, to) && x.transType() == AccountTransactionType.Capital && x.amount() > 0)
                        .mapToDouble(TradingAccountEntry::amount)
                        .sum())
                .capitalWithdrawn(History.stream()
                        .filter(x -> Utils.IsWithinRange(x.date(), from, to)  && x.transType() == AccountTransactionType.Capital && x.amount() < 0)
                        .mapToDouble(TradingAccountEntry::amount)
                        .sum());
    }

    public void deleteTillAndIncluding(LocalDate date) {
        initializeHistoryIfNecessary();
        if (History.size() > 0) {
            History.removeAll(History.stream().filter(x -> x.date().isAfter(date) == false).toList());
        }
    }

    public double getClosingBalanceOn(LocalDate date) {
        initializeHistoryIfNecessary();
        return History.stream()
                .filter(x -> x.date().isAfter(date) == false)
                .mapToDouble(TradingAccountEntry::amount)
                .sum();
    }

    public void addCapital(double amount, LocalDate date) {
        if (amount <= 0) {
            return;
        }
        recordAccountEntry(date, amount, AccountTransactionType.Capital);
    }

    public void withdrawCapital(double amount, LocalDate date) {
        if (amount <= 0) {
            return;
        }
        recordAccountEntry(date, -1 * amount, AccountTransactionType.Capital);
    }

    public void recordSale(double amount, double charges, LocalDate date, int tradeid, int contractId) {
        if (amount <= 0) {
            return;
        }
        var revenue = amount - charges;
        recordAccountEntry(date, revenue, AccountTransactionType.Trade, generateBusinessTransactionId(tradeid, contractId));
    }

    public void recordPurchase(double amount, double charges, LocalDate date, int tradeid, int contractId) {
        if (amount <= 0) {
            return;
        }
        var cost = amount + charges;
        recordAccountEntry(date, -1 * cost, AccountTransactionType.Trade, generateBusinessTransactionId(tradeid, contractId));
    }

    public void closeTrade(int tradeId, Collection<Integer> contractIds) {
        for (var id : contractIds) {
            final var businessTansId = generateBusinessTransactionId(tradeId, id);
            final var entry = History.stream()
                    .filter(x -> x.businessTransactionId().equals(businessTansId))
                    .findFirst();
            if (entry.isEmpty() == false) {
                final var closed = entry.get().closeTrade();
                History.set(History.indexOf(entry), closed);
            }
        }
    }

    public void deleteTrade(int tradeId, Collection<Integer> contractIds) {
        for (var id : contractIds) {
            deleteAccountEntry(generateBusinessTransactionId(tradeId, id));
        }
    }

    private String generateBusinessTransactionId(int tradeId, int contractId) {
        return tradeId + "-" + contractId;
    }

    synchronized private boolean deleteAccountEntry(String businessTansId) {
        if (History == null || History.size() == 0) {
            throw new RuntimeException("Nothing to reverse");
        }
        if (businessTansId == null || businessTansId.isEmpty() || businessTansId.isBlank()) {
            throw new IllegalArgumentException("businessTansId");
        }

        if (History.stream().anyMatch(x -> x.businessTransactionId().equals(businessTansId)) == false) {
            throw new RuntimeException("No entry found. Business Trans Id:" + businessTansId);
        }

        var oldEntry = History.stream()
                .filter(x -> x.businessTransactionId().equals(businessTansId))
                .findFirst();

        if (oldEntry.isPresent())
        {
            History.remove(oldEntry.get());
            cashBalance -= oldEntry.get().amount();
            return true;
        }
        else {
            return false;
        }
    }

    private void recordAccountEntry(LocalDate date, double amount, AccountTransactionType type) {
        recordAccountEntry(date, amount, type, "");
    }

    synchronized private void recordAccountEntry(LocalDate date, double amount, AccountTransactionType type, String businessTansId) {
        initializeHistoryIfNecessary();
        double deltaBalance = 0.00;
        if (businessTansId != null &&
                businessTansId.isBlank() == false &&
                businessTansId.isEmpty() == false &&
                History.stream().anyMatch(x -> x.businessTransactionId().equals(businessTansId))) {
            // update to an older entry
            var oldEntry = History.stream()
                    .filter(x -> x.businessTransactionId().equals(businessTansId))
                    .findFirst()
                    .orElse(TradingAccountEntry.Empty());
            deltaBalance = amount - oldEntry.amount();
            History.removeAll(History.stream().filter(x -> x.businessTransactionId().equals(businessTansId)).toList());
        } else {
            deltaBalance = amount;
        }

        History.add(new TradingAccountEntry(type, businessTansId, amount, date, false));
        cashBalance += deltaBalance;
    }

    synchronized private void initializeHistoryIfNecessary() {
        if (History == null) {
            History = new ArrayList<TradingAccountEntry>();
        }
    }
}
