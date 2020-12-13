package com.ec.application.repository;

import java.util.List;

import javax.persistence.LockModeType;
import javax.transaction.Transactional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import com.ec.application.model.Contact;

@Repository
@Transactional
public interface ContactInfoRepo extends JpaRepository<Contact, Long>, JpaSpecificationExecutor<Contact>,
		PagingAndSortingRepository<Contact, Long>
{

	default void softDelete(Contact entity)
	{

		Assert.notNull(entity, "The entity must not be null!");
		Assert.isInstanceOf(Contact.class, entity, "The entity must be soft deletable!");

		((Contact) entity).setDeleted(true);
		save(entity);
	}

	default void softDeleteById(Long id)
	{

		Assert.notNull(id, "The given id must not be null!");
		this.softDelete(findById(id).orElseThrow(
				() -> new EmptyResultDataAccessException(String.format("No %s entity with id %s exists!", "", id), 1)));
	}

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Contact save(Contact entity);

	@Query(value = "SELECT count(*) from Contact m where mobileNo=:mobileNo")
	int getCountByMobileNo(String mobileNo);

	@Query(value = "SELECT distinct name from Contact m where name like %:str%")
	List<String> getAllNamesMatchingName(String str);

	@Query(value = "SELECT distinct mobileNo from Contact m where mobileNo like %:str%")
	List<String> getAllNamesMatchingMobile(String str);

}
