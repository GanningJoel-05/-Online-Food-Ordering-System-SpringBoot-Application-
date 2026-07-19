package com.foodapp.dto.request;

import com.foodapp.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data   // Lombok: generates getters/setters/toString/equals/hashCode in one line
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phoneNumber;

    @NotBlank(message = "Role is required")
    private Role role;   // CUSTOMER or RESTAURANT_OWNER at signup; ADMIN created manually
}
