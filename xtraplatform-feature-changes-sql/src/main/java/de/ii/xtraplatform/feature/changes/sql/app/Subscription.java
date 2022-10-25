/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.changes.sql.app;

import de.ii.xtraplatform.base.domain.util.Tuple;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.immutables.value.Value;

@Value.Immutable
interface Subscription {

  String getType();

  String getTable();

  String getIdColumn();

  Optional<String> getGeometryColumn();

  Optional<Tuple<String, String>> getIntervalColumns();

  Optional<String> getInstantColumn();

  Optional<Connection> getCurrentConnection();

  Supplier<Connection> getConnectionFactory();

  Function<Connection, List<String>> getNotificationPoller();

  @Value.Derived
  default String getChannel() {
    return String.format("%s_%s", getType(), getTable());
  }

  @Value.Lazy
  default Connection getConnection() {
    return getCurrentConnection().orElseThrow(() -> new IllegalStateException("Not connected"));
  }

  default Subscription connect() {
    return ((ImmutableSubscription) this).withCurrentConnection(getConnectionFactory().get());
  }

  default boolean isConnected() {
    try {
      return getCurrentConnection().isPresent() && getCurrentConnection().get().isValid(1);
    } catch (SQLException e) {
      // continue
    }
    return false;
  }

  default List<String> pollNotifications() {
    return getNotificationPoller().apply(getConnection());
  }

  // TODO: handle empty geo, begin, end
  @Value.Derived
  default String getCreateFunction() {
    String template =
        "CREATE OR REPLACE FUNCTION pg_temp.%1$s_trigger()\n"
            + " RETURNS trigger\n"
            + " LANGUAGE plpgsql\n"
            + "AS $function$\n"
            + "   DECLARE\n"
            + "      bbox box2d;\n"
            + "   BEGIN\n"
            + "        IF (TG_OP = 'DELETE') THEN\n"
            + "            bbox = box2d(ST_Transform(OLD.%4$s,4326));\n"
            + "            PERFORM pg_notify('%1$s', concat_ws(',', TG_OP, OLD.%5$s, COALESCE(to_char(OLD.%2$s, 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"'),'NULL'), COALESCE(to_char(OLD.%3$s, 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"'),'NULL'), st_xmin(bbox), st_ymin(bbox), st_xmax(bbox), st_ymax(bbox)));\n"
            + "            RETURN OLD;\n"
            + "        ELSE\n"
            + "            bbox = box2d(ST_Transform(NEW.%4$s,4326));\n"
            + "            PERFORM pg_notify('%1$s', concat_ws(',', TG_OP, NEW.%5$s, COALESCE(to_char(NEW.%2$s, 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"'),'NULL'), COALESCE(to_char(NEW.%3$s, 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"'),'NULL'), st_xmin(bbox), st_ymin(bbox), st_xmax(bbox), st_ymax(bbox)));\n"
            + "            RETURN NEW;\n"
            + "        END IF;\n"
            + "    END;\n"
            + "$function$\n"
            + ";";

    return String.format(
        template,
        getChannel(),
        getIntervalColumns().get().first(),
        getIntervalColumns().get().second(),
        getGeometryColumn().get(),
        getIdColumn());
  }

  @Value.Derived
  default String getCreateTrigger() {
    String template =
        "CREATE TRIGGER %1$s_trigger\n"
            + "AFTER INSERT OR UPDATE OR DELETE ON %2$s\n"
            + "    FOR EACH ROW EXECUTE PROCEDURE pg_temp.%1$s_trigger();";

    return String.format(template, getChannel(), getTable());
  }

  @Value.Derived
  default String getListen() {
    return String.format("LISTEN %s;", getChannel());
  }
}
