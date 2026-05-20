package com.ec.application.repository;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.data.IdNameAndUnit;
import com.ec.application.model.Product;

@Repository
public interface ProductRepo extends BaseRepository<Product, Long>
{

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Product save(Product entity);

	@Query("Select p from Product p where p.isDeleted=false and p.productId IN :ids")
	ArrayList<Product> findByProductIdIn(@Param("ids") List<Long> ids);

	boolean existsByProductName(String productName);

	ArrayList<Product> findByproductName(String productName);

	ArrayList<Product> findByproductCode(String productCode);

	@Query(value = "SELECT m from Product m where m.category.categoryId=id")
	ArrayList<Product> existsByCategoryId(@Param("id") Long id);

	@Query(value = "SELECT productId as id,productName as name from Product m  order by name")
	List<IdNameProjections> findIdAndNames();

	@Query(value = "SELECT count(*) from Product m where m.category.categoryId=:categoryId")
	int categoryUsageCount(@Param("categoryId") Long categoryId);

	@Query(value = "SELECT productName from Product m where productName like %:name% order by productName")
	List<String> getNames(@Param("name") String name);

	@Query(value = "SELECT distinct productId from Product m")
	List<Long> fetchUniqueProductIds();

	@Query(value = "SELECT new com.ec.application.data.IdNameAndUnit(productId,productName,measurementUnit, productCode, isManagedInventory, isExpirable) from Product m")
	List<IdNameAndUnit> getProductMeasurementUnit();

	@Query(
			"SELECT new com.ec.application.data.IdNameAndUnit(" +
					"   m.productId, m.productName, m.measurementUnit, m.productCode, m.isManagedInventory, m.isExpirable" +
					") " +
					"FROM Product m " +
					"WHERE (:isManagedInventory IS NULL OR m.isManagedInventory = :isManagedInventory) " +
					"AND (:categoryId IS NULL OR m.category.categoryId = :categoryId)"
	)
	List<IdNameAndUnit> getProducts(
			@Param("isManagedInventory") Boolean isManagedInventory,
			@Param("categoryId") Long categoryId
	);



	@Query(value = "SELECT new com.ec.application.data.IdNameAndUnit(productId,productName,measurementUnit, productCode, isManagedInventory, isExpirable) from Product m")
	List<IdNameAndUnit> getProducts();

	@Query(value = "SELECT p from Product p where p.showOnDashboard=true")
    List<Product> getDashboardProducts();

	Product findByProductName(String inventory);

	Product findByProductId(long productId);

	boolean existsByProductNameAndIsDeleted(String inventory, boolean b);

	@Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Product m WHERE TRIM(m.productName) = TRIM(:name) AND m.isDeleted = false")
	boolean existsByProductNameTrimmed(@Param("name") String name);

	@Query("SELECT m FROM Product m WHERE TRIM(m.productName) = TRIM(:name) AND m.isDeleted = false")
	Product findByProductNameTrimmed(@Param("name") String name);

	@Query(value = "SELECT productId as id,measurementUnit as name from Product m  where m.productId=:id order by name")
	List<IdNameProjections> findIdAndMeasurementUnitNames(@Param("id") long id);

	@Query(value = "SELECT productId as id,productCode as name from Product m  order by productCode")
	List<IdNameProjections> findIdAndProductCodes();

//	@Query(value = "SELECT productId as id,measurementUnit as name from Product m  where m.productId=:id order by name")
//	List<IdNameProjections> findIdAndMeasurementUnitNames(long productId);

	@Query(value = "SELECT * FROM Product WHERE productId = :productId", nativeQuery = true)
	java.util.Optional<Product> findByIdIncludingDeleted(@Param("productId") Long productId);
}
