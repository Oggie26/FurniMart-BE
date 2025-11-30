package com.example.userservice.entity;

import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "accounts", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "email")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Account extends AbstractEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumStatus status;

    @Builder.Default
    private boolean enabled = true;
    @Builder.Default
    private boolean accountNonExpired = true;
    @Builder.Default
    private boolean accountNonLocked = true;
    @Builder.Default
    private boolean credentialsNonExpired = true;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL)
    private User user;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL)
    private Employee employee;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // Manual getters/setters/builder (Lombok not working in Docker build)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public EnumRole getRole() { return role; }
    public void setRole(EnumRole role) { this.role = role; }
    public EnumStatus getStatus() { return status; }
    public void setStatus(EnumStatus status) { this.status = status; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setAccountNonExpired(boolean accountNonExpired) { this.accountNonExpired = accountNonExpired; }
    public void setAccountNonLocked(boolean accountNonLocked) { this.accountNonLocked = accountNonLocked; }
    public void setCredentialsNonExpired(boolean credentialsNonExpired) { this.credentialsNonExpired = credentialsNonExpired; }

    // Builder pattern
    public static AccountBuilder builder() {
        return new AccountBuilder();
    }

    public static class AccountBuilder {
        private String id;
        private String email;
        private String password;
        private EnumRole role;
        private EnumStatus status;
        private boolean enabled = true;
        private boolean accountNonExpired = true;
        private boolean accountNonLocked = true;
        private boolean credentialsNonExpired = true;
        private User user;
        private Employee employee;

        public AccountBuilder id(String id) { this.id = id; return this; }
        public AccountBuilder email(String email) { this.email = email; return this; }
        public AccountBuilder password(String password) { this.password = password; return this; }
        public AccountBuilder role(EnumRole role) { this.role = role; return this; }
        public AccountBuilder status(EnumStatus status) { this.status = status; return this; }
        public AccountBuilder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public AccountBuilder accountNonExpired(boolean accountNonExpired) { this.accountNonExpired = accountNonExpired; return this; }
        public AccountBuilder accountNonLocked(boolean accountNonLocked) { this.accountNonLocked = accountNonLocked; return this; }
        public AccountBuilder credentialsNonExpired(boolean credentialsNonExpired) { this.credentialsNonExpired = credentialsNonExpired; return this; }
        public AccountBuilder user(User user) { this.user = user; return this; }
        public AccountBuilder employee(Employee employee) { this.employee = employee; return this; }

        public Account build() {
            Account account = new Account();
            account.id = this.id;
            account.email = this.email;
            account.password = this.password;
            account.role = this.role;
            account.status = this.status;
            account.enabled = this.enabled;
            account.accountNonExpired = this.accountNonExpired;
            account.accountNonLocked = this.accountNonLocked;
            account.credentialsNonExpired = this.credentialsNonExpired;
            account.user = this.user;
            account.employee = this.employee;
            return account;
        }
    }
}