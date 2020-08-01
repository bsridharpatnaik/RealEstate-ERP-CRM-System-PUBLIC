package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.Lead;
import com.ec.crm.Model.Note;
import com.ec.crm.ReusableClasses.BaseRepository;

@Repository
public interface NoteRepo extends BaseRepository<Note, Long>, JpaSpecificationExecutor<Note> 
{
	@Query(value="SELECT n from Note n where n.leadId=:id and pinned=:pinned order by created desc")
	List<Note> findNotesForLead(@Param("id")Long id,@Param("pinned")boolean pinned);
	
}
