package me.sk.ta.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
@ConfigurationProperties(prefix = "ta.rates")
@Validated
public class TradingRatesConfiguration {
    public double IntraDayBrokerageCost = 20.0;
    public double BrokerageCost = 0.00;
    public double SttRate = 0.001;
    public double IntraDaySttRate = 0.00025;
    public double Demat = 0.00;
    public double IntraDayStampDutyRate = 0.0001;
    public double StampDutyRate = 0.0001;
    public double IntraDayExchangeFee = 0.0000015;
    public double ExchangeFee = 0.0000015;
    public double IntraDaySebiFee = 0.0000325;
    public double SebiFee = 0.0000325;
    public double GstRate = 0.18;
}
