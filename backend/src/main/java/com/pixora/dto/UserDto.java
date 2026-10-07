package com.pixora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    //Unique identifier for the user
    private Long userId;

    //Full name of the user
    private String fullName;

    //User email
    private String email;

    private String phone;

    //Role of the user
    private String role;

    //URL for the website portfolio
    private String portfolioUrl;

    //Status of the account
    private String accountStatus;
}
