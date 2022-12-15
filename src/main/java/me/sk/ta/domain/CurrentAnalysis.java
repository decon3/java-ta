package me.sk.ta.domain;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.LocalDate;

@Getter @SuperBuilder
public class CurrentAnalysis extends AnalysisAbstract {
    private static final Logger log = LoggerFactory.getLogger(CurrentAnalysis.class);
    private LocalDate date;
    public void setDate(LocalDate date) {
        this.date = date;
    }

    private double price;

    public CurrentAnalysis setPrice(double price) {
        this.price = price;
        return this;
    }

    public static CurrentAnalysis from(BuyAnalysis b) {
        return CurrentAnalysis.builder()
                .date(Utils.UtcToday())
                .alpha(b.alpha)
                .beta(b.beta)
                .debt(b.beta)
                .stopLoss(b.stopLoss)
                .dma20(b.dma20)
                .dma50(b.dma50)
                .dma100(b.dma100)
                .obvValue(b.obvValue)
                .obvTrend(b.obvTrend)
                .obvPositive(b.obvPositive)
                .cmfTrend(b.cmfTrend)
                .cmfPositive(b.cmfPositive)
                .adTrend(b.adTrend)
                .adPositive(b.adPositive)
                .comments(b.comments)
                .wonADRating(b.wonADRating)
                .wonEpsRating(b.wonEpsRating)
                .wonMasterRating(b.wonMasterRating)
                .wonRSRating(b.wonRSRating)
                .guruScoreBg(b.guruScoreBg)
                .guruScoreJos(b.guruScoreJos)
                .guruScoreWb(b.guruScoreWb)
                .guruScoreWon(b.guruScoreWon)
                .guruScorePl(b.guruScorePl)
                .marketTrend(b.marketTrend)
                .increaseInFunds(b.increaseInFunds)
                .increaseInFundHoldings(b.increaseInFundHoldings)
                .floatingShares(b.floatingShares)
                .build();
    }

    private class CurrentAnalysisValidator implements Validator {

        @Override
        public boolean supports(Class<?> clazz) {
            return CurrentAnalysis.class.isAssignableFrom(clazz);
        }

        @Override
        public void validate(Object target, Errors e) {
            var ca = (CurrentAnalysis) target;
            if (ca.stopLoss <= 0) {
                e.rejectValue("stopLoss", "Mandatory");
            }
            if (ca.dma20 <= 0) {
                e.rejectValue("dma20", "Mandatory");
            }
            if (ca.dma50 <= 0) {
                e.rejectValue("dma50", "Mandatory");
            }
            if (ca.dma100 <= 0) {
                e.rejectValue("dma100", "Mandatory");
            }
            if (ca.price <= 0) {
                e.rejectValue("price", "Mandatory");
            }
            if (ca.guruScoreWon <= 0) {
                e.rejectValue("guruScoreWon", "Mandatory", "William O'Neil score");
            }
            if (ca.guruScorePl <= 0) {
                e.rejectValue("guruScorePl", "Mandatory", "Peter Lynch score");
            }
            if (ca.guruScoreWb <= 0) {
                e.rejectValue("guruScoreWb", "Mandatory", "Warren Buffet score");
            }
            if (ca.guruScoreBg <= 0) {
                e.rejectValue("guruScoreBg", "Mandatory", "Benjamin Graham score");
            }
            if (ca.guruScoreJos <= 0) {
                e.rejectValue("guruScoreJos", "Mandatory", "James O'Shauganessy score");
            }
            if (ca.adTrend == Trend.None) {
                e.rejectValue("adTrend", "Mandatory");
            }
            if (ca.obvTrend == Trend.None) {
                e.rejectValue("obvTrend", "Mandatory");
            }
            if (ca.cmfTrend == Trend.None) {
                e.rejectValue("cmfTrend", "Mandatory");
            }
            if (ca.marketTrend == MarketTrend.None) {
                e.rejectValue("marketTrend", "Mandatory");
            }
        }
    }
}