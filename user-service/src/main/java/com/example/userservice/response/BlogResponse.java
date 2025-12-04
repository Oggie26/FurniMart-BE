package com.example.userservice.response;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlogResponse {

    private Integer id;
    private String name;
    private String content;
    private Boolean status;
    private String employeeId;
    private String employeeName;
    private Date createdAt;
    private Date updatedAt;
    private String image;
}
