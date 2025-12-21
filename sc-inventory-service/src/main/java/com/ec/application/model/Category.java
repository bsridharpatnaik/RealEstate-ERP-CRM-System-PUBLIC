package com.ec.application.model;

import javax.persistence.*;

import com.ec.application.datasync.MultiTableSyncListener;
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
public class Category extends ReusableFields {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Long categoryId;

    @NonNull
    @JsonDeserialize(using = ToTitleCaseDeserializer.class)
    @Column(name = "category_name")
    String categoryName;

    @JsonDeserialize(using = ToSentenceCaseDeserializer.class)
    @Column(name = "categoryDescription")
    String categoryDescription;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryDescription() {
        return categoryDescription;
    }

    public void setCategoryDescription(String categoryDescription) {
        this.categoryDescription = categoryDescription;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    @Override
    public String toString() {
        return "Category [categoryId=" + categoryId + ", categoryName=" + categoryName + ", categoryDescription="
                + categoryDescription + "]";
    }
}
