package com.ec.application.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.InwardOutwardList;

@Repository
public interface InwardOutwardListRepo extends BaseRepository<InwardOutwardList, Long>
{
	@Query(value="SELECT count(*) from InwardOutwardList m where m.product.productId=:productId")
	int productUsageCount(Long productId);
	
}
