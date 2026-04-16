package com.pharmacy.pattern.observer;

import com.pharmacy.domain.Medicine;

public interface InventoryObserver {

    void onInventoryAlert(Medicine medicine, String message);
}
