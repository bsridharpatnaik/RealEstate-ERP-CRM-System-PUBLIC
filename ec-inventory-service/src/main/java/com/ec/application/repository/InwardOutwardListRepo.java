package com.ec.application.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.InwardOutwardList;

@Repository
public interface InwardOutwardListRepo extends BaseRepository<InwardOutwardList, Long>
{

	@Query(value = "SELECT count(*) from InwardOutwardList m where m.product.productId=:productId")
	int productUsageCount(@Param("productId") Long productId);

	@Query(value = "SELECT iol from InwardOutwardList iol where iol.entryid>=:startEntryId and iol.entryid<=:endEntryId")
	List<InwardOutwardList> getRecordsBetweenEntryIds(@Param("startEntryId") Long startEntryId,
			@Param("endEntryId") Long endEntryId);
}
