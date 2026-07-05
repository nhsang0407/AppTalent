package com.shoplens.ai.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

/**
 * Firestore user document. Stored in the "users" collection keyed by uid.
 */
public class User {

    private String uid;
    private String name;
    private String email;
    private String role;      // "admin" or "user"
    private String address;
    private String phone;
    private String avatarUrl;
    private Timestamp createdAt;

    /** Required empty constructor for Firestore deserialization. */
    public User() {
    }

    public User(String uid, String name, String email, String role) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.address = "";
        this.phone = "";
        this.createdAt = Timestamp.now();
    }

    @PropertyName("uid")
    public String getUid() {
        return uid;
    }

    @PropertyName("uid")
    public void setUid(String uid) {
        this.uid = uid;
    }

    @PropertyName("name")
    public String getName() {
        return name;
    }

    @PropertyName("name")
    public void setName(String name) {
        this.name = name;
    }

    @PropertyName("email")
    public String getEmail() {
        return email;
    }

    @PropertyName("email")
    public void setEmail(String email) {
        this.email = email;
    }

    @PropertyName("role")
    public String getRole() {
        return role;
    }

    @PropertyName("role")
    public void setRole(String role) {
        this.role = role;
    }

    @PropertyName("address")
    public String getAddress() {
        return address;
    }

    @PropertyName("address")
    public void setAddress(String address) {
        this.address = address;
    }

    @PropertyName("phone")
    public String getPhone() {
        return phone;
    }

    @PropertyName("phone")
    public void setPhone(String phone) {
        this.phone = phone;
    }

    @PropertyName("avatarUrl")
    public String getAvatarUrl() {
        return avatarUrl;
    }

    @PropertyName("avatarUrl")
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    @PropertyName("createdAt")
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    @PropertyName("createdAt")
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
