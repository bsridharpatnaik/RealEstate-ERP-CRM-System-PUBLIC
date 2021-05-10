/*
 * package com.ec.crm.Filters;
 * 
 * import javax.persistence.criteria.CriteriaBuilder; import
 * javax.persistence.criteria.CriteriaQuery; import
 * javax.persistence.criteria.Root;
 * 
 * import org.springframework.data.jpa.domain.Specification;
 * 
 * import com.ec.crm.Model.Sentiment; import com.ec.crm.Model.Sentiment_;
 * 
 * import lombok.NonNull;
 * 
 * public final class SentimentSpecifications { public static
 * Specification<Sentiment> whereSentimentnameContains(@NonNull String name) {
 * return (Root<Sentiment> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
 * cb.like(root.get(Sentiment_.name), "%"+name+"%"); } }
 */