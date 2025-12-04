package com.example.userservice.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlogRequest {

    @NotBlank(message = "Blog name is required")
    private String name;

    @NotBlank(message = "Blog content is required")
    private String content;

    private String employeeId;

    @NotNull(message = "Status is required")
    private Boolean status;

    private String image;
}
