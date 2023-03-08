package me.sk.ta.domain;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TradingChargesCalculator {
    @Autowired
    @Getter
    TradingRatesConfiguration rates;

    public TradeCharges calculatePriceAndCost(double price, boolean isIntraDay, boolean isSale) {

        var tc = new TradeCharges()
                .exchange(isIntraDay ? (price * rates.IntraDayExchangeFee) : (price * rates.ExchangeFee))
                .sebi(isIntraDay ? (price * rates.IntraDaySebiFee) : (price * rates.SebiFee))
                .brokerage(isIntraDay ? rates.IntraDayBrokerageCost : rates.BrokerageCost)
                .demat(isSale ? rates.Demat : 0.00);
        tc.gst((tc.demat() + tc.exchange() + tc.sebi() + tc.brokerage()) * rates.GstRate);
        tc.stampDuty(isIntraDay ? price * rates.IntraDayStampDutyRate : price * rates.StampDutyRate);
        tc.stampDuty(tc.stampDuty() < 100 ? tc.stampDuty() : 100);
        tc.stt(isIntraDay ? (price * rates.IntraDaySttRate) : (price * rates.SttRate));
        if (isIntraDay && isSale == false) {
            // stt is not charged for intraday purchases
            tc.stt(0.00);
        }
        tc.roundoff();
        return tc;
    }

    public TradeCharges calculateCostOfSale(double price, boolean isIntraDay) {
        return calculatePriceAndCost(price, isIntraDay, true);
    }

    public TradeCharges calculateCostOfPurchase(double price, boolean isIntraDay) {
        return calculatePriceAndCost(price, isIntraDay, false);
    }

    public TradeCharges estimateCostOfTrade(double price, boolean isIntraDay) {
        return calculateCostOfPurchase(price, isIntraDay).add(calculateCostOfSale(price, isIntraDay));
    }
}
