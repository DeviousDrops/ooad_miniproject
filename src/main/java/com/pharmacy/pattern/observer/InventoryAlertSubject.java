package com.pharmacy.pattern.observer;

import com.pharmacy.domain.Medicine;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryAlertSubject {

    private final List<InventoryObserver> observers;

    public InventoryAlertSubject(List<InventoryObserver> observers) {
        this.observers = observers;
    }

    public void notifyLowStockOrExpiry(Medicine medicine) {
        if (medicine.isLowStock()) {
            notifyAllObservers(medicine, "Stock quantity is below threshold: " + medicine.getStockQty());
        }
        if (medicine.isNearExpiry(30)) {
            notifyAllObservers(medicine, "Medicine is near expiry date: " + medicine.getExpiryDate());
        }
    }

    private void notifyAllObservers(Medicine medicine, String message) {
        for (InventoryObserver observer : observers) {
            observer.onInventoryAlert(medicine, message);
        }
    }
}
