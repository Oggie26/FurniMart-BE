# Danh Sách Tài Khoản Test

## Admin Accounts

### Admin Account 1
- **Email**: `string@gmail.com`
- **Password**: `string`
- **Role**: `ADMIN`
- **Employee Code**: `ADM-1763212809-045`
- **Full Name**: `Admin String`
- **Mục đích**: Test admin với email và password đơn giản
- **Trạng thái**: ✅ Đã tạo và test thành công

### Admin Account 2
- **Email**: `admin@furnimart.com`
- **Password**: `admin123`
- **Role**: `ADMIN`
- **ID**: `2c66698b-263f-461e-9c54-8e20880d7d84`
- **Full Name**: `Admin FurniMart`
- **Phone**: `0900000000`
- **Status**: `ACTIVE`
- **Mục đích**: Admin account chính của hệ thống
- **Trạng thái**: ✅ Đã tạo và test thành công (tạo lại ngày 2025-11-15)

## Test Accounts (Đã tạo)

### 1. BRANCH_MANAGER
- **Email**: `branchmanager@furnimart.com`
- **Password**: `BranchManager@123`
- **Role**: `BRANCH_MANAGER`
- **ID**: `ec474758-c388-41f1-89eb-bab1fcac35f0`
- **Full Name**: `Branch Manager Test`
- **Phone**: `0901234567`
- **Status**: `ACTIVE`
- **Created At**: `2025-11-15`
- **Mục đích**: Test các chức năng quản lý chi nhánh, assign orders
- **Trạng thái**: ✅ Đã tạo thành công

### 2. STAFF
- **Email**: `staff@furnimart.com`
- **Password**: `Staff@123`
- **Role**: `STAFF`
- **ID**: `3c98faf5-12d8-4264-a402-f7812d00719e`
- **Full Name**: `Staff Test`
- **Phone**: `0901234568`
- **Status**: `ACTIVE`
- **Created At**: `2025-11-15`
- **Mục đích**: Test các chức năng nhân viên: assign orders, generate invoices, prepare products
- **Trạng thái**: ✅ Đã tạo thành công

### 3. DELIVERY
- **Email**: `delivery@furnimart.com`
- **Password**: `Delivery@123`
- **Role**: `DELIVERY`
- **ID**: `d65e329f-f92d-49eb-b068-b61b08ec8be0`
- **Full Name**: `Delivery Staff Test`
- **Phone**: `0901234569`
- **Status**: `ACTIVE`
- **Created At**: `2025-11-15`
- **Mục đích**: Test các chức năng giao hàng: xem assignments, cập nhật trạng thái delivery
- **Trạng thái**: ✅ Đã tạo thành công

## Test Accounts từ các file test khác

### 4. MANAGER (từ test-user-apis.bat) - ✅ Đã tạo với role BRANCH_MANAGER
- **Email**: `manager@furnimart.com`
- **Password**: `manager123`
- **Role**: `BRANCH_MANAGER` (đã thay đổi từ MANAGER vì role MANAGER không tồn tại)
- **ID**: `b2e7a6db-71d4-49a0-98d2-a2968f623c9c`
- **Full Name**: `Manager Test`
- **Phone**: `0901234570`
- **Status**: `ACTIVE`
- **Created At**: `2025-11-15`
- **Mục đích**: Test các chức năng quản lý chi nhánh (tương tự BRANCH_MANAGER)
- **Trạng thái**: ✅ Đã tạo thành công
- **Lưu ý**: Account này được tạo với role BRANCH_MANAGER vì role MANAGER không tồn tại trong hệ thống

### 5. CUSTOMER (từ test-user-apis.bat)
- **Email**: `customer@gmail.com`
- **Password**: `customer123`
- **Role**: `CUSTOMER`
- **ID**: `229d4c36-fa2e-4045-b028-79f27d45ff1e` (từ lần tạo trước)
- **Full Name**: `Customer Test`
- **Status**: `ACTIVE`
- **Mục đích**: Test các chức năng khách hàng
- **Trạng thái**: ✅ Đã tạo thành công (có thể đã tồn tại từ trước)
