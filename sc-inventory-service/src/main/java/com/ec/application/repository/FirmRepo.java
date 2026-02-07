package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.model.Firm;
import com.ec.application.model.Firm;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.ArrayList;
import java.util.List;

@Repository
public interface FirmRepo extends BaseRepository<Firm, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Firm save(Firm entity);

    boolean existsByFirmName(String firmName);

    ArrayList<Firm> findByfirmName(String firmName);

    @Query(value = "SELECT m from Firm m where firmName LIKE %:name%")
    ArrayList<Firm> findByPartialName(@Param("name") String name);

    @Query(value = "SELECT firmId as id,firmName as name from Firm m order by name")
    List<IdNameProjections> findIdAndNames();

    @Query(value = "SELECT firmName from Firm m order by firmName")
    List<String> getNames();
}
