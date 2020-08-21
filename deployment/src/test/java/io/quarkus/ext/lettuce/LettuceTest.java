package io.quarkus.ext.lettuce;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.BinaryDataStrategy;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.quarkus.ext.lettuce.runtime.LettuceRedisClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;

/**
 * VM arguments add "-Djava.util.logging.manager=org.jboss.logmanager.LogManager"
 * 
 * @author Leo Tu
 */
//@Disabled
public class LettuceTest {
    private static final Logger LOGGER = Logger.getLogger(LettuceTest.class);

    static private Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
            .withFormatting(true)
            .withBinaryDataStrategy(BinaryDataStrategy.BASE_64));

    // @Order(10)
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application.properties", "application.properties")
                    .addClasses(TestBean.class, Demo.class,
                            DemoEvent.class, DemoEventImpl.class));

    @Inject
    TestBean testBean;

    @Test
    public void testEntry() {
        LOGGER.info("BEGIN...");
        if (testBean == null) {
            LOGGER.error("Inject testBean is null !");
            return;
        }
        try {
            testBean.testConnection();

            testBean.testAction();
            testBean.testAction1();
        } catch (Exception e) {
            LOGGER.error("", e);
        } finally {
            LOGGER.info("END.");
        }
    }

    // @ApplicationScoped
    @Singleton
    static class TestBean {

        @Inject
        LettuceRedisClient client; // default

        @Inject
        @Named("c1")
        LettuceRedisClient client1;

        @Inject
        Event<DemoEvent> demoEvent;

        private Service service;
        private Service service1;

        @PostConstruct
        void onPostConstruct() {
            LOGGER.debug("onPostConstruct");
        }

        @PreDestroy
        void onPreDestroy() {
            LOGGER.debug("onPreDestroy");
            client.shutdown();
            client1.shutdown();
        }

        /**
         * Called when the runtime has started
         *
         * @param event
         */
        void onStart(@Observes StartupEvent event) {
            LOGGER.debug("onStart, event=" + event);
            service = new Service(client, "client-default", demoEvent);
            service1 = new Service(client1, "client-c1", demoEvent);
        }

        void onStop(@Observes ShutdownEvent event) {
            LOGGER.debug("onStop, event=" + event);
        }

        void onDemoEvent(@Observes DemoEvent event) {
            LOGGER.debugf("onDemoEvent, event.data: %s", jsonb.toJson(event.getData()));
        }

        void testConnection() throws Exception {
            LOGGER.info("testConnection...");
            // (default)
            try (StatefulRedisConnection<String, String> conn = client.connect()) {
                LOGGER.debugv("client, isOpen: {0}, isMulti: {1}", conn.isOpen(), conn.isMulti());
            } catch (Exception e) {
                LOGGER.error("client.getConnection", e);
                throw e;
            }

            //             (1)
            try (StatefulRedisConnection<String, String> conn = client1.connect()) {
                LOGGER.debugv("client1, isOpen: {0}, isMulti: {1}", conn.isOpen(), conn.isMulti());
            } catch (Exception e) {
                LOGGER.error("client1.getConnection", e);
                throw e;
            }
        }

        void testAction() throws Exception {
            service.testBasic();
            service.testAsync();
            service.testCodec();
        }

        //
        void testAction1() throws Exception {
            service1.testBasic();
            service1.testAsync();
            service1.testCodec();
        }
    }

    static class Service {
        private LettuceRedisClient client;
        private String prefix;
        final private Event<DemoEvent> demoEvent;

        public Service(LettuceRedisClient client, String prefix, Event<DemoEvent> demoEvent) {
            this.client = client;
            this.prefix = prefix;
            this.demoEvent = demoEvent;
        }

        void testBasic() {
            try (StatefulRedisConnection<String, String> connection = client.connect()) {
                RedisCommands<String, String> sync = connection.sync();

                String key = prefix + ":basic-key";
                String reply = sync.set(key, "basic-value");
                Assertions.assertEquals("OK", reply);

                String value = sync.get(key);

                Assertions.assertEquals("basic-value", value);
                LOGGER.debugv("Successfully testBasic prefix: {0}, value: {1}", prefix, value);

                long removed = sync.del(key);
                Assertions.assertEquals(removed, 1);

                value = sync.get(key);
                Assertions.assertNull(value);
            }
        }

        void testAsync() throws Exception {
            try (StatefulRedisConnection<String, String> connection = client.connect()) {
                RedisAsyncCommands<String, String> async = connection.async();

                String key = prefix + ":async-key";
                RedisFuture<String> reply = async.set(key, "async-value");
                Assertions.assertEquals("OK", reply.get());

                CountDownLatch latch = new CountDownLatch(1);
                async.get(key).thenAccept(value -> {
                    // LOGGER.debugf("1) value: %s", value);
                    Assertions.assertEquals("async-value", value);
                }).thenCompose(vd -> {
                    // LOGGER.debugf("2) vd: %s", vd);
                    RedisFuture<Long> removed = async.del(key);
                    return removed;
                }).thenAccept(removed -> {
                    // LOGGER.debugf("3) removed: %d", removed);
                    Assertions.assertEquals(removed, 1);
                    LOGGER.debugv("Successfully testAsync prefix: {0}", prefix);
                    latch.countDown();
                }).exceptionally(err -> {
                    LOGGER.debugv("Error testAsync prefix: {0}, exception: {1}", prefix, err.toString());
                    latch.countDown();
                    return null;
                });

                latch.await();
                // LOGGER.debugv("End testAsync prefix: {0}", prefix);
            }
        }

        void testCodec() {
            try (StatefulRedisConnection<String, Demo> connection = client.connect(new DemoRedisCodec())) {
                RedisCommands<String, Demo> sync = connection.sync();

                String id = UUID.randomUUID().toString().replaceAll("-", "");
                Demo data = new Demo();
                data.setId(id);
                data.setAmount(new BigDecimal("12.15"));
                data.setName(prefix + "-demo");
                data.setCreatedAt(new Date());

                String key = prefix + ":demo-key";

                String reply = sync.set(key, data);
                Assertions.assertEquals("OK", reply);

                Demo value = sync.get(key);
                Assertions.assertEquals(data, value);

                long removed = sync.del(key);
                Assertions.assertEquals(removed, 1);

                value = sync.get(key);
                Assertions.assertNull(value);

                LOGGER.debugv("Successfully testCodec prefix: {0}, value: {1}", prefix, value);

                demoEvent.fire(new DemoEventImpl(data));
            }
        }

        /**
         * @see io.lettuce.core.codec.StringCodec
         */
        static public class DemoRedisCodec implements RedisCodec<String, Demo> {
            final private StringCodec strCodec = new StringCodec(StandardCharsets.UTF_8);

            @Override
            public String decodeKey(ByteBuffer bytes) {
                return strCodec.decodeKey(bytes);
            }

            @Override
            public Demo decodeValue(ByteBuffer bytes) {
                String json = strCodec.decodeValue(bytes);
                // LOGGER.debugv("decodeValue json: {0}", json);
                return jsonb.fromJson(json, Demo.class);
            }

            @Override
            public ByteBuffer encodeKey(String key) {
                return strCodec.encodeKey(key);
            }

            @Override
            public ByteBuffer encodeValue(Demo value) {
                String json = jsonb.toJson(value);
                // LOGGER.debugv("encodeValue json: {0}", json);
                return strCodec.encodeValue(json);
            }
        }

    }

    static public interface DemoEvent {

        public Demo getData();
    }

    static public class DemoEventImpl implements DemoEvent {
        private final Demo data;

        public DemoEventImpl(Demo data) {
            this.data = data;
        }

        @Override
        public Demo getData() {
            return data;
        }
    }
}
