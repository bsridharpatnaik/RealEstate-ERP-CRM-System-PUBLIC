package com.ec.ReusableClasses;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.util.Assert;

@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> ,PagingAndSortingRepository<T, ID>
{


    default void softDelete(T entity) {

        Assert.notNull(entity, "The entity must not be null!");
        Assert.isInstanceOf(ReusableFields.class, entity, "The entity must be soft deletable!");

        ((ReusableFields)entity).setDeleted(true);
        save(entity);
    }

    default void softDeleteById(ID id) {

        Assert.notNull(id, "The given id must not be null!");
        this.softDelete(findById(id).orElseThrow(() -> new EmptyResultDataAccessException(
                String.format("No %s entity with id %s exists!", "", id), 1)));
    }


}
