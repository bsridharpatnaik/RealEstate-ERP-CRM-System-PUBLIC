package com.ec.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.application.model.TxnSchema;
import com.ec.application.repository.TxnRepo;

@Service
public class TxnService {

	@Autowired
	private TxnRepo txnRepo;
	
	public void txn1() {
		TxnSchema txn = new TxnSchema();
		txn.setCount(1);
		txnRepo.save(txn);
	}
	
	public void txn2() {
		TxnSchema txn = new TxnSchema();
		txn.setCount(1);
		txnRepo.save(txn);
	}
}
