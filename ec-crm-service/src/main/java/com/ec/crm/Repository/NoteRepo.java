package com.ec.crm.Repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.Lead;
import com.ec.crm.Model.Note;
import com.ec.crm.ReusableClasses.BaseRepository;

@Repository
public interface NoteRepo extends BaseRepository<Note, Long>, JpaSpecificationExecutor<Note> {

}
