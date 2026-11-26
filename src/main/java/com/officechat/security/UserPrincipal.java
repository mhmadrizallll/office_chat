
package com.officechat.security;

import com.officechat.entity.User;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails, Principal {

    private final Long userId;
    private final String employeeId;
    private final String email;
    private final String password;
    private final String role;

    public UserPrincipal(User user) {
        this.userId = user.getUserId();
        this.employeeId = user.getEmployeeId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.role = user.getRole();
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String getName() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
            new SimpleGrantedAuthority("ROLE_" + role)
        );
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
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
