package com.rentlaw.transactionservice.model;

import lombok.*;

@Generated
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String id;
    private String username;
    private String email;
    private String first_name;
    private String last_name;
}
