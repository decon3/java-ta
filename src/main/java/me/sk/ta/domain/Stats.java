package me.sk.ta.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Getter
@Setter
@Accessors(fluent = true, chain = true)
public class Stats {
    private static final Logger log = LoggerFactory.getLogger(Stats.class);
    public LocalDate from;
    public LocalDate to;
    public double winRatio;
    public double payoffRatio;
    public double commissionRatio;
    public double profitFactor;
    public double grossProfit;
    public double grossLoss;
    public double commissionAndTaxes;
    public double largestWin;
    public double largestLoss;
    public double averageWin;
    public double averageLoss;
    public int longestWinStreak;
    public int longestLossStreak;
    public double averageWinStreak;
    public double averageLossStreak;
    public double largestDrawdown;
    public double averageDrawdown;

    public static Stats build(LocalDate from, LocalDate to, List<Trade> trades) {
        Supplier<Stream<Trade>> closedTrades = () -> trades.stream().filter(x -> x.isClosed());
        Supplier<Stream<Trade>> wins = () -> closedTrades.get().filter(x -> x.realisedPnl() >= 0);
        Supplier<Stream<Trade>> losses = () -> closedTrades.get().filter(x -> x.realisedPnl() < 0);

        log.trace("Closed: {}, Wins:{} Losses:{}", closedTrades.get().count(), wins.get().count(), losses.get().count());

        if (closedTrades.get().count() == 0) {
            return new Stats();
        }
        var stats = new Stats()
                .from(from)
                .to(to)
                .longestLossStreak(getMaxConsecutiveLosses(closedTrades))
                .longestWinStreak(getMaxConsecutiveWins(closedTrades))
                .averageLossStreak(getAverageConsecutiveLosses(closedTrades))
                .averageWinStreak(getAverageConsecutiveWins(closedTrades));

        if (wins.get().count() > 0) {
            stats.grossProfit = wins.get().mapToDouble(x -> x.grossPnl()).sum();
            stats.largestWin = wins.get().sorted(Comparator.comparing(x -> x.realisedPnl()))
                    .skip(wins.get().count() - 1)
                    .findFirst()
                    .get()
                    .realisedPnl();
            stats.winRatio = wins.get().count() / closedTrades.get().count();
            stats.commissionRatio = closedTrades.get().mapToDouble(x -> x.totalCharges()).sum() /
                    wins.get().mapToDouble(x -> x.realisedPnl() + x.totalCharges()).sum();
            stats.averageWin = wins.get().mapToDouble(x -> x.realisedPnl()).average().getAsDouble();
        }
        if (losses.get().count() > 0) {
            stats.grossLoss = losses.get().mapToDouble(x -> x.grossPnl()).sum();
            stats.largestLoss = losses.get().sorted(Comparator.comparing(x -> x.realisedPnl())).findFirst().get().realisedPnl();
            stats.averageLoss = losses.get().mapToDouble(x -> x.realisedPnl()).average().getAsDouble();
            stats.profitFactor = wins.get().mapToDouble(x -> x.realisedPnl()).sum() / losses.get().mapToDouble(x -> x.realisedPnl()).sum();
        }
        if (wins.get().count() > 0 && losses.get().count() > 0) {
            stats.payoffRatio = wins.get().mapToDouble(x -> x.realisedPnl()).sum() / losses.get().mapToDouble(x -> x.realisedPnl()).sum() * -1;
        }

        log.debug("{}", stats);
        return stats;
    }

    public static int getMaxConsecutiveWins(Supplier<Stream<Trade>> trades) {
        return measureStreaks(trades, true, true);
    }

    public static int getMaxConsecutiveLosses(Supplier<Stream<Trade>> trades) {
        return measureStreaks(trades, false, true);
    }

    public static int getAverageConsecutiveWins(Supplier<Stream<Trade>> trades) {
        return measureStreaks(trades, true, false);
    }

    public static int getAverageConsecutiveLosses(Supplier<Stream<Trade>> trades) {
        return measureStreaks(trades, false, false);
    }

    public static int measureStreaks(Supplier<Stream<Trade>> trades, boolean countWins, boolean max) {
        if (trades.get().count() == 0) {
            return 0;
        }

        Supplier<Stream<Boolean>> booleans = () -> trades.get().map(x -> x.realisedPnl() > 0);

        if (booleans.get().count() == 0) {
            return 0;
        }

        var counts = splitIntoStreaks(booleans.get(), countWins);
        if (counts.size() == 0)
        {
            return 0;
        }
        return max ? counts.stream().mapToInt(x -> x).max().getAsInt() : (int)counts.stream().mapToInt(x -> x).average().getAsDouble();
    }

    public static List<Integer> splitIntoStreaks(final Stream<Boolean> s, boolean criteria) {
        List<Boolean> l = s.toList();
        var list = new ArrayList<Integer>();
        var prev = l.stream().findFirst().get();
        var count = 0;
        for(var item: l) {
            if (item == prev) {
                count += 1;
                continue;
            }
            if (prev == criteria && count > 1) {
                list.add(count);
            }
            count = 1;
            prev = item;
        }
        if (prev == criteria && count > 0) {
            list.add(count);
        }
        return list;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append(String.format("From:%s to: %s\n", from.toString(), to.toString()));
        sb.append(String.format("winRatio: %.3f\n", winRatio));
        sb.append(String.format("payoffRatio: %.3f\n", payoffRatio));
        sb.append(String.format("commissionRatio: %.3f\n", commissionRatio));
        sb.append(String.format("LongestLosingStreak: %d\n", longestLossStreak));
        sb.append(String.format("LongestWinningStreak: %d\n", longestWinStreak));
        sb.append(String.format("largestWin: %.2f\n", largestWin));
        sb.append(String.format("largestLoss: %.2f\n", largestLoss));
        return sb.toString();
    }
}
