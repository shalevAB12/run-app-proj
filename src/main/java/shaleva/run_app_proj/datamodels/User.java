package shaleva.run_app_proj.datamodels;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "Users")
public class User {

    @Id
    private String email; 
    private String username; 
    private String password; 
    private double weightKg;
    private double heightCm;
    private long birthDate;
    private long createdAt;

    // קונסטרקטור ריק (חובה עבור מונגו ו-Jackson)
    public User() {
        this.createdAt = System.currentTimeMillis();
    }

    // קונסטרקטור מלא לשימוש בקוד
    public User(String username, String email, String password, double weightKg, double heightCm, long birthDate) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.weightKg = weightKg;
        this.heightCm = heightCm;
        this.birthDate = birthDate;
        this.createdAt = System.currentTimeMillis();
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setpassword(String password) { this.password = password; }

    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }

    public double getHeightCm() { return heightCm; }
    public void setHeightCm(double heightCm) { this.heightCm = heightCm; }

    public long getBirthDate() { return birthDate; }
    public void setBirthDate(long birthDate) { this.birthDate = birthDate; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public void setCreatedAt() {
        createdAt = System.currentTimeMillis();
    }
}