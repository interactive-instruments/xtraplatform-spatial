package de.ii.ogc.wfs.proxy;

import java.util.List;

/**
 * @author zahnen
 */
public class WfsProxyMappingStatus {

    private boolean enabled;
    private boolean loading;
    private boolean supported;
    private String errorMessage;
    private List<String> errorMessageDetails;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public boolean isSupported() {
        return supported;
    }

    public void setSupported(boolean supported) {
        this.supported = supported;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<String> getErrorMessageDetails() {
        return errorMessageDetails;
    }

    public void setErrorMessageDetails(List<String> errorMessageDetails) {
        this.errorMessageDetails = errorMessageDetails;
    }
}
