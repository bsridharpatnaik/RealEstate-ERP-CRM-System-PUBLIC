package com.ec.application.model;

import javax.persistence.*;

import com.ec.application.datasync.MultiTableSyncListener;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "Product")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Data
@NoArgsConstructor
@EntityListeners(MultiTableSyncListener.class)
public class Product extends ReusableFields {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Long productId;

    @NonNull
    @Column(name = "product_name", unique = true, nullable = false)
    String productName;

    @Column(name = "productDescription")
    String productDescription;

    @Column(name = "measurementUnit")
    String measurementUnit;

    @Column(name = "reorderQuantity")
    Double reorderQuantity;

    @Column(name = "is_managed_inventory", columnDefinition = "boolean default true")
    Boolean isManagedInventory;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "categoryId", nullable = false)
    @JsonIgnoreProperties(
            {"hibernateLazyInitializer", "handler"})
    @NotFound(action = NotFoundAction.IGNORE)
    Category category;

    @Column(name = "show_on_dashboard")
    Boolean showOnDashboard;
}
