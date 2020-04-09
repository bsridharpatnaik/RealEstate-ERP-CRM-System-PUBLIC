package com.ec.common.Repository;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ec.common.Model.ContactAllInfo;

@Repository
//@Transactional
public interface ContactAllInfoRepo extends JpaRepository<ContactAllInfo, Long>
{

}
