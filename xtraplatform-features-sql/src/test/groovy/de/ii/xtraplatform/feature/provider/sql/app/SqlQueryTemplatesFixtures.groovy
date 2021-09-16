/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app


import de.ii.xtraplatform.features.domain.Tuple

class SqlQueryTemplatesFixtures {

    static String META = "WITH\n" +
            "    NR AS (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM externalprovider A ORDER BY SKEY LIMIT 10 OFFSET 10) AS IDS),\n" +
            "    NM AS (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM externalprovider A ORDER BY 1) AS IDS) \n" +
            "  SELECT * FROM NR, NM"

    static String META_WITHOUT_NUMBER_MATCHED = "WITH\n" +
                    "    NR AS (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM externalprovider A ORDER BY SKEY LIMIT 10 OFFSET 10) AS IDS)\n" +
                    "  SELECT *, -1 AS numberMatched FROM NR"

    static String META_SORT_BY = "WITH\n" +
            "    NR AS (SELECT NULL AS minKey, NULL AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM externalprovider A ORDER BY CSKEY_0, SKEY LIMIT 10 OFFSET 10) AS IDS),\n" +
            "    NM AS (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM externalprovider A ORDER BY 1) AS IDS) \n" +
            "  SELECT * FROM NR, NM"

    static String META_SORT_BY_DESC = "WITH\n" +
            "    NR AS (SELECT NULL AS minKey, NULL AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM externalprovider A ORDER BY CSKEY_0 DESC, SKEY LIMIT 10 OFFSET 10) AS IDS),\n" +
            "    NM AS (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM externalprovider A ORDER BY 1) AS IDS) \n" +
            "  SELECT * FROM NR, NM"

    static String META_SORT_BY_MIXED = "WITH\n" +
            "    NR AS (SELECT NULL AS minKey, NULL AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM externalprovider A ORDER BY CSKEY_0 DESC, CSKEY_1, SKEY LIMIT 10 OFFSET 10) AS IDS),\n" +
            "    NM AS (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM externalprovider A ORDER BY 1) AS IDS) \n" +
            "  SELECT * FROM NR, NM"

    static String META_FILTER = "WITH\n" +
            "    NR AS (SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.id AS SKEY FROM externalprovider A WHERE A.id IN (SELECT AA.id FROM externalprovider AA WHERE AA.type = 1) ORDER BY SKEY LIMIT 10 OFFSET 10) AS IDS),\n" +
            "    NM AS (SELECT count(*) AS numberMatched FROM (SELECT A.id AS SKEY FROM externalprovider A WHERE A.id IN (SELECT AA.id FROM externalprovider AA WHERE AA.type = 1) ORDER BY 1) AS IDS) \n" +
            "  SELECT * FROM NR, NM"

    static List<String> VALUE_ARRAY = [
            "SELECT A.id AS SKEY, A.id FROM externalprovider A ORDER BY 1",
            "SELECT A.id AS SKEY, B.id AS SKEY_1, B.externalprovidername FROM externalprovider A JOIN externalprovider_externalprovidername B ON (A.id=B.externalprovider_fk) ORDER BY 1,2"
    ]

    static List<String> OBJECT_ARRAY = [
            "SELECT A.id AS SKEY, A.id FROM explorationsite A ORDER BY 1",
            "SELECT A.id AS SKEY, B.id AS SKEY_1, C.id AS SKEY_2, C.projectname, C.id FROM explorationsite A JOIN explorationsite_task B ON (A.id=B.explorationsite_fk) JOIN task C ON (B.task_fk=C.id) ORDER BY 1,2,3"
    ]

    static List<String> MERGE = [
            "SELECT A.id AS SKEY, A.programm FROM eignungsflaeche A ORDER BY 1",
            "SELECT A.id AS SKEY, B.id AS SKEY_1, B.id, B.kennung FROM eignungsflaeche A JOIN osirisobjekt B ON (A.id=B.id) ORDER BY 1,2"
    ]

    static List<String> SELF_JOINS = [
            "SELECT A.id AS SKEY, A.id FROM building A ORDER BY 1",
            "SELECT A.id AS SKEY, B.id AS SKEY_1, B.id FROM building A JOIN building B ON (A.id=B.fk_buildingpart_parent) ORDER BY 1,2",
            "SELECT A.id AS SKEY, B.id AS SKEY_1, B.id FROM building A JOIN building B ON (A.fk_buildingpart_parent=B.id) ORDER BY 1,2"
    ]

    static List<String> SELF_JOINS_FILTER = [
            "SELECT A.id AS SKEY, A.oid FROM building A WHERE (A.id IN (SELECT AA.id FROM building AA WHERE AA.oid > 1)) ORDER BY 1",
            "SELECT A.id AS SKEY, B.id AS SKEY_1, B.id FROM building A JOIN building B ON (A.id=B.fk_buildingpart_parent AND (B.id > 100)) WHERE (A.id IN (SELECT AA.id FROM building AA WHERE AA.oid > 1)) ORDER BY 1,2",
            "SELECT A.id AS SKEY, B.id AS SKEY_1, B.id FROM building A JOIN building B ON (A.fk_buildingpart_parent=B.id AND (B.id > 1000)) WHERE (A.id IN (SELECT AA.id FROM building AA WHERE AA.oid > 1)) ORDER BY 1,2"
    ]

    static List<String> OBJECT_WITHOUT_SOURCE_PATH = [
            "SELECT A.id AS SKEY, A.id, A.legalavailability_fk, A.legalavailability_fk FROM explorationsite A ORDER BY 1"
    ]

}
