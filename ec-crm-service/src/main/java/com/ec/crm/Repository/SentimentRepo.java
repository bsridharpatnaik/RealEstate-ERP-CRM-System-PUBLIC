package com.ec.crm.Repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ec.ReusableClasses.BaseRepository;
import com.ec.crm.Model.Sentiment;

public interface SentimentRepo extends BaseRepository<Sentiment, Long>, JpaSpecificationExecutor<Sentiment>{

}
