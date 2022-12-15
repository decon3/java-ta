package me.sk.ta.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter @Setter @Accessors(fluent = true, chain = true)
public class TradeCharges
{
    private double brokerage;
    private double sebi;
    private double exchange;
    private double demat;
    private double stampDuty;
    private double stt;
    private double gst;
    public double total()
    {
        return brokerage + sebi + exchange + demat + stampDuty + stt + gst;
    }
    public void roundoff()
    {
        brokerage = Utils.round(brokerage, 2);
        sebi = Utils.round(sebi, 2);
        exchange = Utils.round(exchange, 2);
        demat = Utils.round(demat, 2);
        stampDuty = Utils.round(stampDuty, 2);
        stt = Utils.round(stt, 2);
        gst = Utils.round(gst, 2);
    }

    @Override
    public java.lang.String toString() {
        return String.format("Brok:%.2f SEBI:%.2f Exch:%.2f Demat:%.2f Stamp:%.2f STT:%.2f GST:%.2f. Total:%.2f",
                brokerage,
                sebi,
                exchange,
                demat,
                stampDuty,
                stt,
                gst,
                total());
    }

    public TradeCharges add(TradeCharges o)
    {
        return new TradeCharges()
                .brokerage(brokerage + o.brokerage)
                .sebi(sebi + o.sebi)
                .exchange(exchange + o.exchange)
                .demat(demat + o.demat)
                .stampDuty(stampDuty + o.stampDuty)
                .stt(stt + o.stt)
                .gst(gst + o.gst);
    }
} 