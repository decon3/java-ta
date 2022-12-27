package me.sk.ta.repositories

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
        symbolIndex = new MVStoreIndex<>(
                "db\\trade\\index",
                "sindex",
                MVStoreIndex.IndexingStrategy.PostfixWithCount,
                String.class,
                Integer.class,
                "!@#\$");

        and:
        symbolIndex.index("INFY", 1);
        symbolIndex.index("INFY", 2);
        symbolIndex.index("INFY", 2);
        when:
        var ids = (List<Integer>) (List) symbolIndex.find("INFY");
        then:
        ids.size() == 3;
    }

    def "PostfixValue allows duplicate keys but merges duplicate values"() {
        given:
        symbolIndex = new MVStoreIndex<>(
                "db\\trade\\index",
                "symbol",
                MVStoreIndex.IndexingStrategy.PostfixValue,
                String.class,
                Integer.class,
                "!@#\$");

        and:
        symbolIndex.index("INFY", 1);
        symbolIndex.index("INFY", 2);
        symbolIndex.index("INFY", 2);
        when:
        var ids = (List<Integer>) (List) symbolIndex.find("INFY");
        then:
        ids.size() == 2;
    }

    def "Can add multiple trades for the same date"() {
        given:
        dateIndex = new MVStoreIndex<>("db/trade/index",
                "date",
                MVStoreIndex.IndexingStrategy.PostfixValue,
                LocalDate.class,
                Integer.class,
                "!!##@@");
        when:
        dateIndex.index(Utils.UtcToday(), 1);
        and:
        dateIndex.index(Utils.UtcToday(), 2);
        and:
        var ids = (List<Integer>) dateIndex.find(Utils.UtcToday());
        then:
        ids.size() == 2;

    }

    def "Delete"() {
        given:
        var symbolIndex = new MVStoreIndex<>(
                "db\\trade\\index",
                "sindex",
                MVStoreIndex.IndexingStrategy.PostfixValue,
                String.class,
                Integer.class,
                "!@#\$");

        and:
        symbolIndex.index("INFY", 1);
        symbolIndex.index("INFY", 2);
        symbolIndex.index("INFY", 2);
        when:
        symbolIndex.delete("INFY", 2);
        and:
        var ids = (List<Integer>) (List) symbolIndex.find("INFY");
        then:
        ids.size() == 1;
    }

    void cleanup() {
        if (symbolIndex != null) {
            symbolIndex.drop();
        }
        if (dateIndex != null) {
            dateIndex.drop();
        }
    }
}
