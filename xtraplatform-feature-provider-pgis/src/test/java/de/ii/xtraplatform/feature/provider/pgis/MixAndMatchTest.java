/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.japi.Pair;
import akka.japi.function.Function2;
import akka.stream.ActorMaterializer;
import akka.stream.DelayOverflowStrategy;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.javadsl.TestKit;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import scala.concurrent.duration.FiniteDuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import static org.testng.Assert.assertEquals;

/**
 * @author zahnen
 */
public class MixAndMatchTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderPgisTest.class);

    private static List<String> EXPECTED = ImmutableList.of("1.1", "2.1", "2.1", "2.1", "1.2", "3.2", "3.2", "1.3", "2.3", "2.3", "3.3", "3.3");
    private static List<String> EXPECTED_SORTED = ImmutableList.of("2.1", "2.1", "2.1", "1.1", "1.2", "3.2", "3.2", "2.3", "2.3", "1.3", "3.3", "3.3");

    private static List<String> EXPECTED_SORTED_MAIN_LAST = ImmutableList.of("2.1", "2.1", "2.1", "1.1", "3.2", "3.2", "1.2", "2.3", "2.3", "3.3", "3.3", "1.3");

    private static List<String> EXPECTED_TWO_KEYS = ImmutableList.of("2.1.1", "3.2.1", "2.2.1", "2.3.1", "3.2.3", "1.1", "1.2", "2.4.3", "2.5.3", "3.2.5", "3.2.5", "1.3");

    private static List<Row> EXPECTED_MERGE_SORTED = ImmutableList.of(
            new Row(0, "7", "1"),
            new Row(3, "7", "1", "1"),
            new Row(0, "7", "2"),
            new Row(0, "7", "3"),
            new Row(3, "7", "3", "2"),
            new Row(4, "7"),
            new Row(4, "8"),
            new Row(0, "9", "4"),
            new Row(0, "9", "5"),
            new Row(3, "9", "5", "3"),
            new Row(3, "9", "5", "4"),
            new Row(4, "9")
    );

    static ActorSystem system;
    static ActorMaterializer materializer;

    @BeforeClass(groups = {"default"})
    public static void setup() {
        system = ActorSystem.create();
        materializer = ActorMaterializer.create(system);
    }

    @AfterClass(groups = {"default"})
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test(groups = {"default"})
    public void test() throws InterruptedException, ExecutionException, TimeoutException {
        new TestKit(system) {
            {
                TestKit probe = new TestKit(system);
                ActorMaterializer materializer = ActorMaterializer.create(probe.getSystem());

                Source<String, NotUsed> cities = Source.from(Arrays.asList("1.1", "1.2", "1.3")).delay(FiniteDuration.create(50,"ms"), DelayOverflowStrategy.backpressure());
                Source<String, NotUsed> empty = Source.from(Arrays.asList());
                Source<String, NotUsed> rivers = Source.from(Arrays.asList("2.1", "2.1", "2.1", "2.3", "2.3")).delay(FiniteDuration.create(200,"ms"), DelayOverflowStrategy.backpressure());
                Source<String, NotUsed> altnames = Source.from(Arrays.asList("3.2", "3.2", "3.3", "3.3")).delay(FiniteDuration.create(100,"ms"), DelayOverflowStrategy.backpressure());

                List<String> result = new ArrayList<>();

                Sink<String, CompletionStage<Done>> logger = Sink.foreach(row -> {result.add(row); LOGGER.debug("ROW: {}", row);});

                Predicate<Pair<String,String>> matcher = pair -> Objects.nonNull(pair.second()) && Objects.nonNull(pair.first()) && pair.second().substring(pair.second().indexOf(".")+1).equals(pair.first().substring(pair.first().indexOf(".")+1));
                Function2<String,String,String> aggregator = (aggregate, element) -> Objects.isNull(aggregate) ? element : aggregate + "|" + element;

                CompletionStage<Done> done = MixAndMatch.create(matcher, cities, empty, rivers, altnames)
                                                        .runWith(logger, materializer);


                done
                        .toCompletableFuture()
                        .get(10, TimeUnit.SECONDS);

                assertEquals(result, EXPECTED);
            }
        };
    }

    @Test(groups = {"default"})
    public void testSorted() throws InterruptedException, ExecutionException, TimeoutException {
        new TestKit(system) {
            {
                TestKit probe = new TestKit(system);
                ActorMaterializer materializer = ActorMaterializer.create(probe.getSystem());

                Source<String, NotUsed> cities = Source.from(Arrays.asList("1.1", "1.2", "1.3")).delay(FiniteDuration.create(50,"ms"), DelayOverflowStrategy.backpressure());
                Source<String, NotUsed> empty = Source.from(Arrays.asList());
                Source<String, NotUsed> rivers = Source.from(Arrays.asList("2.1", "2.1", "2.1", "2.3", "2.3")).delay(FiniteDuration.create(200,"ms"), DelayOverflowStrategy.backpressure());
                Source<String, NotUsed> altnames = Source.from(Arrays.asList("3.2", "3.2", "3.3", "3.3")).delay(FiniteDuration.create(100,"ms"), DelayOverflowStrategy.backpressure());

                List<String> result = new ArrayList<>();

                Sink<String, CompletionStage<Done>> logger = Sink.foreach(row -> {result.add(row); LOGGER.debug("ROW: {}", row);});

                Predicate<Pair<String,String>> matcher = pair -> Objects.nonNull(pair.second()) && Objects.nonNull(pair.first()) && pair.second().substring(pair.second().indexOf(".")+1).equals(pair.first().substring(pair.first().indexOf(".")+1));
                Function2<String,String,String> aggregator = (aggregate, element) -> Objects.isNull(aggregate) ? element : aggregate + "|" + element;

                Map<Integer, List<Integer>> dependencies = ImmutableMap.of(
                        2, ImmutableList.of(0,1,3)
                );

                Map<Integer, Predicate<Pair<String,String>>> matchers = ImmutableMap.of(
                        0, matcher,
                        1, matcher,
                        2, matcher,
                        3, matcher
                );

                CompletionStage<Done> done = MixAndMatch.create(dependencies, matchers, 2, rivers, empty, cities, altnames)
                                                        .runWith(logger, materializer);


                done
                        .toCompletableFuture()
                        .get(10, TimeUnit.SECONDS);

                assertEquals(result, EXPECTED_SORTED);
            }
        };
    }

    @Test(groups = {"default"})
    public void testSortedMainLast() throws InterruptedException, ExecutionException, TimeoutException {
        new TestKit(system) {
            {
                TestKit probe = new TestKit(system);
                ActorMaterializer materializer = ActorMaterializer.create(probe.getSystem());

                Source<String, NotUsed> cities = Source.from(Arrays.asList("1.1", "1.2", "1.3")).delay(FiniteDuration.create(50,"ms"), DelayOverflowStrategy.backpressure());
                Source<String, NotUsed> empty = Source.from(Arrays.asList());
                Source<String, NotUsed> rivers = Source.from(Arrays.asList("2.1", "2.1", "2.1", "2.3", "2.3")).delay(FiniteDuration.create(200,"ms"), DelayOverflowStrategy.backpressure());
                Source<String, NotUsed> altnames = Source.from(Arrays.asList("3.2", "3.2", "3.3", "3.3")).delay(FiniteDuration.create(100,"ms"), DelayOverflowStrategy.backpressure());

                List<String> result = new ArrayList<>();

                Sink<String, CompletionStage<Done>> logger = Sink.foreach(row -> {result.add(row); LOGGER.debug("ROW: {}", row);});

                Predicate<Pair<String,String>> matcher = pair -> Objects.nonNull(pair.second()) && Objects.nonNull(pair.first()) && pair.second().substring(pair.second().indexOf(".")+1).equals(pair.first().substring(pair.first().indexOf(".")+1));
                Function2<String,String,String> aggregator = (aggregate, element) -> Objects.isNull(aggregate) ? element : aggregate + "|" + element;

                Map<Integer, List<Integer>> dependencies = ImmutableMap.of(
                        3, ImmutableList.of(0,1,2)
                );

                Map<Integer, Predicate<Pair<String,String>>> matchers = ImmutableMap.of(
                        0, matcher,
                        1, matcher,
                        2, matcher,
                        3, matcher
                );

                CompletionStage<Done> done = MixAndMatch.create(dependencies, matchers, 3, rivers, empty, altnames, cities)
                                                        .runWith(logger, materializer);


                done
                        .toCompletableFuture()
                        .get(10, TimeUnit.SECONDS);

                assertEquals(result, EXPECTED_SORTED_MAIN_LAST);
            }
        };
    }

    @Test(groups = {"default"})
    public void testTwoKeys() throws InterruptedException, ExecutionException, TimeoutException {
        new TestKit(system) {
            {
                TestKit probe = new TestKit(system);
                ActorMaterializer materializer = ActorMaterializer.create(probe.getSystem());

                Source<String, NotUsed> ortsangaben = Source.from(Arrays.asList("2.1.1", "2.2.1", "2.3.1", "2.4.3", "2.5.3")).delay(FiniteDuration.create(200,"ms"), DelayOverflowStrategy.backpressure());
                Source<String, NotUsed> empty = Source.from(Arrays.asList());
                Source<String, NotUsed> fundorttiere = Source.from(Arrays.asList("1.1", "1.2", "1.3")).delay(FiniteDuration.create(50,"ms"), DelayOverflowStrategy.backpressure());
                Source<String, NotUsed> ortsangaben_flurstueckskennzeichen = Source.from(Arrays.asList("3.2.1", "3.2.3", "3.2.5", "3.2.5")).delay(FiniteDuration.create(100,"ms"), DelayOverflowStrategy.backpressure());

                //private static List<String> EXPECTED_TWO_KEYS = ImmutableList.of("2.1.1", "3.2.1", "2.2.1", "2.3.1", "3.2.3", "1.1", "1.2", "2.4.3", "2.5.3", "3.2.5", "3.2.5", "1.3");
                List<String> result = new ArrayList<>();

                Map<Integer, List<Integer>> dependencies = ImmutableMap.of(
                        2, ImmutableList.of(0,1),
                        0, ImmutableList.of(3)
                );

                Predicate<Pair<String,String>> matcher = pair -> {
                    if (Objects.nonNull(pair.second()) &&
                            Objects.nonNull(pair.first())) {
                        String s = pair.second()
                            .substring(pair.second()
                                           .lastIndexOf(".") + 1);

                            String s2 = pair.first()
                                        .substring(pair.first()
                                                       .indexOf(".") + 1);

                        return s.equals(s2);
                    }
                    return false;
                };

                Predicate<Pair<String,String>> matcher2 = pair -> {
                 if (Objects.nonNull(pair.second()) &&
                            Objects.nonNull(pair.first())) {
                     String s = pair.second()
                         .substring(pair.second()
                                        .indexOf(".") + 1);

                         String s2 = pair.first()
                                     .substring(0, pair.first()
                                                       .lastIndexOf("."));

                         return s.equals(s2);
                 }
                 return false;
                };

                Map<Integer, Predicate<Pair<String,String>>> matchers = ImmutableMap.of(
                        0, matcher2,
                        1, matcher,
                        2, matcher,
                        3, matcher
                );

                Sink<String, CompletionStage<Done>> logger = Sink.foreach(row -> {result.add(row); LOGGER.debug("ROW: {}", row);});

                CompletionStage<Done> done = MixAndMatch.create(dependencies, matchers, 2, ortsangaben, empty, fundorttiere, ortsangaben_flurstueckskennzeichen)
                                                        .runWith(logger, materializer);


                done
                        .toCompletableFuture().get();
                        //.get(10, TimeUnit.SECONDS);

                assertEquals(result, EXPECTED_TWO_KEYS);
            }
        };
    }

    @Test(groups = {"default"})
    public void testSortedMainLastGroupBy() throws InterruptedException, ExecutionException, TimeoutException {
        new TestKit(system) {
            {
                TestKit probe = new TestKit(system);
                ActorMaterializer materializer = ActorMaterializer.create(probe.getSystem());

                Source<Row, NotUsed> ortsangaben = Source.from(ImmutableList.of(new Row(0, "7", "1"), new Row(0, "7", "2"), new Row(0, "7", "3"), new Row(0, "9", "4"), new Row(0, "9", "5"))).delay(FiniteDuration.create(200,"ms"), DelayOverflowStrategy.backpressure());
                Source<Row, NotUsed> empty = Source.from(Arrays.asList());
                Source<Row, NotUsed> fundorttiere = Source.from(ImmutableList.of(new Row(4, "7"), new Row(4, "8"), new Row(4, "9"))).delay(FiniteDuration.create(50,"ms"), DelayOverflowStrategy.backpressure());
                Source<Row, NotUsed> ortsangaben_flurstueckskennzeichen = Source.from(ImmutableList.of(new Row(3, "7", "1", "1"), new Row(3, "7", "3", "2"), new Row(3, "9", "5", "3"), new Row(3, "9", "5", "4"))).delay(FiniteDuration.create(100,"ms"), DelayOverflowStrategy.backpressure());

                List<Row> result = new ArrayList<>();

                Sink<Row, CompletionStage<Done>> logger = Sink.foreach(row -> {result.add(row); LOGGER.debug("ROW: {}", row);});

                Source<Row, NotUsed> stream = mergeAndSort(ortsangaben, empty, ImmutableList.of(fundorttiere, ortsangaben_flurstueckskennzeichen));


                CompletionStage<Done> done = stream
                      .runWith(logger, materializer);

                done
                        .toCompletableFuture()
                        .get(10, TimeUnit.SECONDS);

                assertEquals(result, EXPECTED_MERGE_SORTED);
            }
        };
    }

    static class Row implements Comparable<Row> {
        final List<String> ids;
        final int priority;

        Row(int priority, String... ids) {
            this.ids = ImmutableList.copyOf(ids);
            this.priority = priority;
        }

        @Override
        public int compareTo(Row row) {
            int size = Math.min(ids.size(), row.ids.size());
            int result = compareIdLists(ids.subList(0, size), row.ids.subList(0, size));


            return result == 0 ? priority - row.priority : result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Row row = (Row) o;
            return priority == row.priority &&
                    Objects.equals(ids, row.ids);
        }

        @Override
        public int hashCode() {

            return Objects.hash(ids, priority);
        }

        @Override
        public String toString() {
            return "ids=" + ids +
                    ", priority=" + priority;
        }
    }

    static int compareIdLists( List<String> ids1, List<String> ids2) {
        for (int i = 0; i < ids1.size(); i++) {
            int result = ids1.get(i).compareTo(ids2.get(i));
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    static <T extends Comparable<T>> Source<T, NotUsed> mergeAndSort(Source<T, NotUsed> source1, Source<T, NotUsed> source2, Iterable<Source<T, NotUsed>> rest) {
        Comparator<T> comparator = Comparator.naturalOrder();
        Source<T, NotUsed> mergedAndSorted = source1.mergeSorted(source2, comparator);
        for (Source<T, NotUsed> source3: rest) {
            mergedAndSorted = mergedAndSorted.mergeSorted(source3, comparator);
        }
        return mergedAndSorted;
    }

}