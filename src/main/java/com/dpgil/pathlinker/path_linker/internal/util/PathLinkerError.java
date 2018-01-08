package com.dpgil.pathlinker.path_linker.internal.util;

import org.cytoscape.ci.model.CIError;

/**
 * Class extending CIError which stores and represents errors generate by user inputs in PathLinker
 */
public class PathLinkerError extends CIError {

    /** Response String for error origin */
    public final static String RESOURCE_ERROR_ROOT = "urn:cytoscape:ci:pathlinker-app:v1";
    /** Error message for Network Not Found */
    public static final String CY_NETWORK_NOT_FOUND_ERROR = "CY_NETWORK_NOT_FOUND_ERROR";
    /** Error message for invalid user input */
    public static final String INVALID_INPUT_ERROR = "INVALID_INPUT_ERROR";
    /** Error message for path not found */
    public static final String PATH_NOT_FOUND_ERROR = "PATH_NOT_FOUND_ERROR";
    /** Error code for Network Not Found */
    public static final int CY_NETWORK_NOT_FOUND_CODE = 404;
    /** Error code for invalid user input */
    public static final int INVALID_INPUT_CODE = 422;
    /** Error code for path not found  */
    public static final int PATH_NOT_FOUND_CODE = 204;

    /** Message specifically used for generating error message on UI */
    private String uiMessage;

    /**
     * Default constructor
     * @param status    error status
     * @param type      error type
     * @param message   message for API function call
     * @param uiMessage message for UI
     */
    public PathLinkerError(Integer status, String type, String message, String uiMessage) {
        this.status = status;
        this.type = type;
        this.message = message;
        this.uiMessage = uiMessage;
    }

    /**
     * Getter method for uiMessage
     * @return uiMessage
     */
    public String getUIMessage() {
        return this.uiMessage;
    }

    /**
     * Setter method for uiMessage
     * @param uiMessage
     */
    public void setUIMessage(String uiMessage) {
        this.uiMessage = uiMessage;
    }
}
