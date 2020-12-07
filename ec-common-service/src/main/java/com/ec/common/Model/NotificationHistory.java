package com.ec.common.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Where;

import com.ec.ReusableClasses.ReusableFields;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_history")
//@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class NotificationHistory
{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "history_id", updatable = false, nullable = false)
	Long historyId;
	Long userId;
	String body;
	String title;
}
