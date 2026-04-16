package com.pharmacy.pattern.observer;

import com.pharmacy.domain.Medicine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class AdminAlertObserver implements InventoryObserver {

    private static final Logger log = LoggerFactory.getLogger(AdminAlertObserver.class);

    private final List<String> alerts = new ArrayList<>();

    @Override
    public synchronized void onInventoryAlert(Medicine medicine, String message) {
        String payload = "[ADMIN ALERT] " + medicine.getName() + " - " + message;
        alerts.add(0, payload);
        if (alerts.size() > 200) {
            alerts.remove(alerts.size() - 1);
        }
        log.warn(payload);
    }

    public synchronized List<String> latestAlerts() {
        return Collections.unmodifiableList(alerts);
    }
}
