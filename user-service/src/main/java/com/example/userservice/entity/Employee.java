package com.example.userservice.entity;

import com.example.userservice.enums.EnumStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "employees")
public class Employee extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(unique = true, nullable = false)
    String code;

    @Column
    String fullName;

    @Column(unique = true)
    String phone;

    @Column
    @Temporal(TemporalType.DATE)
    Date birthday;

    @Column
    Boolean gender;

    @Enumerated(EnumType.STRING)
    EnumStatus status;

    @Column
    String avatar;

    @Column(unique = true, length = 20)
    String cccd;

    @Column
    String department;

    @Column
    String position;

    @Column
    BigDecimal salary;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EmployeeStore> employeeStores;

    // Manual getters/setters/builder (Lombok not working in Docker build)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Date getBirthday() { return birthday; }
    public void setBirthday(Date birthday) { this.birthday = birthday; }
    public Boolean getGender() { return gender; }
    public void setGender(Boolean gender) { this.gender = gender; }
    public EnumStatus getStatus() { return status; }
    public void setStatus(EnumStatus status) { this.status = status; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getCccd() { return cccd; }
    public void setCccd(String cccd) { this.cccd = cccd; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public List<EmployeeStore> getEmployeeStores() { return employeeStores; }
    public void setEmployeeStores(List<EmployeeStore> employeeStores) { this.employeeStores = employeeStores; }

    // Builder pattern
    public static EmployeeBuilder builder() {
        return new EmployeeBuilder();
    }

    public static class EmployeeBuilder {
        private String id;
        private String code;
        private String fullName;
        private String phone;
        private Date birthday;
        private Boolean gender;
        private EnumStatus status;
        private String avatar;
        private String cccd;
        private String department;
        private String position;
        private BigDecimal salary;
        private Account account;
        private List<EmployeeStore> employeeStores;

        public EmployeeBuilder id(String id) { this.id = id; return this; }
        public EmployeeBuilder code(String code) { this.code = code; return this; }
        public EmployeeBuilder fullName(String fullName) { this.fullName = fullName; return this; }
        public EmployeeBuilder phone(String phone) { this.phone = phone; return this; }
        public EmployeeBuilder birthday(Date birthday) { this.birthday = birthday; return this; }
        public EmployeeBuilder gender(Boolean gender) { this.gender = gender; return this; }
        public EmployeeBuilder status(EnumStatus status) { this.status = status; return this; }
        public EmployeeBuilder avatar(String avatar) { this.avatar = avatar; return this; }
        public EmployeeBuilder cccd(String cccd) { this.cccd = cccd; return this; }
        public EmployeeBuilder department(String department) { this.department = department; return this; }
        public EmployeeBuilder position(String position) { this.position = position; return this; }
        public EmployeeBuilder salary(BigDecimal salary) { this.salary = salary; return this; }
        public EmployeeBuilder account(Account account) { this.account = account; return this; }
        public EmployeeBuilder employeeStores(List<EmployeeStore> employeeStores) { this.employeeStores = employeeStores; return this; }

        public Employee build() {
            Employee employee = new Employee();
            employee.id = this.id;
            employee.code = this.code;
            employee.fullName = this.fullName;
            employee.phone = this.phone;
            employee.birthday = this.birthday;
            employee.gender = this.gender;
            employee.status = this.status;
            employee.avatar = this.avatar;
            employee.cccd = this.cccd;
            employee.department = this.department;
            employee.position = this.position;
            employee.salary = this.salary;
            employee.account = this.account;
            employee.employeeStores = this.employeeStores;
            return employee;
        }
    }
}

