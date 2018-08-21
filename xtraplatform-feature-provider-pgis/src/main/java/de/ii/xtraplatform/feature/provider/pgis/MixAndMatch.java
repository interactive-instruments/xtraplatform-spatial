/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import akka.NotUsed;
import akka.japi.function.Function2;
import akka.japi.Pair;
import akka.stream.Attributes;
import akka.stream.Inlet;
import akka.stream.Outlet;
import akka.stream.UniformFanInShape;
import akka.stream.UniformFanInShape$;
import akka.stream.javadsl.Source;
import akka.stream.stage.AbstractInHandler;
import akka.stream.stage.AbstractOutHandler;
import akka.stream.stage.GraphStage;
import akka.stream.stage.GraphStageLogic;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author zahnen
 */
//TODO: to xtraplatform-akka utils
public class MixAndMatch {

    public static <T> Source<T, NotUsed> create(Predicate<Pair<T, T>> matcher, Source<T, NotUsed>... sources) {
        ImmutableList<Source<T, ?>> sourcesList = ImmutableList.copyOf(sources);

        return Source.combine(sourcesList.get(0), sourcesList.get(1), sourcesList.size() > 2 ? sourcesList.subList(2, sourcesList.size()) : ImmutableList.of(), numberOfSources -> new MixAndMatchStage<>(numberOfSources, matcher));
    }

    public static <T> Source<T, NotUsed> create(Map<Integer, List<Integer>> sourceNesting, Map<Integer, Predicate<Pair<T, T>>> matchers, int mainSourceIndex, Source<T, NotUsed>... sources) {
        ImmutableList<Source<T, ?>> sourcesList = ImmutableList.copyOf(sources);

        return Source.combine(sourcesList.get(0), sourcesList.get(1), sourcesList.size() > 2 ? sourcesList.subList(2, sourcesList.size()) : ImmutableList.of(), numberOfSources -> new MixAndMatchStage<>(numberOfSources, sourceNesting, matchers, mainSourceIndex));
    }

    public static <T, U> Source<U, NotUsed> create(Predicate<Pair<T, T>> matcher, Function2<U, T, U> aggregator, Source<T, NotUsed>... sources) {
        T[] lastElement = (T[]) new Object[1];

        Source<T, NotUsed> combinedSources = MixAndMatch.create(matcher, sources);

        return combinedSources
                .splitWhen(element -> {
                    boolean split = Objects.nonNull(lastElement[0]) && !matcher.test(new Pair<>(element, lastElement[0]));
                    lastElement[0] = element;
                    return split;
                })
                .fold(null, aggregator)
                .mergeSubstreams();
    }


    static class MixAndMatchStage<T> extends GraphStage<UniformFanInShape<T, T>> {

        private static final Logger LOGGER = LoggerFactory.getLogger(MixAndMatchStage.class);

        private final List<Inlet<T>> in;
        private final Outlet<T> out;
        private final UniformFanInShape<T, T> shape;

        private final int numberOfSources;
        private final Map<Integer, List<Integer>> sourceNesting;
        private final Map<Integer, Predicate<Pair<T, T>>> matchers;
        private final int mainSourceIndex;
        private final List<Integer> blueprint;
        private final Map<Integer,Integer> parents;
        private final Map<Integer,Boolean> multipleVisits;

        MixAndMatchStage(int numberOfSources, Predicate<Pair<T, T>> matcher) {
            this(numberOfSources, ImmutableMap.of(0, IntStream.range(1, numberOfSources)
                                                              .boxed()
                                                              .collect(Collectors.toList())), ImmutableMap.of(0, matcher), 0);
        }

        MixAndMatchStage(int numberOfSources, Map<Integer, List<Integer>> sourceNesting, Map<Integer, Predicate<Pair<T, T>>> matchers, int mainSourceIndex) {
            this.numberOfSources = numberOfSources;
            this.sourceNesting = sourceNesting;
            this.matchers = matchers;

            this.out = Outlet.create("SlickMultiSource.out");
            this.in = IntStream.range(0, numberOfSources)
                               .mapToObj(i -> Inlet.<T>create("SlickMultiSource.in" + i))
                               .collect(Collectors.toList());
            this.shape = UniformFanInShape$.MODULE$.apply(out, scala.collection.JavaConversions.asScalaBuffer(in));
            this.mainSourceIndex = mainSourceIndex;

            this.blueprint = nestedSourcesToStack(sourceNesting, numberOfSources, mainSourceIndex);
            this.parents = getParents(sourceNesting);
            this.multipleVisits = getVisits(blueprint);

        }




        private static Map<Integer,Boolean> getVisits(List<Integer> blueprint) {
            Map<Integer, Boolean> visits = new LinkedHashMap<>();
            blueprint.forEach(index -> {
                visits.computeIfPresent(index, (integer, aBoolean) -> true);
                visits.putIfAbsent(index, false);
            });

            return visits;
        }

        private static Map<Integer,Integer> getParents(Map<Integer, List<Integer>> nesting) {
            return nesting.entrySet().stream()
                          .flatMap(entry -> entry.getValue().stream().map(child -> new AbstractMap.SimpleImmutableEntry<>(child, entry.getKey())))
                          .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
        }


        // nesting 2 --> (0 --> 3),1
        // visit 2,0,3,1,2,3
        private static Stream<Integer> nestedSourcesToStack(Map<Integer, List<Integer>> nesting, Deque<Integer> order, int sourceIndex, List<Integer> visited) {
            Stream<Integer> next = Stream.of(sourceIndex);
            if (order.peekFirst().equals(sourceIndex)) {
                order.removeFirst();
            } else if (visited.contains(order.peekFirst()) && nesting.containsKey(order.peekFirst())) {
                next = Stream.concat(Stream.of(order.removeFirst()), next);
            }
            return Stream.concat(
                    next,
                    nesting.getOrDefault(sourceIndex, ImmutableList.of())
                           .stream()
                           .flatMap(child -> nestedSourcesToStack(nesting, order, child, ImmutableList.<Integer>builder().addAll(visited).add(sourceIndex).build())));
        }

        private static List<Integer> nestedSourcesToStack(Map<Integer, List<Integer>> nesting, int numberOfSources, int startIndex) {
            //0123
            //2
            //0 --> 123
            //3
            //1 --> 23

            Deque<Integer> order = IntStream.range(0, numberOfSources)
                                            .boxed()
                                            .collect(Collectors.toCollection(ArrayDeque::new));
            List<Integer> nested = nestedSourcesToStack(nesting, order, startIndex, ImmutableList.of()).collect(Collectors.toList());

            List<Integer> rest = new ArrayList<>();
            if (!order.isEmpty() && !order.peekFirst().equals(nested.get(nested.size()-1))) {
                while (!order.isEmpty()) {
                    rest.add(order.removeFirst());
                }
            }
            return Stream.concat(nested.stream(), rest.stream()).collect(Collectors.toList());
        }

        @Override
        public UniformFanInShape<T, T> shape() {
            return shape;
        }

        @Override
        public GraphStageLogic createLogic(Attributes inheritedAttributes) {
            return new GraphStageLogic(shape()) {

                private BlockingQueue<Inlet<T>> pendingQueue = new ArrayBlockingQueue<>(numberOfSources);
                private Inlet[] inletQueue = new Inlet[numberOfSources];
                private BlockingQueue<T> downstreamCache = new ArrayBlockingQueue<>(numberOfSources);


                private boolean hasSubList(int upstreamIndex) {
                    return sourceNesting.containsKey(upstreamIndex);
                }

                private List<Integer> getSubList(int upstreamIndex) {
                    return sourceNesting.get(upstreamIndex);
                }

                private boolean hasParent(int upstreamIndex) { return parents.getOrDefault(upstreamIndex, -1) > -1; }

                private Deque<Integer> parentListStack = new ArrayDeque<>();
                private List<Integer> visited = new ArrayList<>();
                int lastParent;
                int depth = 0;

                int currentBlueprint = 0;


                private boolean pending() {
                    //return !pendingQueue.isEmpty();
                    return Arrays.stream(inletQueue)
                                 .anyMatch(Objects::nonNull);
                }

                private Inlet<T> dequeue(int upstreamIndex) {
                    Inlet<T> inlet = inletQueue[upstreamIndex];
                    inletQueue[upstreamIndex] = null;
                    return inlet;
                }

                private int runningUpstreams = numberOfSources;

                private boolean upstreamsClosed() {
                    return runningUpstreams == 0;
                }

                private T lastMainRow;
                private T[] lastParentRows = (T[]) new Object[in.size()];
                @SuppressWarnings("unchecked")
                private T[] lastRows = (T[]) new Object[in.size()];

                private boolean cached() {
                    return Arrays.stream(lastRows)
                                 .anyMatch(Objects::nonNull);
                }


                boolean[] upstreamClosed = new boolean[numberOfSources];
                int currentUpstream = blueprint.get(currentBlueprint);// mainSourceIndex;
                int waiting = -1;


                @Override
                public void preStart() throws Exception {
                    super.preStart();
                    int ix = 0;
                    while (ix < in.size()) {
                        tryPull(in.get(ix));
                        ix += 1;
                    }
                    LOGGER.debug("SOURCES {} MAIN {}", numberOfSources, mainSourceIndex);
                    LOGGER.debug("BLUEPRINT {}", blueprint);
                    LOGGER.debug("PARENTS {}", parents);
                    LOGGER.debug("VISITS {}", multipleVisits);
                }

                @Override
                public void postStop() throws Exception {
                    super.postStop();
                    LOGGER.debug("STAGE COMPLETE {} {}", allDone(), downstreamCache.isEmpty());
                }

                private void checkCacheAndQueue(int upstreamIndex) {
                    int cacheHit = checkCache(upstreamIndex);
                    if (cacheHit == 0) {
                        if (allDone() && downstreamCache.isEmpty()) completeStage();
                        return;
                    }
                    if (cacheHit == 1) {
                        checkCacheAndQueue(nextUpstream(hasParent(upstreamIndex)));
                        return;
                    }

                    //Inlet<T> inlet = pendingQueue.poll();
                    Inlet<T> inlet = dequeue(upstreamIndex);
                    /*if (isMainRow(inlet) && pending()) {
                        pendingQueue.offer(inlet);
                        inlet = pendingQueue.poll();
                        LOGGER.debug("REQUEUE {} {}", 0, false);
                    }*/
                    if (inlet != null) {
                        LOGGER.debug("DEQUEUE {} {}", in.indexOf(inlet), isAvailable(inlet));
                    } else if (cacheHit == 2 && !upstreamClosed[upstreamIndex]) { // also not in queue, because inlet == null
                        waiting(upstreamIndex);
                        return;
                    }
                    if (inlet == null) {
                        // in is null if we reached the end of the queue
                        if (allDone() && downstreamCache.isEmpty()) completeStage();
                        else checkCacheAndQueue(nextUpstream());
                    } else if (isAvailable(inlet)) {
                        checkRow(inlet);
                        if (allDone() && downstreamCache.isEmpty()) completeStage();
                    } else {
                        // in was closed after being enqueued
                        // try next in queue
                        checkCacheAndQueue(nextUpstream());
                    }
                }

                private boolean allDone() {
                    return upstreamsClosed() && !pending() && !cached();
                }

                private void waiting(int upstreamIndex) {
                    LOGGER.debug("WAITING {}", upstreamIndex);
                    waiting = upstreamIndex;
                }

                private boolean isMainRow(Inlet<T> inlet) {
                    return inlet != null && in.indexOf(inlet) == mainSourceIndex;
                }

                private boolean isParentRow(int upstreamIndex) {
                    return sourceNesting.containsKey(upstreamIndex);
                }

                private boolean matchesLastMainRow(T row) {
                    LOGGER.debug("MATCHING {} {}", lastMainRow, row);
                    return matchers.get(mainSourceIndex)
                                   .test(new Pair<>(lastMainRow, row));
                }

                private boolean matchesLastParentRow(T row, int upstreamIndex) {
                    int parentIndex = parents.getOrDefault(upstreamIndex, -1);
                    if (parentIndex == -1) return false;
                    LOGGER.debug("MATCHING {} {}", lastParentRows[parentIndex], row);
                    return matchers.get(parentIndex)
                                   .test(new Pair<>(lastParentRows[parentIndex], row));
                }

                private T grabRow(Inlet<T> inlet) {
                    T row = grab(inlet);

                    int upstreamIndex = in.indexOf(inlet);

                    /*if (isMainRow(inlet)) {
                        lastMainRow = row;
                    }*/
                    if (isParentRow(upstreamIndex)) {
                        lastParentRows[upstreamIndex] = row;
                    }

                    return row;
                }

                private void cacheRow(T row, int upstreamIndex) {
                    lastRows[upstreamIndex] = row;
                    //LOGGER.debug("TOCACHE {} {} {}", upstreamIndex == mainSourceIndex ? "MAIN" : "", upstreamIndex, row);
                    LOGGER.debug("TOCACHE {} {} {}", hasSubList(upstreamIndex) ? "MAIN" : "", upstreamIndex, row);
                }

                private void queueUpstream(Inlet<T> inlet, int upstreamIndex) {
                    inletQueue[upstreamIndex] = inlet;
                    LOGGER.debug("TOQUEUE {}", upstreamIndex);
                }

                private boolean isUpstreamEmpty(int upstreamIndex) {
                    return upstreamClosed[upstreamIndex] && lastRows[upstreamIndex] == null && inletQueue[upstreamIndex] == null;
                }

                private void pushRow(T row, int upstreamIndex) {
                    if (isAvailable(out)) {
                        LOGGER.debug("WRITE {} {}", upstreamIndex, row);
                        push(out, row);
                    } else {
                        LOGGER.debug("WRITE TOCACHE {} {}", upstreamIndex, row);
                        downstreamCache.add(row);
                    }
                    lastRows[upstreamIndex] = null;
                }

                private boolean checkRow(Inlet<T> inlet) {
                    T row = grabRow(inlet);
                    int upstreamIndex = in.indexOf(inlet);

                    boolean hasSubListOneVisit = hasSubList(upstreamIndex) && !multipleVisits.get(upstreamIndex);// && !hasParent(upstreamIndex);
                    boolean hasParentAndMatches = hasParent(upstreamIndex) && matchesLastParentRow(row, upstreamIndex);
                    boolean hasNoParentOrHasParentAndMatches = !hasParent(upstreamIndex) || (hasParent(upstreamIndex) && matchesLastParentRow(row, upstreamIndex));

                    //if ((isMainRow(inlet) && upstreamIndex == 0) || (upstreamIndex != mainSourceIndex && matchesLastMainRow(row))) {
                    if ((hasSubListOneVisit && hasNoParentOrHasParentAndMatches) || hasParentAndMatches) {
                        pushRow(row, upstreamIndex);
                        //if ((isMainRow(inlet) && upstreamIndex == 0)) {
                        if (hasSubListOneVisit) {
                            nextUpstream();
                        }
                        waiting(currentUpstream);
                        tryPull(inlet);
                        return true;
                    } else {
                        cacheRow(row, upstreamIndex);
                        checkCacheAndQueue(nextUpstream(hasParent(upstreamIndex)));
                    }
                    return false;
                }

                private int checkCache(int upstreamIndex) {
                    //for (int upstreamIndex = 1; upstreamIndex < in.size(); upstreamIndex++) {
                    // mainRow != firstRow, mainRow was cached
                    //if (upstreamIndex != 0 && upstreamIndex == mainSourceIndex && Objects.nonNull(lastRows[upstreamIndex])) {
                    if (upstreamIndex != 0 && hasSubList(upstreamIndex) && Objects.nonNull(lastRows[upstreamIndex])) {
                        LOGGER.debug("CACHE MAIN {} {}", upstreamIndex, lastRows[upstreamIndex]);
                        pushRow(lastRows[upstreamIndex], upstreamIndex);
                        //if (!gotnextmain)
                        if (!multipleVisits.get(upstreamIndex) || isSecondVisit(upstreamIndex))
                            nextUpstream();
                        waiting(currentUpstream);
                        tryPull(in.get(upstreamIndex));
                        return 0;
                    //} else if (matchesLastMainRow(lastRows[upstreamIndex])) {
                    } else if (matchesLastParentRow(lastRows[upstreamIndex], upstreamIndex)) {
                        LOGGER.debug("CACHE {} {}", upstreamIndex, lastRows[upstreamIndex]);
                        pushRow(lastRows[upstreamIndex], upstreamIndex);

                        boolean hasSubListOneVisit = hasSubList(upstreamIndex) && !multipleVisits.get(upstreamIndex);// && !hasParent(upstreamIndex);
                        if (hasSubListOneVisit) {
                            nextUpstream();
                        }

                        tryPull(in.get(upstreamIndex));
                        return 0;
                    } else if (Objects.nonNull(lastRows[upstreamIndex])) {
                        LOGGER.debug("CACHE MISMATCH {}", upstreamIndex);
                        //checkCacheAndQueue(nextUpstream());
                        return 1;
                    }
                    //}
                    return 2;
                }

                private boolean isSecondVisit(int upstreamIndex) {
                    return blueprint.indexOf(upstreamIndex) != currentBlueprint;
                }

                int count = 0;
                boolean gotnextmain = true;

                private int nextUpstream() {
                    return nextUpstream(false);
                }
                private int nextUpstream(boolean hasParentAndMissed) {
                    int initialUpstream = currentUpstream;
                    if (allDone()) {
                        if (downstreamCache.isEmpty())
                            completeStage();
                        return 0;
                    }
                    do {
                        // order 0,1,2,3
                        // nesting 2 --> 0,2,3

                        // order 0,1,2,3
                        // nesting 2 --> (0 --> 3),1
                        // visit 2,0,3,1,2,3
                        //
                        // --> 2 --> cache --> descend (hasSub) --> (depth=1, parentList=[2], visited=[2])
                        // --> 0 --> write --> descend (hasSub) --> (depth=2, parentList=[0,1], visited=[2,0])
                        // --> 3 --> cache --> ascend (!hasSub && !hasSibling && depth > 1) --> (depth=1, parentList=[2], visited=[2,0,3])
                        // --> 1 --> write --> next (visited.length==numberOfSources) --> (, visited=[2,0,3,1])
                        // --> 2 --> write --> next (is in visited)
                        // --> 3 --> write --> back to main (is in visited && index==upstreamIndex) --> (clear all)


                        // mainSourceIndex=2, numberOfSources=4, all deps of 2
                        // --> 2 --> cache --> go to first
                        // --> 0 --> write
                        // --> 1 --> write
                        // --> 2 --> write
                        // --> 3 --> write --> back to main
                        /*if (mainSourceIndex != 0 || sourceNesting.size() > 1) {
                            if (hasSubList(currentUpstream)) {
                                parentListStack.addAll(getSubList(currentUpstream));
                                visited.add(currentUpstream);
                                LOGGER.debug("DESCEND {} --> {}", currentUpstream, parentListStack.peekFirst());
                                currentUpstream = parentListStack.removeFirst();
                            }

                            // back to main
                            if (currentUpstream == numberOfSources - 1 && !gotnextmain) {
                                currentUpstream = mainSourceIndex - 1;
                                gotnextmain = true;//parentQueue.addFirst(currentUpstream);
                                LOGGER.debug("BACK TO MAIN");
                            }
                            // back to first
                            else if (currentUpstream == mainSourceIndex && gotnextmain) {
                                currentUpstream = numberOfSources - 1;
                                gotnextmain = false;
                                LOGGER.debug("BACK TO FIRST");
                            }
                        }*/
                        if (hasParentAndMissed && initialUpstream == currentUpstream) {
                            List<Integer> currentSubList = sourceNesting.get(parents.get(currentUpstream));
                            boolean upstreamEmpty = isUpstreamEmpty(currentUpstream);
                            int parent = parents.get(currentUpstream);
                            boolean parentEmpty = isUpstreamEmpty(parent);
                            boolean isLastInSublist = currentUpstream == currentSubList.get(currentSubList.size()-1);

                            if (isLastInSublist  && (!upstreamEmpty || initialUpstream == currentUpstream) && !parentEmpty) {
                                currentBlueprint = blueprint.indexOf(parent) - 1;
                                LOGGER.debug("REWIND");
                            } else if (!isLastInSublist) {
                                int nextInSubList = currentSubList.get(currentSubList.indexOf(currentUpstream)+1);
                                currentBlueprint = blueprint.indexOf(nextInSubList) - 1;
                                LOGGER.debug("FORWARD");
                            }

                        } else

                        // end of sublist, return to parent
                        if (!hasSubList(currentUpstream)) {
                            List<Integer> currentSubList = sourceNesting.get(parents.get(currentUpstream));
                            boolean upstreamEmpty = isUpstreamEmpty(currentUpstream);
                            int parent = parents.get(currentUpstream);
                            boolean parentEmpty = isUpstreamEmpty(parent);
                            if (currentUpstream == currentSubList.get(currentSubList.size()-1)  /*&& (!upstreamEmpty || initialUpstream == currentUpstream)*/ && !parentEmpty) {
                                currentBlueprint = blueprint.indexOf(parent) - 1;
                                LOGGER.debug("REWIND");
                            }
                        }


                        currentBlueprint = (currentBlueprint + 1) % blueprint.size();
                        currentUpstream = blueprint.get(currentBlueprint);
                        //currentUpstream = (currentUpstream + 1) % numberOfSources;
                    } while (isUpstreamEmpty(currentUpstream));
                    LOGGER.debug("NEXT UPSTREAM {} blueprint[{}]", currentUpstream, currentBlueprint);
                    return currentUpstream;
                }

                {
                    int ix = 0;
                    while (ix < in.size()) {
                        Inlet<T> i = in.get(ix);
                        //boolean isMain = ix == mainSourceIndex;
                        int ixx = ix;
                        ix += 1;

                        setHandler(i, new AbstractInHandler() {
                            @Override
                            public void onPush() throws Exception {
                                LOGGER.debug("PUSH {} {}", ixx, isAvailable(out));
                                if (waiting == ixx) waiting = -1;
                                if (isAvailable(out) && ixx == currentUpstream) {
                                    // isAvailable(out) implies !pending
                                    // -> grab and push immediately
                                    checkRow(i);
                                } else queueUpstream(i, ixx); //pendingQueue.offer(i);
                            }

                            @Override
                            public void onUpstreamFinish() throws Exception {
                                runningUpstreams -= 1;
                                upstreamClosed[ixx] = true;
                                LOGGER.debug("CLOSED {}", ixx);
                                if (allDone() && !downstreamCache.isEmpty()) return;
                                else if (allDone() && downstreamCache.isEmpty()) completeStage();
                                else if (currentUpstream == ixx && waiting == ixx) checkCacheAndQueue(nextUpstream());
                            }
                        });
                    }

                    setHandler(out, new AbstractOutHandler() {
                        @Override
                        public void onPull() throws Exception {
                            LOGGER.debug("PULL {} {} {}", pending(), cached(), currentUpstream);
                            if (!downstreamCache.isEmpty()) {
                                T row = downstreamCache.poll();
                                push(out, row);
                                LOGGER.debug("WRITE FROMCACHE {} {}", row);
                            }
                            if (allDone() && downstreamCache.isEmpty()) {
                                completeStage();
                            } else if (downstreamCache.isEmpty() && (pending() || cached()))
                                checkCacheAndQueue(currentUpstream);
                        }
                    });
                }

            };
        }

    }
}
