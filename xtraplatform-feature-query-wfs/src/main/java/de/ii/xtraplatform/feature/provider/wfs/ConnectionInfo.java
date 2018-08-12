package de.ii.xtraplatform.feature.provider.wfs;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import org.immutables.value.Value;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableConnectionInfo.class)
public abstract class ConnectionInfo {

    enum METHOD {GET,POST}

    public abstract URI getUri();
    public abstract METHOD getMethod();
    public abstract String getVersion();
    public abstract String getGmlVersion();
    public abstract Optional<String> getUser();
    public abstract Optional<String> getPassword();
    public abstract Map<String,String> getOtherUrls();
    public abstract Map<String,String> getNamespaces();
    public abstract EpsgCrs getNativeCrs();
}
