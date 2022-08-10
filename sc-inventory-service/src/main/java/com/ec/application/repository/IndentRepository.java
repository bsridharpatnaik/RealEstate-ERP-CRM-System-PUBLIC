package com.ec.application.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.model.Indent;

@Transactional
public interface IndentRepository extends BaseRepository<Indent, Long> {

	@Modifying
	@Query(value = "delete from indent where indentNumber =?1", nativeQuery = true)
	void deleteByIndentNumber(long indentNumber);

	List<Indent> findByIndentNumberIn(List<Long> list);

	@Query(value = "SELECT indentNumber as id from Indent m  order by indentNumber")
	List<IdNameProjections> findIdAndNames();

	Indent findByIndentNumber(long indentNumber);

}
