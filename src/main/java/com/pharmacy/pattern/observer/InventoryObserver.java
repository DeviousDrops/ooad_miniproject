package com.pharmacy.pattern.observer;

import com.pharmacy.model.Medicine;

public interface InventoryObserver {

    void onInventoryAlert(Medicine medicine, String message);
}
