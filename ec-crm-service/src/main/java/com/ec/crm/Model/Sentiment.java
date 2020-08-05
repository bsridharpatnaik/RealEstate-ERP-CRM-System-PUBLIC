/*
 * package com.ec.crm.Model;
 * 
 * import java.io.Serializable;
 * 
 * import javax.persistence.Column; import javax.persistence.Entity; import
 * javax.persistence.GeneratedValue; import javax.persistence.GenerationType;
 * import javax.persistence.Id; import javax.persistence.Table; import
 * javax.validation.constraints.NotBlank;
 * 
 * import org.hibernate.annotations.Where; import org.hibernate.envers.Audited;
 * 
 * import com.ec.crm.Data.AssigneeDAO; import
 * com.ec.crm.ReusableClasses.ReusableFields;
 * 
 * import lombok.Data;
 * 
 * @Entity
 * 
 * @Table(name = "sentiment")
 * 
 * @Audited
 * 
 * @Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
 * 
 * @Data public class Sentiment extends ReusableFields implements Serializable {
 * 
 * @Id
 * 
 * @GeneratedValue(strategy = GenerationType.AUTO)
 * 
 * @Column(name = "sentiment_id", updatable = false, nullable = false) Long
 * sentimentId;
 * 
 * 
 * @NotBlank(message = "Name is mandatory") String name;
 * 
 * String description; }
 */