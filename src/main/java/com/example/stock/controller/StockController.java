package com.example.stock.controller;

import com.example.stock.facade.*;
import com.example.stock.service.PessimisticLockStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StockController {

    private final PessimisticLockStockFacade pessimisticLockStockFacade;
    private final OptimisticLockStockFacade optimisticLockStockFacade;
    private final NamedLockStockFacade namedLockStockFacade;
    private final LettuceLockStockFacade lettuceLockStockFacade;
    private final RedissonLockStockFacade redissonLockStockFacade;

    private final PessimisticLockStockService pessimisticLockStockService;

    @GetMapping("/")
    public String test() {
        return "test";
    }

    /**
     *  Pessimistic Lock
     */
    @PostMapping("/pessimisticLock/{id}")
    public String pessimisticLock(@PathVariable("id") Long id) throws InterruptedException {
//        pessimisticLockStockFacade.decrease(id, 1L);
        pessimisticLockStockService.decrease(id, 1L);
        return "pessimisticLock";
    }

    /**
     *  Optimistic Lock
     */
    @PostMapping("/optimisticLock/{id}")
    public String optimisticLock(@PathVariable("id") Long id) throws InterruptedException {
        optimisticLockStockFacade.decrease(id, 1L);
        return "optimisticLock";
    }

    /**
     *  Named Lock
     */
    @PostMapping("/namedLock/{id}")
    public String namedLock(@PathVariable("id") Long id) {
        namedLockStockFacade.decrease(id, 1L);
        return "namedLock";
    }

    /**
     *  Lettuce
     */
    @PostMapping("/lettuceLock/{id}")
    public String lettuceLock(@PathVariable("id") Long id) throws InterruptedException {
        lettuceLockStockFacade.decrease(id, 1L);
        return "lettuceLock";
    }

    /**
     *  Redisson
     */
    @PostMapping("/redissonLock/{id}")
    public String redissonLock(@PathVariable("id") Long id) {
        redissonLockStockFacade.decrease(id, 1L);
        return "redissonLock";
    }

}
