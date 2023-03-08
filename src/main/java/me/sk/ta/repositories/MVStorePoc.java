package me.sk.ta.repositories;

import org.h2.mvstore.MVStore;
import org.h2.mvstore.tx.Transaction;
import org.h2.mvstore.tx.TransactionMap;
import org.h2.mvstore.tx.TransactionStore;
import org.h2.store.fs.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MVStorePoc {
    private static final Logger log = LoggerFactory.getLogger(MVStorePoc.class);

    public boolean checkDefaultAction() {
        String fileName = "db/mvstore";
        FileUtils.delete(fileName);
        try (MVStore s = MVStore.open(fileName)) {
                TransactionStore ts = new TransactionStore(s);
                ts.init();
                var tx = ts.begin();
                tx.setName("first transaction");
                TransactionMap<String, String> m = tx.openMap("test");
                m.put("1", "Hello");
                tx.commit();

            TransactionStore ts2 = new TransactionStore(s);
            ts2.init();
            var tx2 = ts2.begin();
            tx2.setName("first transaction");
            TransactionMap<String, String> m2 = tx2.openMap("test");
            var result = m2.get("1");
            log.info("Result class: {} value {}", result.getClass(), result);
            tx2.commit();

        }

        try (MVStore s = MVStore.open(fileName)) {
            TransactionStore ts2 = new TransactionStore(s);
            ts2.init();
            var tx2 = ts2.begin();
            tx2.setName("first transaction");
            TransactionMap<String, String> m2 = tx2.openMap("test");
            var result = m2.get("1");
            log.info("Result class: {} value {}", result.getClass(), result);
            tx2.commit();
        }
        return true;
    }

    public boolean check2() {
        log.info("entered");
        TransactionMap<String, String> m, m2;
        Transaction tx, tx2;
        String fileName = "db/mvstore";
        FileUtils.delete(fileName);
        FileUtils.delete(fileName + "2");
        try (MVStore s = MVStore.open(fileName)) {
            try (MVStore s2 = MVStore.open(fileName + "2")) {
                TransactionStore ts = new TransactionStore(s);
                ts.init();
                tx = ts.begin();
                tx.setName("first transaction");
                m = tx.openMap("test");
                m.put("1", "Hello");
                log.info("TS Open transactions: {}", ts.getOpenTransactions().size());
                log.info("tx status: {}. M.isClosed? {}", tx.getStatus(), m.isClosed());
                log.info("preparing tx");
                tx.prepare();

                log.info("tx status: {}. M.isClosed? {}", tx.getStatus(), m.isClosed());
                TransactionStore ts2 = new TransactionStore(s2);
                ts2.init();
                tx2 = ts2.begin();
                tx2.setName("index transaction");
                log.info(tx2.getName());
                m2 = tx2.openMap("symbol_index");
                if (m2 == null) {
                    log.info("M2 is null");
                }
                if (m2.isClosed()) {
                    log.info("M2 is closed");
                }
                m2.put("Hello", "1");
                log.info("TS Open transactions: {}", ts.getOpenTransactions().size());
                log.info("TS2 Open transactions: {}", ts2.getOpenTransactions().size());
                log.info("tx status: {}. M.isClosed? {}", tx.getStatus(), m.isClosed());
                log.info("tx2 status: {}. M2.isClosed? {}", tx2.getStatus(), m2.isClosed());
                log.info("committing tx2");
                tx2.rollback();

                log.info("tx status: {}. M.isClosed? {}", tx.getStatus(), m.isClosed());
                log.info("tx2 status: {}. M2.isClosed? {}", tx2.getStatus(), m2.isClosed());
                tx.commit();
                log.info("TS Open transactions: {}", ts.getOpenTransactions().size());
                log.info("TS2 Open transactions: {}", ts2.getOpenTransactions().size());

                log.info("tx status: {}. M.isClosed? {}", tx.getStatus(), m.isClosed());
                log.info("tx2 status: {}. M2.isClosed? {}", tx2.getStatus(), m2.isClosed());
                log.info("transaction is committed");
                var tx3 = ts.begin();
                var m3 = tx3.openMap("test");
                if (m3 == null) {
                    log.info("M3 is null");
                }
                var result = m3.get("1");
                log.info("Result '{}': '{}'", result.getClass(), result);
            } catch (Exception ex) {
                log.error("Exception2: {}", ex);
                return false;
            }
        } catch (Exception ex) {
            log.error("Exception1: {}", ex);
            return false;
        }
        return true;
    }
    public boolean check() {
        log.info("entered");
        TransactionMap<String, String> m;
        String fileName = "db/mvstore";
        FileUtils.delete(fileName);
        try (MVStore s = MVStore.open(fileName)) {
            TransactionStore ts = new TransactionStore(s);
            ts.init();
            Transaction tx = ts.begin();
            tx.setName("first transaction");
            m = tx.openMap("test");
            m.put("1", "Hello");
            List<Transaction> list = ts.getOpenTransactions();
            Transaction txOld = list.get(0);
            log.info("Name: {}", txOld.getName());
            s.commit();
        } catch (Exception ex) {
            log.error("{}", ex);
        }
        log.info("entered2");

        try (MVStore s = MVStore.open(fileName)) {
            TransactionStore ts = new TransactionStore(s);
            ts.init();
            Transaction tx = ts.begin();
            m = tx.openMap("test");
            m.put("2", "Hello");
            List<Transaction> list = ts.getOpenTransactions();
            Transaction txOld = list.get(0);
            txOld.prepare();
            txOld = list.get(1);
            txOld.commit();
            s.commit();
        } catch (Exception ex) {
            log.error("{}", ex);
        }
        log.info("entered3");


        try (MVStore s = MVStore.open(fileName)) {
            TransactionStore ts = new TransactionStore(s);
            ts.init();
            Transaction tx = ts.begin();
            m = tx.openMap("test");
            m.put("3", "Test");
            List<Transaction> list = ts.getOpenTransactions();
            Transaction txOld = list.get(1);
            txOld.rollback();
            txOld = list.get(0);
            txOld.commit();
        } catch (Exception ex) {
            log.error("{}", ex);
        }
        log.info("finished");

        return true;
    }
}
