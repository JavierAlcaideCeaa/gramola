package edu.uclm.esi.gramolaJavier.models;

import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "tokens")
public class Token {
    @Id
    private String id;
    
    @Column(nullable = false)
    private long creationTime;
    
    @Column(nullable = false)
    private boolean used;
    
    public Token() {
        this.id = UUID.randomUUID().toString();
        this.creationTime = System.currentTimeMillis();
        this.used = false;
    }
    
    public void Use() {
        this.used = true;
    }
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public long getCreationTime() { return creationTime; }
    public void setCreationTime(long creationTime) { this.creationTime = creationTime; }
    
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
}