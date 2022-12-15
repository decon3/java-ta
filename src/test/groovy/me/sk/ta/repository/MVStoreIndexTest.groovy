package me.sk.ta.repository

import me.sk.ta.domain.Utils
import spock.lang.Specification

import java.time.LocalDate

class MVStoreIndexTest extends Specification {
    MVStoreIndex<LocalDate, Integer> dateIndex;
    MVStoreIndex<String, Integer> symbolIndex;

    def "Find"() {
    }

    def "TestFind"() {
    }

    def "PostfixWithCount allows duplicate keys AND duplicate values"() {
        given:
        var sindex = new MVStoreIndex<>(
                "db\\trade\\index",
                "sindex",
                MVStoreIndex.IndexingStrategy.PostfixWithCount,
                String.class,
                Integer.class,
                "!@#\$");
        sindex.clear()

        and:
        sindex.index("INFY", 1);
        sindex.index("INFY", 2);
        sindex.index("INFY", 2);
        when:
        var ids = (List<Integer>)(List)sindex.find("INFY");
        then:
        ids.size() == 3;
    }

    def "PostfixValue allows duplicate keys but merges duplicate values"() {
        given:
        var sindex = new MVStoreIndex<>(
                "db\\trade\\index",
                "sindex",
                MVStoreIndex.IndexingStrategy.PostfixValue,
                String.class,
                Integer.class,
                "!@#\$");
        sindex.clear()

        and:
        sindex.index("INFY", 1);
        sindex.index("INFY", 2);
        sindex.index("INFY", 2);
        when:
        var ids = (List<Integer>) (List) sindex.find("INFY");
        then:
        ids.size() == 2;
    }
        def "Can add multiple trades for the same date"() {
        given:
        dateIndex = new MVStoreIndex<>("db/trade/index", "date", MVStoreIndex.IndexingStrategy.PostfixValue, LocalDate.class, Integer.class, "!!##@@");
        dateIndex.clear();
        when:
        dateIndex.index(Utils.UtcToday(), 1);
        and:
        dateIndex.index(Utils.UtcToday(), 2);
        and:
        var ids = (List<Integer>)dateIndex.find(Utils.UtcToday());
        then:
        ids.size() == 2;

    }

    /*void setup() {
        symbolIndex = new MVStoreIndex<>("db\\trade\\index", "symbol", MVStoreIndex.IndexingStrategy.PostfixValue, String.class, Integer.class, "!!##@@");
        symbolIndex.clear();
        dateIndex = new MVStoreIndex<>("db/trade/index", "date", MVStoreIndex.IndexingStrategy.PostfixValue, LocalDate.class, Integer.class, "!!##@@");
        dateIndex.clear();
    }

    void cleanup() {
        if (symbolIndex != null) {
            symbolIndex.close();
        }
        if (dateIndex != null) {
            dateIndex.close();
        }

    }*/

    def "Delete"() {
    }
}
