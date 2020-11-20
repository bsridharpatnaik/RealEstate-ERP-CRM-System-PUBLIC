package com.ec.crm.Data;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Where;

import com.ec.ReusableClasses.ReusableFields;
import com.ec.crm.Model.User;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_token")
//@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@AllArgsConstructor
@NoArgsConstructor
public class AndroidTokenDetails extends ReusableFields
{

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long tokenId;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "userId", nullable = false)
	User user;

	String token;

	public Long getTokenId()
	{
		return tokenId;
	}

	public void setTokenId(Long tokenId)
	{
		this.tokenId = tokenId;
	}

	public User getUser()
	{
		return user;
	}

	public void setUser(User user)
	{
		this.user = user;
	}

	public String getToken()
	{
		return token;
	}

	public void setToken(String token)
	{
		this.token = token;
	}

}
