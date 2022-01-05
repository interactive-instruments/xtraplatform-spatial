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

    Optional<TAfter> getTAfter();

    Optional<TBefore> getTBefore();

    Optional<TStarts> getTStarts();

    Optional<TStartedBy> getTStartedBy();

    Optional<TContains> getTContains();

    Optional<TDuring> getTDuring();

    Optional<TFinishedBy> getTFinishedBy();

    Optional<TFinishes> getTFinishes();

    Optional<TEquals> getTEquals();

    Optional<TMeets> getTMeets();

    Optional<TMetBy> getTMetBy();

    Optional<TOverlaps> getTOverlaps();

    Optional<TOverlappedBy> getTOverlappedBy();

    Optional<TDisjoint> getTDisjoint();

}
