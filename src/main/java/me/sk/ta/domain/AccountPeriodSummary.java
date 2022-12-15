package me.sk.ta.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Getter @Setter @Accessors(fluent = true, chain = true)
public class AccountPeriodSummary {
    private double opening;
    private double closing;
    private double invested;
    private double grossProfit;
    private double grossLoss;
    private int LosingTrades;
    private int winningTrades;
    private double capitalInfused;
    private double capitalWithdrawn;
    private LocalDate from;
    private LocalDate to;


    public double netPnL() {
        return grossProfit + grossLoss;
    }
    public double capitalForPositionCalculation() {
        return opening + capitalInfused - capitalWithdrawn;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append(String.format("%s to %s", from, to));
        sb.append(String.format("Opening Bal:%.2f. Closing:%.2f Invested:%.2f", opening, closing, invested));
        sb.append(String.format("Capintal IN:%.2f, OUT:%.2f", capitalInfused, capitalWithdrawn));
        sb.append(String.format("PNL: %.2f. Gross Profit:%.2f (%d trades) GrossLoss:%.2f(%d trades)",
                netPnL(),
                grossProfit,
                winningTrades,
                grossLoss,
                LosingTrades));
        return sb.toString();
    }

}
