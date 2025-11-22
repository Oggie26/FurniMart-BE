-- Migration: Make delivery_staff_id nullable again
-- Date: 2025-01-21
-- Description: Rollback V999 - Allow delivery_staff_id to be NULL
--              This is necessary because assignments can be created before delivery staff is assigned
--              - prepareProducts() creates assignment with deliveryStaffId = null
--              - generateInvoice() creates assignment with deliveryStaffId = null
--              - assignOrderToDelivery() sets deliveryStaffId when assigning to delivery staff
--
-- Reason: Assignment is used to track progress (productsPrepared, invoiceGenerated, status)
--         and does not require delivery_staff_id until assignOrderToDelivery() is called

-- Step 1: Make delivery_staff_id nullable
ALTER TABLE delivery_assignments 
MODIFY COLUMN delivery_staff_id VARCHAR(255) NULL 
COMMENT 'Nullable until assigned to delivery staff. Assignment can be created before delivery staff assignment.';

-- Step 2: Add comment to document the change
-- Note: This allows assignments to be created during prepareProducts() and generateInvoice()
--       before delivery staff is assigned via assignOrderToDelivery()

