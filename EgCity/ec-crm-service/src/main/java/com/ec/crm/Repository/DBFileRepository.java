package com.ec.crm.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.DBFile;


@Repository
public interface DBFileRepository extends JpaRepository<DBFile, String> {

}