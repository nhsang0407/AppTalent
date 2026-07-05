package com.shoplens.ai.model;

import com.google.firebase.firestore.PropertyName;

/**
 * Represents a shipping/delivery address stored in the Firestore subcollection
 * users/{uid}/addresses.
 */
public class Address {

    private String id;
    private String receiverName;
    private String phone;
    private String detail;
    private String ward;
    private String district;
    private String city;
    private String label;   // "Home", "Work", "Other"
    private boolean isDefault;

    /** Required empty constructor for Firestore deserialization. */
    public Address() {
    }

    public Address(String receiverName, String phone, String detail,
                   String ward, String district, String city,
                   String label, boolean isDefault) {
        this.receiverName = receiverName;
        this.phone = phone;
        this.detail = detail;
        this.ward = ward;
        this.district = district;
        this.city = city;
        this.label = label;
        this.isDefault = isDefault;
    }

    @PropertyName("id")
    public String getId() { return id; }
    @PropertyName("id")
    public void setId(String id) { this.id = id; }

    @PropertyName("receiverName")
    public String getReceiverName() { return receiverName; }
    @PropertyName("receiverName")
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    @PropertyName("phone")
    public String getPhone() { return phone; }
    @PropertyName("phone")
    public void setPhone(String phone) { this.phone = phone; }

    @PropertyName("detail")
    public String getDetail() { return detail; }
    @PropertyName("detail")
    public void setDetail(String detail) { this.detail = detail; }

    @PropertyName("ward")
    public String getWard() { return ward; }
    @PropertyName("ward")
    public void setWard(String ward) { this.ward = ward; }

    @PropertyName("district")
    public String getDistrict() { return district; }
    @PropertyName("district")
    public void setDistrict(String district) { this.district = district; }

    @PropertyName("city")
    public String getCity() { return city; }
    @PropertyName("city")
    public void setCity(String city) { this.city = city; }

    @PropertyName("label")
    public String getLabel() { return label; }
    @PropertyName("label")
    public void setLabel(String label) { this.label = label; }

    @PropertyName("isDefault")
    public boolean isDefault() { return isDefault; }
    @PropertyName("isDefault")
    public void setDefault(boolean aDefault) { isDefault = aDefault; }

    /** Returns a single formatted string of the full address. */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder(detail);
        if (ward != null && !ward.isEmpty()) sb.append(", ").append(ward);
        if (district != null && !district.isEmpty()) sb.append(", ").append(district);
        if (city != null && !city.isEmpty()) sb.append(", ").append(city);
        return sb.toString();
    }
}
