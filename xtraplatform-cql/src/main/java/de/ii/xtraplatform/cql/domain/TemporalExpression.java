/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import java.util.Optional;

public interface TemporalExpression {

    Optional<After> getAfter();

    Optional<Before> getBefore();

    Optional<Begins> getBegins();

    Optional<BegunBy> getBegunBy();

    Optional<TContains> getTContains();

    Optional<During> getDuring();

    Optional<EndedBy> getEndedBy();

    Optional<Ends> getEnds();

    Optional<TEquals> getTEquals();

    Optional<Meets> getMeets();

    Optional<MetBy> getMetBy();

    Optional<TOverlaps> getTOverlaps();

    Optional<OverlappedBy> getOverlappedBy();

    Optional<AnyInteracts> getAnyInteracts();

}
