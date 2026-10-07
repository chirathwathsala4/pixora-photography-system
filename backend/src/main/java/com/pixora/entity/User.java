package com.pixora.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    //Generates a unique value for the user id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    //Fullname of the user
    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    //Unique email for each user
    @Column(name = "email", nullable = false, unique = true, length = 200)
    private String email;

    //User password
    @JsonIgnore
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "phone", length = 20)
    private String phone;

    //Role of the user
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    //URL of the portfolio
    @Column(name = "portfolio_url", length = 500)
    private String portfolioUrl;

    //User account status
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus;

    //List of client bookings
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonIgnore
    private List<Booking> clientBookings;

    //List of photographer bookings
    @OneToMany(mappedBy = "photographer", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonIgnore
    private List<Booking> photographerBookings;

    //List of reviews made by clients
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonIgnore
    private List<Review> clientReviews;

    //List of photographer reviews
    @OneToMany(mappedBy = "photographer", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonIgnore
    private List<Review> photographerReviews;

    //List of photos uploaded by photographers
    @OneToMany(mappedBy = "photographer", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonIgnore
    private List<Photo> photos;

    //List of user notifications
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonIgnore
    private List<Notification> notifications;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return accountStatus != AccountStatus.REJECTED; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return accountStatus == AccountStatus.ACTIVE; }

    public enum Role { ADMIN, CLIENT, PHOTOGRAPHER }

    //Account Status is either PENDING_APPROVAL, ACTIVE, REJECTED
    public enum AccountStatus { PENDING_APPROVAL, ACTIVE, REJECTED }
}