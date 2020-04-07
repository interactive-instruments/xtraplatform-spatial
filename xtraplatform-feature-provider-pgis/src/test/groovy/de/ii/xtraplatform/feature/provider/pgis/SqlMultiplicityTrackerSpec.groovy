/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis

import de.ii.xtraplatform.feature.provider.sql.app.SqlMultiplicityTracker
import spock.lang.Specification

/**
 * @author zahnen
 */
class SqlMultiplicityTrackerSpec extends Specification {
/*
    //TODO non-multi tables, clean path (outside? only multi?), clean ids (outside?), expected errors

    //TODO preconditions: SlickRowInfo has path and ids, SqlPathTree.getAdditionalSortKeys adds sort key for every multi table, mapping only uses valid multi tables in json names

    def 'return levels'() {

        given: "Five levels (1,2,3,4,5) for path /a/b/c/d/e"

        def tracker = getTracker(['a','b','c','d','e'], [1,2,3,4,5])

        when: "levels are requested"

        then: '(1,2,3,4,5) is returned'

        tracker.getMultiplicitiesForPath(['a','b','c','d','e']) == [1,2,3,4,5]
    }

    def 'increase'() {

        given: "One level (1) for path /a"

        def tracker = getTracker(['a'], [1])

        when: "a new id for a is tracked"

        tracker.track(['a'], ['ignored', '2'])

        then: 'the level for a should be 2'

        tracker.getMultiplicitiesForPath(['a']) == [2]
    }

    def 'increase child'() {

        given: "Two levels (1,1) for path /a/b"

        def tracker = getTracker(['a','b'], [1,1])

        when: "a new id for b is tracked"

        tracker.track(['a','b'], ['ignored', '1', '2'])

        then: 'the level for b should be 2'

        tracker.getMultiplicitiesForPath(['b']) == [2]

        and: 'the level for a should be unchanged'

        tracker.getMultiplicitiesForPath(['a']) == [1]
    }

    def 'reset children on increase'() {

        given: "Three levels (1,2,3) for path /a/b/c"

        def tracker = getTracker(['a', 'b', 'c'], [1,2,3])

        when: "a new id for a and unchanged ids for b and c are tracked"

        tracker.track(['a','b','c'], ['ignored', '2','2','3'])

        then: 'the level for a should be 2'

        tracker.getMultiplicitiesForPath(['a']) == [2]

        and: 'the levels for b and c should be reset to 1'

        tracker.getMultiplicitiesForPath(['b','c']) == [1,1]
    }

    def 'reset children that were seen before on increase'() {

        given: "Three levels (1,2,3) for path /a/b/c"

        def tracker = getTracker(['a', 'b', 'c'], [2,2,3])

        when: "a new id for b is tracked"

        tracker.track(['b'], ['ignored', '3'])

        then: 'the level for b should be 3'

        tracker.getMultiplicitiesForPath(['b']) == [3]

        and: 'the level for a should be unchanged'

        tracker.getMultiplicitiesForPath(['a']) == [2]

        and: 'the level for c should be reset to 1'

        tracker.getMultiplicitiesForPath(['c']) == [1]
    }

    def getTracker(List<String> path, List<Integer> levels) {
        def tracker = new SqlMultiplicityTracker(path)

        def ids = [1] * levels.size()

        for (int i = 0; i < levels.size(); i++) {
            for (int j = 1; j <= levels.get(i); j++) {
                ids[i] = j
                tracker.track(path, ids)
            }
        }

        return tracker
    }

 */
}
