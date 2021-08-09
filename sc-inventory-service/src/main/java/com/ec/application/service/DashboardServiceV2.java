package com.ec.application.service;

import com.ec.application.data.InventoryHistoricalStats;
import com.ec.application.data.StockPercentageForDashboard;
import com.ec.application.data.TimelyProductStatsForDashboard;
import com.ec.application.model.*;
import com.ec.application.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DashboardServiceV2 {

    @Autowired
    ProductRepo productRepo;

    @Autowired
    InwardInventoryRepo iiRepo;

    @Autowired
    OutwardInventoryRepo oiRepo;

    @Autowired
    StockRepo stockRepo;

    @Autowired
    InwardOutwardTrendRepo inwardOutwardTrendRepo;

    @Autowired
    InwardInventoryStatsForDashboardRepo inwardInventoryStatsForDashboardRepo;

    @Autowired
    OutwardInventoryStatsForDashboardRepo outwardInventoryStatsForDashboardRepo;

    public List<InventoryHistoricalStats> getInventoryHistoricalStats() {
        List<Product> productsForDashboard = getDashboardProducts();
        List<InwardInventory> inwardsInOneMonth =iiRepo.getCurrentMonthData();
        List<OutwardInventory> outwardsInOneMonth =oiRepo.getCurrentMonthData();
        return transformInwardAndOutward(productsForDashboard,inwardsInOneMonth,outwardsInOneMonth);
    }

    private List<InventoryHistoricalStats> transformInwardAndOutward(List<Product> productsForDashboard, List<InwardInventory> inwardsInOneMonth, List<OutwardInventory> outwardsInOneMonth) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        HashMap<Product,InventoryHistoricalStats> map = new HashMap<>();
        initializeMap(productsForDashboard,map);
        for(Product p:productsForDashboard) {
            for(InwardInventory ii:inwardsInOneMonth){
                for(InwardOutwardList iol:ii.getInwardOutwardList()){
                    if(iol.getProduct().getProductId().equals(p.getProductId())){
                        InventoryHistoricalStats ihs = map.get(p);
                        cal1.setTime(ii.getDate());
                        cal2.setTime(new Date());
                        if(cal1.get(Calendar.DAY_OF_YEAR)==cal2.get(Calendar.DAY_OF_YEAR))
                            ihs.getInward().setDaily(ihs.getInward().getDaily()+iol.getQuantity());

                        if(cal1.get(Calendar.WEEK_OF_YEAR)==cal2.get(Calendar.WEEK_OF_YEAR))
                            ihs.getInward().setWeekly(ihs.getInward().getWeekly()+iol.getQuantity());

                        if(cal1.get(Calendar.MONTH)==cal2.get(Calendar.MONTH))
                            ihs.getInward().setMonthly(ihs.getInward().getMonthly()+iol.getQuantity());
                        map.replace(p,ihs);
                    }
                }
            }

            for(OutwardInventory ii:outwardsInOneMonth){
                for(InwardOutwardList iol:ii.getInwardOutwardList()){
                    if(iol.getProduct().getProductId().equals(p.getProductId())){
                        InventoryHistoricalStats ihs = map.get(p);
                        cal1.setTime(ii.getDate());
                        cal2.setTime(new Date());
                        if(cal1.get(Calendar.DAY_OF_YEAR)==cal1.get(Calendar.DAY_OF_YEAR))
                            ihs.getOutward().setDaily(ihs.getOutward().getDaily()+iol.getQuantity());

                        if(cal1.get(Calendar.WEEK_OF_YEAR)==cal1.get(Calendar.WEEK_OF_YEAR))
                            ihs.getOutward().setWeekly(ihs.getOutward().getWeekly()+iol.getQuantity());

                        if(cal1.get(Calendar.MONTH)==cal1.get(Calendar.MONTH))
                            ihs.getOutward().setMonthly(ihs.getOutward().getMonthly()+iol.getQuantity());
                        map.replace(p,ihs);
                    }
                }
            }
        }
        return transformDataForDashboard(map);
    }

    private List<InventoryHistoricalStats> transformDataForDashboard(HashMap<Product, InventoryHistoricalStats> map) {
        List<InventoryHistoricalStats> returnData = new ArrayList<>();
        for(Map.Entry<Product,InventoryHistoricalStats> set:map.entrySet()){
            InventoryHistoricalStats ihs = set.getValue();
            ihs.setProductName(set.getKey().getProductName());
            ihs.setMeasurementUnit(set.getKey().getMeasurementUnit());
            ihs.setCurrentStock(stockRepo.getCurrentTotalStockForProduct(set.getKey().getProductId()));
            returnData.add(ihs);
        }
        return returnData;
    }

    private void initializeMap(List<Product> productsForDashboard, HashMap<Product, InventoryHistoricalStats> map) {
        for(Product p:productsForDashboard) {
            InventoryHistoricalStats ihs = new InventoryHistoricalStats();
            ihs.setInward(new TimelyProductStatsForDashboard());
            ihs.setOutward(new TimelyProductStatsForDashboard());
            ihs.getInward().setMonthly((double) 0);
            ihs.getInward().setWeekly((double) 0);
            ihs.getInward().setDaily((double) 0);
            ihs.getOutward().setMonthly((double) 0);
            ihs.getOutward().setWeekly((double) 0);
            ihs.getOutward().setDaily((double) 0);
            map.put(p,ihs);
        }
    }

    private List<Product> getDashboardProducts() {
        List<Product> productsForDashboard = productRepo.getDashboardProducts();
        if(productsForDashboard.size()>9) {
            return productsForDashboard;
        }
        else{
            int ctr=0;
            List<Product> allProducts = productRepo.findAll(Sort.by(Sort.Direction.DESC, "productId"));
            while(productsForDashboard.size()!=10 && allProducts.size()>=10){
                Long pid = allProducts.get(ctr).getProductId();
                if(!productsForDashboard.stream().anyMatch(o -> o.getProductId().equals(pid)))
                    productsForDashboard.add(allProducts.get(ctr));
                ctr++;
            }
            return productsForDashboard;
        }
    }

    public List<StockPercentageForDashboard> getStockPercentForDashboard() {
        List<Product> productsForDashboard = getDashboardProducts();
        List<StockPercentageForDashboard> returnData = new ArrayList<StockPercentageForDashboard>();
        List<StockPercentageForDashboard> existingStock = stockRepo.getCurrentStockPercentForDashboardProducts(productsForDashboard);
        for(Product p:productsForDashboard){
            Double stock = null;
            for(StockPercentageForDashboard sp:existingStock){
                if(p.getProductName().equals(sp.getProductName())){
                    stock = sp.getStockPercent();
                }
            }
            if(stock==null)
                stock=(double)0;
            returnData.add(new StockPercentageForDashboard(p.getProductName(),stockRepo.getCurrentTotalStockForProduct(p.getProductId()),stock));
        }
        return returnData;
    }

    public List<InwardOutwardTrend> getInwardOutwardTrend(){
        return inwardOutwardTrendRepo.findAll();
    }

    public Page<InwardInventoryStatsForDashboardV2> getInwardStats(Pageable pageable){
        return inwardInventoryStatsForDashboardRepo.findAll(pageable);
    }

    public Page<OutwardInventoryStatsForDashboardV2> getOutwardStats(Pageable pageable){
        return outwardInventoryStatsForDashboardRepo.findAll(pageable);
    }
}
