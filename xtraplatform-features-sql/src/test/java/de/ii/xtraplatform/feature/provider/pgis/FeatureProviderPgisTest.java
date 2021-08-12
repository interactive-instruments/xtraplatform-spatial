/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

/**
 * @author zahnen
 */
public class FeatureProviderPgisTest {

    /*private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderPgisTest.class);

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
    public void testGetFeatureStream() throws InterruptedException, ExecutionException, TimeoutException {
        new TestKit(system) {
            {
                TestKit probe = new TestKit(system);
                ActorMaterializer materializer = ActorMaterializer.create(probe.getSystem());

                Config config = ConfigFactory.parseString("{\n" +
                        "  profile = \"slick.jdbc.PostgresProfile$\"\n" +
                        "  db {\n" +
                        "    user = \"postgres\"\n" +
                        "    password = \"postgres\"\n" +
                        "    url = \"jdbc:postgresql://192.168.230.76/simpledemo_hale\"\n" +
                        "  }\n" +
                        "}");

                SlickSession session = SlickSession.forConfig(config);
                system.registerOnTermination(session::close);


                ImmutableSqlFeatureQueries queries = ImmutableSqlFeatureQueries.builder()
                                                                             .addPaths("/city")
                                                                             .addPaths("/city/id")
                                                                             .addPaths("/city/name")
                                                                             .addPaths("/city/[id=cid]alternativename/name")
                                                                             .addPaths("/city/[id=cid]city_river/[rid=id]river/name")
                                                                             .build();

                SqlFeatureSource sqlFeatureSource = new SqlFeatureSource(session, queries, materializer, true, featureTypeMappings.getValue()
                                                                                                                                  .getMappings());
                LoggingFeatureConsumer loggingConsumer = new LoggingFeatureConsumer();

                CompletionStage<Done> done = sqlFeatureSource.runQuery(ImmutableFeatureQuery.builder().type("city")
                                                                                            .build(), loggingConsumer);


                done.toCompletableFuture()
                    .get(10, TimeUnit.SECONDS);

                LOGGER.debug("CONSUMER {}", loggingConsumer.log);
            }
        };
    }

    static class LoggingFeatureConsumer implements FeatureConsumer {
        public StringBuilder log = new StringBuilder();

        @Override
        public void onStart(OptionalLong numberReturned, OptionalLong numberMatched) throws Exception {
            log.append(String.format("START: %d %d\n", numberReturned.orElse(-1), numberMatched.orElse(-1)));
        }

        @Override
        public void onEnd() throws Exception {
            log.append(String.format("END\n"));
        }

        @Override
        public void onFeatureStart(List<String> path) throws Exception {
            log.append(String.format("START FEATURE: %ss\n", path));
        }

        @Override
        public void onFeatureEnd(List<String> path) throws Exception {
            log.append(String.format("END FEATURE: %s\n", path));
        }

        @Override
        public void onPropertyStart(List<String> path, List<Integer> multiplicities) throws Exception {
            log.append(String.format("START PROPERTY: %s\n", path));
        }

        @Override
        public void onPropertyText(String text) throws Exception {
            log.append(String.format("VALUE: %s\n", text));
        }

        @Override
        public void onPropertyEnd(List<String> path) throws Exception {
            log.append(String.format("END PROPERTY: %s\n", path));
        }
    }*/
}