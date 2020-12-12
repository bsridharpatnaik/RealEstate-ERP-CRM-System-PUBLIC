package com.ec.common.Filters;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import com.ec.common.Model.User;
import com.ec.common.Model.User_;

import lombok.NonNull;

public final class UserSpecifications
{
	public static Specification<User> whereUsernameContains(@NonNull String username)
	{
		return (Root<User> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb.like(root.get(User_.userName),
				"%" + username + "%");
	}
}
