package com.rentlaw.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthorizationDTO {
    public String token_type;
    public Long user_id;
    public String username;
    public String email;
    public String first_name;
    public String last_name;
}