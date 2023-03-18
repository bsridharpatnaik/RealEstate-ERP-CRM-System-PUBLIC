package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.BOQStatusView;
import com.ec.application.model.BOQStatusViewV2;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BOQStatusViewRepositoryV2 extends BaseRepository<BOQStatusViewV2, Long> {

    @Query("SELECT b FROM BOQStatusViewV2 b WHERE b.locationId=:locationId AND b.productId=:productId")
    List<BOQStatusViewV2> getboqStatusForOutward(@Param("locationId") Long locationId, @Param("productId") Long productId);
}
