package io.iamcyw.tower.quarkus.it.domain;

/**
 * Test result for integration testing.
 *
 * <p>This class represents the result of processing a {@link BasicTestCommand}.
 * It contains the processed payload and a success indicator.</p>
 *
 * @see BasicTestCommand
 * @since 2.0
 */
public class BasicTestResult {

    private String processedPayload;
    private boolean success;

    /**
     * Default constructor required for serialization/deserialization.
     */
    public BasicTestResult() {
    }

    /**
     * Constructs a BasicTestResult with the given processed payload and success status.
     *
     * @param processedPayload the processed payload string
     * @param success          true if the command was processed successfully
     */
    public BasicTestResult(String processedPayload, boolean success) {
        this.processedPayload = processedPayload;
        this.success = success;
    }

    /**
     * Returns the processed payload.
     *
     * @return the processed payload string, may be null
     */
    public String getProcessedPayload() {
        return processedPayload;
    }

    /**
     * Sets the processed payload.
     *
     * @param processedPayload the processed payload string to set
     */
    public void setProcessedPayload(String processedPayload) {
        this.processedPayload = processedPayload;
    }

    /**
     * Returns whether the command was processed successfully.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets the success status.
     *
     * @param success true if successful, false otherwise
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

}
