package com.ec.application.SoftDelete;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.util.Assert;

@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {


    default void softDelete(T entity) {

        Assert.notNull(entity, "The entity must not be null!");
        Assert.isInstanceOf(SoftDeletableEntity.class, entity, "The entity must be soft deletable!");

        ((SoftDeletableEntity)entity).setDeleted(true);
        save(entity);
    }

    default void softDeleteById(ID id) {

        Assert.notNull(id, "The given id must not be null!");
        this.softDelete(findById(id).orElseThrow(() -> new EmptyResultDataAccessException(
                String.format("No %s entity with id %s exists!", "", id), 1)));
    }


}
