package com.pharmacy.service;

import com.pharmacy.model.Medicine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class InventoryObserverTest {

    @Test
    void checkLowStock_shouldNotifyListenersAndStoreAlert() {
        InventoryObserver observer = new InventoryObserver();
        InventoryObserver.StockAlertListener listener = mock(InventoryObserver.StockAlertListener.class);
        observer.register(listener);

        Medicine medicine = new Medicine();
        medicine.setName("Cough Syrup");
        medicine.setStockQty(4);

        observer.checkLowStock(medicine);

        assertEquals(1, observer.latestAlerts().size());
        assertTrue(observer.latestAlerts().get(0).contains("Low stock alert"));
        verify(listener).onLowStock(medicine, observer.latestAlerts().get(0));
    }

    @Test
    void checkLowStock_shouldNotNotifyForSafeQuantity() {
        InventoryObserver observer = new InventoryObserver();
        InventoryObserver.StockAlertListener listener = mock(InventoryObserver.StockAlertListener.class);
        observer.register(listener);

        Medicine medicine = new Medicine();
        medicine.setName("Vitamin C");
        medicine.setStockQty(40);

        observer.checkLowStock(medicine);

        assertTrue(observer.latestAlerts().isEmpty());
        verifyNoInteractions(listener);
    }
}
