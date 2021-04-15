package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaults;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.encoding.BuildableMapEncodingEnabled;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@BuildableMapEncodingEnabled
@JsonDeserialize(builder = ImmutableFeatureProviderCommonData.Builder.class)
public interface FeatureProviderCommonData extends FeatureProviderDataV2, WithConnectionInfo<ConnectionInfo> {

  //TODO: only needed for MigrationV1V2?
  @JsonIgnore
  @Nullable
  @Override
  ConnectionInfo getConnectionInfo();

  abstract class Builder extends
      FeatureProviderDataV2.Builder<ImmutableFeatureProviderCommonData.Builder> implements
      EntityDataBuilder<FeatureProviderDataV2> {

    @Override
    public Builder fillRequiredFieldsWithPlaceholders() {
      return this.id(EntityDataDefaults.PLACEHOLDER)
          .providerType(EntityDataDefaults.PLACEHOLDER)
          .featureProviderType(EntityDataDefaults.PLACEHOLDER);
    }

  }
}
