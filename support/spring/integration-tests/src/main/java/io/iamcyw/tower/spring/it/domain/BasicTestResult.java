package io.iamcyw.tower.spring.it.domain;

/**
 * Test result for Spring Boot integration testing.
 *
 * @since 2.0
 */
public class BasicTestResult {

    private String processedPayload;
    private boolean success;

    public BasicTestResult() {
    }

    public BasicTestResult(String processedPayload, boolean success) {
        this.processedPayload = processedPayload;
        this.success = success;
    }

    public String getProcessedPayload() {
        return processedPayload;
    }

    public void setProcessedPayload(String processedPayload) {
        this.processedPayload = processedPayload;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

}
