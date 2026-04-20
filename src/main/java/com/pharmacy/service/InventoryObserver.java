package com.pharmacy.service;

import com.pharmacy.model.Medicine;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InventoryObserver {

    public static final int LOW_STOCK_THRESHOLD = 10;

    private final List<StockAlertListener> listeners = new CopyOnWriteArrayList<>();
    private final List<String> alertLog = new CopyOnWriteArrayList<>();

    public InventoryObserver() {
    }

    public void register(StockAlertListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void unregister(StockAlertListener listener) {
        listeners.remove(listener);
    }

    // Observer Pattern: one inventory event fan-outs to multiple interested listeners.
    public void checkLowStock(Medicine medicine) {
        if (medicine == null || medicine.getStockQty() == null) {
            return;
        }
        if (medicine.getStockQty() < LOW_STOCK_THRESHOLD) {
            String message = "Low stock alert for " + medicine.getName() + " (qty=" + medicine.getStockQty() + ") at " + LocalDateTime.now();
            alertLog.add(0, message);
            for (StockAlertListener listener : listeners) {
                listener.onLowStock(medicine, message);
            }
        }
    }

    public List<String> latestAlerts() {
        return new ArrayList<>(alertLog);
    }

    public interface StockAlertListener {
        void onLowStock(Medicine medicine, String message);
    }
}
