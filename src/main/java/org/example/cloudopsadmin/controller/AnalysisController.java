package org.example.cloudopsadmin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.common.ApiResponse;
import org.example.cloudopsadmin.entity.CustomerMonthlyBill;
import org.example.cloudopsadmin.service.CustomerMonthlyBillService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@Tag(name = "Business Analysis", description = "Monthly business analysis APIs")
public class AnalysisController {

    private final CustomerMonthlyBillService customerMonthlyBillService;

    @GetMapping("/monthly")
    @Operation(summary = "Get monthly business analysis", description = "Get analysis data including revenue by provider and overview")
    public ApiResponse<Map<String, Object>> getMonthlyAnalysis(
            @RequestParam(required = false) String month,
            @RequestParam(name = "cloud_provider", required = false) String cloudProvider
    ) {
        String targetMonth = StringUtils.hasText(month) ? month : DateTimeFormatter.ofPattern("yyyy-MM").format(LocalDate.now());
        String prevMonth = DateTimeFormatter.ofPattern("yyyy-MM").format(
                LocalDate.parse(targetMonth + "-01").minusMonths(1)
        );

        // Fetch data
        String vendorFilter = null;
        if (StringUtils.hasText(cloudProvider) && !"All".equalsIgnoreCase(cloudProvider) && !"全部".equals(cloudProvider)) {
            vendorFilter = cloudProvider;
        }

        List<CustomerMonthlyBill> currentBills = customerMonthlyBillService.listBillsByFilters(targetMonth, null, null, vendorFilter);
        List<CustomerMonthlyBill> prevBills = customerMonthlyBillService.listBillsByFilters(prevMonth, null, null, vendorFilter);

        // Process Current Month
        Map<String, List<CustomerMonthlyBill>> currentByVendor = currentBills.stream()
                .filter(b -> b.getCloudVendor() != null)
                .collect(Collectors.groupingBy(CustomerMonthlyBill::getCloudVendor));

        double totalProfitAll = 0.0;
        double totalRevenueAll = 0.0;
        double totalCostAll = 0.0;
        double totalCustomerPayableAll = 0.0;

        List<Map<String, Object>> thisMonthProviderStats = new ArrayList<>();

        // First pass to calculate totals for profit share
        for (CustomerMonthlyBill bill : currentBills) {
            Financials f = calculateFinancials(bill);
            totalRevenueAll += f.revenue;
            totalCustomerPayableAll += f.customerPayable;
            totalCostAll += f.cost;
            totalProfitAll += f.profit;
        }

        // Second pass to build provider stats
        for (Map.Entry<String, List<CustomerMonthlyBill>> entry : currentByVendor.entrySet()) {
            String vendor = entry.getKey();
            List<CustomerMonthlyBill> bills = entry.getValue();

            double revenue = 0;
            double customerPayable = 0;
            double cost = 0;
            double profit = 0;

            for (CustomerMonthlyBill bill : bills) {
                Financials f = calculateFinancials(bill);
                revenue += f.revenue;
                customerPayable += f.customerPayable;
                cost += f.cost;
                profit += f.profit;
            }

            // Get previous month stats for this vendor
            double prevRevenue = 0;
            for (CustomerMonthlyBill pb : prevBills) {
                if (vendor.equals(pb.getCloudVendor())) {
                    prevRevenue += calculateFinancials(pb).revenue;
                }
            }

            Map<String, Object> stat = new HashMap<>();
            stat.put("cloud_vendor", vendor);
            stat.put("revenue", round2(revenue));
            stat.put("customer_payable", round2(customerPayable));
            stat.put("cost", round2(cost));
            stat.put("profit", round2(profit));
            
            double margin = revenue != 0 ? (profit / revenue) * 100 : 0; // Using Profit / Revenue as Margin
            stat.put("margin", round2(margin) + "%");
            
            double profitShare = totalProfitAll != 0 ? (profit / totalProfitAll) * 100 : 0;
            stat.put("profit_share", round2(profitShare) + "%");

            String revenueMom = "-";
            if (prevRevenue != 0) {
                double mom = ((revenue - prevRevenue) / prevRevenue) * 100;
                revenueMom = round2(mom) + "%";
            }
            stat.put("revenue_mom", revenueMom);

            thisMonthProviderStats.add(stat);
        }

        // Process Previous Month for "Last Month Revenue" section
        Map<String, List<CustomerMonthlyBill>> prevByVendor = prevBills.stream()
                .filter(b -> b.getCloudVendor() != null)
                .collect(Collectors.groupingBy(CustomerMonthlyBill::getCloudVendor));
        
        List<Map<String, Object>> lastMonthProviderStats = new ArrayList<>();
        for (Map.Entry<String, List<CustomerMonthlyBill>> entry : prevByVendor.entrySet()) {
            String vendor = entry.getKey();
            List<CustomerMonthlyBill> bills = entry.getValue();

            double revenue = 0;
            double customerPayable = 0;
            double cost = 0;
            double profit = 0;

            for (CustomerMonthlyBill bill : bills) {
                Financials f = calculateFinancials(bill);
                revenue += f.revenue;
                customerPayable += f.customerPayable;
                cost += f.cost;
                profit += f.profit;
            }

            Map<String, Object> stat = new HashMap<>();
            stat.put("cloud_vendor", vendor);
            stat.put("revenue", round2(revenue));
            stat.put("customer_payable", round2(customerPayable));
            stat.put("cost", round2(cost));
            stat.put("profit", round2(profit));
            lastMonthProviderStats.add(stat);
        }

        // Overview
        Map<String, Object> overview = new HashMap<>();
        overview.put("total_revenue", round2(totalRevenueAll));
        // Assuming tax is 0 or needs to be calculated. Using 6% as a placeholder derived from image if needed, 
        // but safe to set 0 or leave empty if unknown. 
        // Let's check if we can calculate it from invoices? 
        // For now, I'll calculate a "Tax" estimation or just return 0.
        // Image: Rev 21500, Tax 1290 -> 6%. 
        // Let's assume tax is 0 unless we have data.
        overview.put("tax", 0.0); 
        overview.put("total_cost", round2(totalCostAll));
        overview.put("total_profit", round2(totalProfitAll));

        Map<String, Object> response = new HashMap<>();
        response.put("this_month_revenue_by_provider", thisMonthProviderStats);
        response.put("last_month_revenue_by_provider", lastMonthProviderStats);
        response.put("this_month_overview", overview);

        return ApiResponse.success("success", response);
    }

    @GetMapping("/yearly")
    @Operation(summary = "Get yearly business analysis", description = "Get yearly analysis data including revenue by provider, overview and comparison")
    public ApiResponse<Map<String, Object>> getYearlyAnalysis(
            @RequestParam(required = false) Integer year
    ) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        int prevYear = targetYear - 1;

        // Fetch data
        List<CustomerMonthlyBill> thisYearBills = customerMonthlyBillService.listBillsByYear(targetYear);
        List<CustomerMonthlyBill> lastYearBills = customerMonthlyBillService.listBillsByYear(prevYear);

        // Calculate totals for overview
        double totalRevenue = 0;
        double totalCost = 0;
        double totalProfit = 0;
        double totalTax = 0; // Currently 0 as per requirement

        for (CustomerMonthlyBill bill : thisYearBills) {
            Financials f = calculateFinancials(bill);
            totalRevenue += f.revenue;
            totalCost += f.cost;
            totalProfit += f.profit;
        }

        // Revenue Comparison Chart (This Year vs Last Year)
        Map<String, Double> thisYearRevenueByVendor = thisYearBills.stream()
                .filter(b -> b.getCloudVendor() != null)
                .collect(Collectors.groupingBy(
                        CustomerMonthlyBill::getCloudVendor,
                        Collectors.summingDouble(b -> calculateFinancials(b).revenue)
                ));

        Map<String, Double> lastYearRevenueByVendor = lastYearBills.stream()
                .filter(b -> b.getCloudVendor() != null)
                .collect(Collectors.groupingBy(
                        CustomerMonthlyBill::getCloudVendor,
                        Collectors.summingDouble(b -> calculateFinancials(b).revenue)
                ));

        Set<String> allVendors = new HashSet<>();
        allVendors.addAll(thisYearRevenueByVendor.keySet());
        allVendors.addAll(lastYearRevenueByVendor.keySet());

        List<Map<String, Object>> revenueComparison = new ArrayList<>();
        for (String vendor : allVendors) {
            Map<String, Object> item = new HashMap<>();
            item.put("cloud_vendor", vendor);
            item.put("this_year", round2(thisYearRevenueByVendor.getOrDefault(vendor, 0.0)));
            item.put("last_year", round2(lastYearRevenueByVendor.getOrDefault(vendor, 0.0)));
            revenueComparison.add(item);
        }

        // Revenue Share Chart (This Year)
        List<Map<String, Object>> revenueShare = new ArrayList<>();
        for (Map.Entry<String, Double> entry : thisYearRevenueByVendor.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("cloud_vendor", entry.getKey());
            item.put("revenue", round2(entry.getValue()));
            double percentage = totalRevenue > 0 ? (entry.getValue() / totalRevenue) * 100 : 0;
            item.put("percentage", round2(percentage));
            revenueShare.add(item);
        }

        // Details Table
        List<Map<String, Object>> details = new ArrayList<>();
        Map<String, List<CustomerMonthlyBill>> thisYearByVendor = thisYearBills.stream()
                .filter(b -> b.getCloudVendor() != null)
                .collect(Collectors.groupingBy(CustomerMonthlyBill::getCloudVendor));

        for (Map.Entry<String, List<CustomerMonthlyBill>> entry : thisYearByVendor.entrySet()) {
            String vendor = entry.getKey();
            List<CustomerMonthlyBill> bills = entry.getValue();

            double revenue = 0;
            double customerPayable = 0;
            double cost = 0;
            double profit = 0;

            for (CustomerMonthlyBill bill : bills) {
                Financials f = calculateFinancials(bill);
                revenue += f.revenue;
                customerPayable += f.customerPayable;
                cost += f.cost;
                profit += f.profit;
            }

            double lastYearRev = lastYearRevenueByVendor.getOrDefault(vendor, 0.0);
            double yoy = 0;
            if (lastYearRev > 0) {
                yoy = ((revenue - lastYearRev) / lastYearRev) * 100;
            }

            Map<String, Object> row = new HashMap<>();
            row.put("cloud_vendor", vendor);
            row.put("revenue", round2(revenue));
            row.put("customer_payable", round2(customerPayable));
            row.put("cost", round2(cost));
            row.put("profit", round2(profit));
            row.put("margin", round2(revenue > 0 ? (profit / revenue) * 100 : 0) + "%");
            row.put("share", round2(totalRevenue > 0 ? (revenue / totalRevenue) * 100 : 0) + "%");
            row.put("yoy_growth", round2(yoy) + "%");
            details.add(row);
        }

        // Overview
        Map<String, Object> overview = new HashMap<>();
        overview.put("total_revenue", round2(totalRevenue));
        overview.put("total_cost", round2(totalCost));
        overview.put("total_tax", round2(totalTax));
        overview.put("total_profit", round2(totalProfit));

        Map<String, Object> response = new HashMap<>();
        response.put("overview", overview);
        response.put("revenue_comparison", revenueComparison);
        response.put("revenue_share", revenueShare);
        response.put("details", details);

        return ApiResponse.success("success", response);
    }

    private Financials calculateFinancials(CustomerMonthlyBill bill) {
        Double originalPct = bill.getOriginalBillingPercentage();
        Double undiscounted = bill.getUndiscountedBill();
        Double costPct = bill.getCostDiscountPercentage();

        double revenue = undiscounted != null ? undiscounted : 0.0;

        Double customerPayable = bill.getCustomerPayableBill();
        if (customerPayable == null && originalPct != null && undiscounted != null) {
            customerPayable = undiscounted * (originalPct / 100.0);
        }
        if (customerPayable == null) customerPayable = 0.0;

        Double supplierPayable = bill.getSupplierPayableBill();
        if (supplierPayable == null && costPct != null && undiscounted != null) {
            supplierPayable = undiscounted * (1.0 - (costPct / 100.0));
        }
        if (supplierPayable == null) supplierPayable = 0.0;

        Double profit = bill.getProfit();
        if (profit == null) {
            profit = customerPayable - supplierPayable;
        }

        return new Financials(revenue, customerPayable, supplierPayable, profit);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static class Financials {
        double revenue;
        double customerPayable;
        double cost;
        double profit;

        public Financials(double revenue, double customerPayable, double cost, double profit) {
            this.revenue = revenue;
            this.customerPayable = customerPayable;
            this.cost = cost;
            this.profit = profit;
        }
    }
}
