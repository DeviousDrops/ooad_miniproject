package com.pharmacy.pattern.factory;

import org.springframework.stereotype.Component;

@Component
public class MedicineFactorySelector {

    private final TabletMedicineFactory tabletFactory;
    private final SyrupMedicineFactory syrupFactory;
    private final GenericMedicineFactory genericFactory;

    public MedicineFactorySelector(
            TabletMedicineFactory tabletFactory,
            SyrupMedicineFactory syrupFactory,
            GenericMedicineFactory genericFactory
    ) {
        this.tabletFactory = tabletFactory;
        this.syrupFactory = syrupFactory;
        this.genericFactory = genericFactory;
    }

    public MedicineFactory byType(String medicineType) {
        if (medicineType == null) {
            return genericFactory;
        }
        return switch (medicineType.trim().toUpperCase()) {
            case "TABLET" -> tabletFactory;
            case "SYRUP" -> syrupFactory;
            default -> genericFactory;
        };
    }
}
