package com.ec.application.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ec.application.model.DBFile;


@Repository
public interface DBFileRepository extends JpaRepository<DBFile, String> {

}