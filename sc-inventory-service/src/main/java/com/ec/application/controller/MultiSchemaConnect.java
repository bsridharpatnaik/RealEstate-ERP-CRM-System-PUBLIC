package com.ec.application.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.service.TxnService;

@RestController
public class MultiSchemaConnect {

    @Autowired
    private TxnService txnService;

    @GetMapping("/multiTxn")
    public void txn() {
        ThreadLocalStorage.setTenantName("test1");
        txnService.txn1(); // Here connect to test1 schema

        ThreadLocalStorage.setTenantName("test2");
        txnService.txn1(); // Here connect to test2 schema

        // output on DB will be same row count in test1 and test2 schema for n number of requests
    }

}
