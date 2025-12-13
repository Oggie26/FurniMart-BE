package com.example.remotetests;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Test Suite để chạy tất cả integration tests
 * 
 * Usage: mvn test -Dtest=AllIntegrationTests
 */
@Suite
@SuiteDisplayName("FurniMart Remote Integration Tests - All Modules")
@SelectPackages({
    "com.example.remotetests.user",
    "com.example.remotetests.product",
    "com.example.remotetests.order",
    "com.example.remotetests.inventory",
    "com.example.remotetests.delivery",
    "com.example.remotetests.ai"
})
public class AllIntegrationTests {
    // Test suite runner
}

