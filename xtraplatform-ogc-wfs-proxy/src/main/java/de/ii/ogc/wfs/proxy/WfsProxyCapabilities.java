package de.ii.ogc.wfs.proxy;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zahnen
 */
public class WfsProxyCapabilities {

    private boolean paging;
    private Map<String, String> errorMessages;

    WfsProxyCapabilities() {
        this.errorMessages = new HashMap<>();
    }

    public boolean isPaging() {
        return paging;
    }

    public void setPaging(boolean paging) {
        this.paging = paging;
    }

    public Map<String, String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(Map<String, String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public void addErrorMessage(String capability, String message) {
        this.errorMessages.put(capability, message);
    }
}
