package com.ec.crm.Model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Entity
@Subselect("select * from lead_activity_for_pipeline")
@Immutable
@Data
@NoArgsConstructor
public class RecentActivityForPipeline {
    @Id
    @Column(name = "lead_id")
    Long leadId;

    @Column(name = "recentIsOpen")
    Boolean recentIsOpen;

    @Column(name = "recentActivityDateTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    Date recentActivityDateTime;
}
