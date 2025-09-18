package com.example.userservice.service.inteface;

import com.example.userservice.request.StaffRequest;
import com.example.userservice.request.StaffUpdateRequest;
import com.example.userservice.response.PageResponse;
import com.example.userservice.response.StaffResponse;

import java.util.List;

public interface StaffService {

    StaffResponse createStaff(StaffRequest staffRequest);

    StaffResponse updateStaff(String id, StaffUpdateRequest staffRequest);

    StaffResponse getStaffById(String id);

    List<StaffResponse> getAllStaff();

    List<StaffResponse> getStaffByStatus(String status);

    PageResponse<StaffResponse> getStaffWithPagination(int page, int size);

    PageResponse<StaffResponse> searchStaff(String searchTerm, int page, int size);

    void deleteStaff(String id);

    void disableStaff(String id);

    void enableStaff(String id);

    StaffResponse getStaffByEmail(String email);

    StaffResponse getStaffByPhone(String phone);

    List<StaffResponse> getStaffByDepartment(String department);
}
