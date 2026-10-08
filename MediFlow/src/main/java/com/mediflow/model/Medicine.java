package com.mediflow.model;

public class Medicine {
    private int medicineId;
    private String name;
    private String description;
    private String dosageForm;
    private String strength;

    public Medicine() {}

    public Medicine(String name, String description, String dosageForm, String strength) {
        this.name = name;
        this.description = description;
        this.dosageForm = dosageForm;
        this.strength = strength;
    }

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDosageForm() { return dosageForm; }
    public void setDosageForm(String dosageForm) { this.dosageForm = dosageForm; }
    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }
}
