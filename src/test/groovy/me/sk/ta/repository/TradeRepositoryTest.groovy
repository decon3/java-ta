package me.sk.ta.repository

import com.fasterxml.jackson.databind.ObjectMapper
import me.sk.ta.TaApplication
import me.sk.ta.domain.ADRating
import me.sk.ta.domain.BreakoutPattern
import me.sk.ta.domain.BuyAnalysis
import me.sk.ta.domain.MarketTrend
import me.sk.ta.domain.Trade
import me.sk.ta.domain.TradeContract
import me.sk.ta.domain.TradingChargesCalculator
import me.sk.ta.domain.Trend
import me.sk.ta.domain.Utils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@SpringBootTest("webEnvironment=NONE")
@ContextConfiguration(classes = [TaApplication.class])
@ActiveProfiles("dev")
class TradeRepositoryTest extends Specification {
    private static final Logger log = LoggerFactory.getLogger(BuyAnalysis.class);

    @Autowired
    ConfigurableApplicationContext ctx;

    TradeRepository repo;
    BuyAnalysis ba;
    @Autowired
    TradingChargesCalculator tcCalculator


    def "Find"() {
    }

    def "Get"() {
    }

    def "GetOpenTrade"() {
    }

    def "GetOpenTrades"() {
    }

    def "GetClosedTrades"() {
    }

    def "spring registers time module with jackson serializer in the background"() {
        given:
        var som = ctx.getBean(ObjectMapper.class);
        var mom = new ObjectMapper();
        expect:
        som.writeValueAsString(Utils.UtcNow()).length() > 0
        mom.writeValueAsString(Utils.UtcNow()).length() > 0
    }

    def "Saves an open trade"() {
        given:
        var t = Trade.initiateTrade("INFY", ba, tcCalculator)
        t.Buy(1, 100, 334.37, Utils.UtcToday().minusDays(5), false)
        when:
        var id = repo.saveOrUpdate(t);
        and:
        var found = repo.get(id)
        then:
        found.isPresent() && found.get().analysisHistory == t.analysisHistory;
    }

    def "Trade is deserialized correctly"() {
        given:
        var t = Trade.initiateTrade("INFY", ba, tcCalculator)
        t.Buy(1, 100, 334.37, Utils.UtcToday().minusDays(5), false)
        t.Sell(1, 100, 345.75, Utils.UtcToday(), false)
        when:
        var bean = ctx.getBean(ObjectMapper.class);
        var serializedJson = bean.writeValueAsString(t);
        var t2 = (Trade)bean.readValue(serializedJson, Trade.class);
        var deserializedJson = bean.writeValueAsString(t2);
        log.debug(serializedJson);
        log.debug(deserializedJson);
        then:
        serializedJson == deserializedJson;
    }

    def "TradeContract is deserialized correctly"() {
        given:
        var t = Trade.initiateTrade("INFY", ba, tcCalculator)
        t.Buy(1, 100, 334.37, Utils.UtcToday().minusDays(5), false)
        t.Sell(1, 100, 345.75, Utils.UtcToday(), false)
        when:
        var bean = ctx.getBean(ObjectMapper.class);
        var serializedJson = bean.writeValueAsString(t.tradeHistory.get(0));
        var t2 = (TradeContract)bean.readValue(serializedJson, TradeContract.class);
        var deserializedJson = bean.writeValueAsString(t2);
        log.debug(serializedJson);
        log.debug(deserializedJson);
        log.debug(t.tradeHistory.get(0).toString());
        then:
        serializedJson == deserializedJson;
    }

    def "SaveOrUpdate saves all the properties of the trade objects"() {
        given: "two winning trades and one loss"

        var wt1 = Trade.initiateTrade("INFY", ba, tcCalculator)
        wt1.Buy(1, 100, 335.35, Utils.UtcToday().minusDays(5), false)
        wt1.Sell(1, 100, 345.75, Utils.UtcToday(), false)

        var losingTrade = Trade.initiateTrade("INFY", ba, tcCalculator)
        losingTrade.Buy(1, 100, 335.35, Utils.UtcToday().minusDays(5), false)
        losingTrade.Sell(1, 100, 320.75, Utils.UtcToday(), false)

        var wt2 = Trade.initiateTrade("INFY", ba, tcCalculator)
        wt2.Buy(1, 100, 330.35, Utils.UtcToday().minusDays(5), false)
        wt2.Sell(1, 100, 345.75, Utils.UtcToday(), false)

        when: "they are saved"
        repo.saveOrUpdate(wt1);
        repo.saveOrUpdate(losingTrade);
        repo.saveOrUpdate(wt2);

        then: "the retrieved objects match the original"
        repo.get(wt1.ID).get() == wt1
        and:
        repo.get(losingTrade.ID).get() == losingTrade
        and:
        repo.get(wt2.ID).get() == wt2
    }

    def "Delete"() {
    }

    def "Close"() {
    }

    void setup() {
        repo = ctx.getBean(TradeRepository.class);
        log.debug("Instantiated the repository");
        ba = BuyAnalysis.builder()
                .earningsDate(Utils.UtcToday().plusDays(20))
                .build()

        ba.priceLevels(9000000, 0.5, 320, 340, 320 * .12, 340 * 0.93)
                .breakoutDetails("c", BreakoutPattern.DoubleBottom, 3, 290)
                .risk(0.9, 0.3, 100000, MarketTrend.Rally)
                .largePlayers(12, 34, 2.3)
                .scoresByGurus(80, 80, 80, 80)
                .wonScores(80, ADRating.C, 80, 80, 90)
                .onBalanceValue(Trend.Up, true, 1000000)
                .adRating(Trend.Up, true)
                .moneyFlow(Trend.Up, true)
                .movingAverages(340, 330, 300)
    }
    void cleanup() {
        if (repo != null)
        {
            repo.close();
            repo.drop();
        }
    }
}
