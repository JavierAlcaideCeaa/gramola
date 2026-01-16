package edu.uclm.esi.gramolaJavier.models;

import jakarta.persistence.*;

@Entity
@Table(name = "prices")
public class Price {
    
    @Id
    private Long priceCents;
    
    @Column(nullable = false)
    private String description;
    
    @Column(nullable = false)
    private String currency = "eur";
    
    public Price() {}
    
    public Price(Long priceCents, String description) {
        this.priceCents = priceCents;
        this.description = description;
    }
    
    public Long getPriceCents() {
        return priceCents;
    }
    
    public void setPriceCents(Long priceCents) {
        this.priceCents = priceCents;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public double getEuros() {
        return priceCents / 100.0;
    }
}