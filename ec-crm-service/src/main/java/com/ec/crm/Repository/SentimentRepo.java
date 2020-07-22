package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.ec.crm.Model.Sentiment;
import com.ec.crm.ReusableClasses.BaseRepository;
import com.ec.crm.ReusableClasses.IdNameProjections;

public interface SentimentRepo extends BaseRepository<Sentiment, Long>, JpaSpecificationExecutor<Sentiment>{
	boolean existsByName(String name);
	
	@Query(value="SELECT sentimentId as id,name as name from Sentiment S")
	List<IdNameProjections> findIdAndNames();
	
	@Query(value="SELECT distinct name from Sentiment S")
	List<String> findNamesList();
}
