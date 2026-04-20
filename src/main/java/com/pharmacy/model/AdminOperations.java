package com.pharmacy.model;

import java.util.List;

// ISP: admin contract isolates inventory/analytics responsibilities.
public interface AdminOperations {

    void manageMedicineInventory();

    Report generateSalesAnalytics();

    List<String> fetchSalesData();

    void automateMedicineSupply();
}
