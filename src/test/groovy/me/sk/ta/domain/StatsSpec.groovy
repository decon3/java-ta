package me.sk.ta.domain

import me.sk.ta.TaApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.lang.Title

@SpringBootTest("webEnvironment=NONE")
@ContextConfiguration(classes = [TaApplication.class])
@ActiveProfiles("dev")
@Title("Testing Stats")
class StatsSpec extends Specification {

    BuyAnalysis ba

    @Autowired
    ConfigurableApplicationContext ctx
    @Autowired
    TradingChargesCalculator tcCalculator

    void setup() {
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

    def "CheckAutowiring"() {
        given: "Autowired configuration"

        expect:
        tcCalculator != null
        ctx != null
    }

    def "Build"() {
        given: "two winning trades and one loss"

        var t = Trade.initiateTrade("INFY", ba, tcCalculator)
        t.Buy(1, 100, 335.35, Utils.UtcToday().minusDays(5), false)
        t.Sell(1, 100, 345.75, Utils.UtcToday(), false)

        var losingTrade = Trade.initiateTrade("INFY", ba, tcCalculator)
        losingTrade.Buy(1, 100, 335.35, Utils.UtcToday().minusDays(5), false)
        losingTrade.Sell(1, 100, 320.75, Utils.UtcToday(), false)

        var t3 = Trade.initiateTrade("INFY", ba, tcCalculator)
        t3.Buy(1, 100, 330.35, Utils.UtcToday().minusDays(5), false)
        t3.Sell(1, 100, 345.75, Utils.UtcToday(), false)

        when: "stats are collected"

        var stats = Stats.build(Utils.UtcToday().minusDays(10), Utils.UtcToday(), List.of(t, losingTrade, t3))
        System.out.println("Profit:" + stats.grossProfit)
        System.out.println("Loss:" + stats.grossLoss)

        then: "longest winning streak should be 1 and losing streak should be zero"
        stats.longestLossStreak == 0
        stats.longestWinStreak == 1
        t.position() == 100;
    }

    def "SplitIntoStreaks"() {
        given: "a streak of two wins and another of 3 losses"
        var list = List.of(true, true, false, false, false, true, false, false)

        when: "asked for streaks of wins"
        var streaks = Stats.splitIntoStreaks(list.stream(), true)

        then: "shows one streak of length 2"
        streaks.size() == 1
        streaks.get(0) == 2
    }
}
