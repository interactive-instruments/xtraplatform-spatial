package de.ii.xtraplatform.feature.provider.pgis;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableConnectionInfo.class)
public abstract class ConnectionInfo {

    public abstract String getHost();
    public abstract String getDatabase();
    public abstract String getUser();
    public abstract String getPassword();
    public abstract EpsgCrs getNativeCrs();
}
