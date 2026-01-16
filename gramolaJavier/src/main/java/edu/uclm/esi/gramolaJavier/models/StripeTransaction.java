package edu.uclm.esi.gramolaJavier.models;

import jakarta.persistence.*;
import org.json.JSONObject;

@Entity
@Table(name = "stripe_transactions")
public class StripeTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "data", columnDefinition = "TEXT")
    private String data;
    
    @Column(name = "email")
    private String email;
    
    public StripeTransaction() {}
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getData() {
        return data;
    }
    
    public void setData(String data) {
        this.data = data;
    }
    
    public void setData(JSONObject json) {
        this.data = json.toString();
    }
    
    public JSONObject getDataAsJson() {
        return new JSONObject(this.data);
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
}