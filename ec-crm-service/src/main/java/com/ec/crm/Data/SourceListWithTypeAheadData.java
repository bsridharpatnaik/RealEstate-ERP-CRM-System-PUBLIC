package com.ec.crm.Data;

import java.util.List;

import org.springframework.data.domain.Page;
import com.ec.crm.Model.Source;

import lombok.Data;

@Data
public class SourceListWithTypeAheadData 
{
	Page<Source> sourceDetails;
	List<String> sourceTypeAhead;	
}
