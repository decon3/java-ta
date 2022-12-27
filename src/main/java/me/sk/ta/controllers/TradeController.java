package me.sk.ta.controllers;

import me.sk.ta.domain.Trade;
import me.sk.ta.repositories.MVStoreIndex;
import me.sk.ta.repositories.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TradeController {
    private static final Logger log = LoggerFactory.getLogger(TradeController.class);
    @Autowired
    private TradeRepository tradeRepo;

    @GetMapping("{tradeId}")
    public ResponseEntity<Trade> Get(@PathVariable int tradeId)
    {
        var trade = tradeRepo.get(tradeId);
        if (trade.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        else
        {
            return ResponseEntity.status(HttpStatus.OK).body(trade.get());
        }
    }

    @GetMapping("find/open/{symbol}")
    public ResponseEntity<Trade> GetOpenTradeBySymbol(@PathVariable String symbol)
    {
        var trade = tradeRepo.getOpenTrade(symbol);
        if (trade.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        else
        {
            return ResponseEntity.status(HttpStatus.OK).body(trade.get());
        }
    }

    @GetMapping("find/all")
    public ResponseEntity<List<Trade>> GetAll()
    {
        var list = tradeRepo.getOpenTrades();
        if (list == null || list.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        else
        {
            return ResponseEntity.status(HttpStatus.OK).body(list);
        }
    }

    @PostMapping("upload")
    public ResponseEntity<String> PostAll(@RequestBody List<Trade> trades)
    {
        if (trades == null || trades.isEmpty())
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No trades to save");
        }
        else
        {
            try
            {
                for(var t: trades) {
                    tradeRepo.saveOrUpdate(t);
                }
                return ResponseEntity.status(HttpStatus.CREATED).build();
            } catch (Exception e) {
                log.error("Error: {}", e);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }
        }
    }
    
}
