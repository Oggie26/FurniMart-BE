package com.example.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "blogs")
public class Blog extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private Boolean status = true;

    @Column(columnDefinition = "TEXT")
    private String image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // Manual getters/setters/builder (Lombok not working in some environments)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public static BlogBuilder builder() {
        return new BlogBuilder();
    }

    public static class BlogBuilder {
        private Integer id;
        private String name;
        private String content;
        private Boolean status = true;
        private String image;
        private Employee employee;
        private Boolean isDeleted = false;

        public BlogBuilder id(Integer id) {
            this.id = id;
            return this;
        }

        public BlogBuilder name(String name) {
            this.name = name;
            return this;
        }

        public BlogBuilder content(String content) {
            this.content = content;
            return this;
        }

        public BlogBuilder status(Boolean status) {
            this.status = status;
            return this;
        }

        public BlogBuilder image(String image) {
            this.image = image;
            return this;
        }

        public BlogBuilder employee(Employee employee) {
            this.employee = employee;
            return this;
        }

        public BlogBuilder isDeleted(Boolean isDeleted) {
            this.isDeleted = isDeleted;
            return this;
        }

        public Blog build() {
            Blog blog = new Blog();
            blog.setId(this.id);
            blog.setName(this.name);
            blog.setContent(this.content);
            blog.setStatus(this.status);
            blog.setImage(this.image);
            blog.setEmployee(this.employee);
            blog.setIsDeleted(this.isDeleted);
            return blog;
        }
    }
}
