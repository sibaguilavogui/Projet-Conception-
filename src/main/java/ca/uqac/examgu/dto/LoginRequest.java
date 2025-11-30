package ca.uqac.examgu.dto;

public class LoginRequest {
    private String email;
    private String password;

    public LoginRequest(String email, String motDePasse) {
        this.email = email;
        this.password = motDePasse;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password= password; }
}
