package com.ec.application.model;

import javax.persistence.*;

import com.ec.application.datasync.MultiTableSyncListener;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.ec.application.Deserializers.ToTitleCaseDeserializer;
import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Entity
@Table(name = "Category")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@EntityListeners(MultiTableSyncListener.class)
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Category extends ReusableFields {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Long categoryId;

    @NonNull
    @JsonDeserialize(using = ToTitleCaseDeserializer.class)
    @Column(name = "category_name", nullable = false, unique = true)
    String categoryName;

    @JsonDeserialize(using = ToSentenceCaseDeserializer.class)
    @Column(name = "categoryDescription")
    String categoryDescription;
}
