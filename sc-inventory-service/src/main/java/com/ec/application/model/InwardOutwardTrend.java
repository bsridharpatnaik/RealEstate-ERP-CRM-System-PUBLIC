package com.ec.application.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Entity
@Subselect("select * from inwardoutwardtrend")
@Immutable
@Audited
@Data
public class InwardOutwardTrend {
    @Id
    String date;

    @Column(name="inward_count")
    Double inwardCount;

    @Column(name="outward_count")
    Double outwardCount;

    public Date getDateFromString() throws ParseException {
        SimpleDateFormat f = new SimpleDateFormat("dd-MM-yyyy");
        return f.parse(this.date);
    }
}
