package com.ec.application.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.IndentInventory;

@Transactional
public interface IndentInventoryRepository extends BaseRepository<IndentInventory, Long> {
List<IndentInventory> findByIndentIndentNumber(long indentNumber);
	
	IndentInventory findById(long id);

	@Modifying
	@Query(value = "delete from indent_inventory where indentNumber =?1", nativeQuery = true)
	void deleteByIndentIndentNumber(long indentNumber);

}
