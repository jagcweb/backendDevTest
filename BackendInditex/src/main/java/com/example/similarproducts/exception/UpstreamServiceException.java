package com.example.similarproducts.exception;

/**
 * Raised when the upstream catalog service cannot answer a request we cannot proceed
 * without (e.g. resolving the similar-ids list failed or timed out). Mapped to a 5xx so the
 * caller knows the failure is on our side of the integration, not theirs.
 */
public class UpstreamServiceException extends RuntimeException {

    public UpstreamServiceException(String message) {
        super(message);
    }

    public UpstreamServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
