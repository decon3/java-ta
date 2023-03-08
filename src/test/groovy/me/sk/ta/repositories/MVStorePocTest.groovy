package me.sk.ta.repositories

import spock.lang.Specification

class MVStorePocTest extends Specification {

    def "Test"() {
        given:
        var x = new MVStorePoc();
        expect:
        x.check2();
    }
}
