package io.quarkus.ext.lettuce;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.BinaryDataStrategy;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisStringAsyncCommands;
import io.lettuce.core.api.sync.RedisStringCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.Utf8StringCodec;
import io.quarkus.ext.lettuce.runtime.LettuceRedisClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;

/**
 * 
 * @author <a href="mailto:leo.tu.taipei@gmail.com">Leo Tu</a>
 */
//@Disabled
public class LettuceTest {
    private static final Logger LOGGER = Logger.getLogger(LettuceTest.class);

    @Order(1)
    @BeforeAll
    static public void beforeAll() {
        LOGGER.debug("...");
    }

    @AfterAll
    static public void afterAll() {
        LOGGER.debug("...");
    }

    @Order(10)
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest() //
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class) //
                    .addAsResource("application.properties", "application.properties") //
                    .addClasses(TestBean.class));

    @Inject
    TestBean testBean;

    // @BeforeEach
    // public void setUp() throws Exception {
    // LOGGER.debug("setUp...");
    // }
    //
    // @AfterEach
    // public void tearDown() throws Exception {
    // LOGGER.debug("tearDown...");
    // }

    @Test
    public void test1() {
        LOGGER.info("BEGIN test1...");
        try {
            testBean.testConnection();

            testBean.testAction();
            testBean.testAction1();
        } catch (Exception e) {
            LOGGER.error("", e);
        } finally {
            LOGGER.info("END test1.");
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

    @ApplicationScoped
    static class TestBean {

        @Inject
        LettuceRedisClient client; // default

        @Inject
        @Named("c1")
        LettuceRedisClient client1;

        @Inject
        Event<DemoEvent> demoEvent;

        private ServiceAction action;
        private ServiceAction action1;

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
            action = new ServiceAction(client, "client-default", demoEvent);
            action1 = new ServiceAction(client1, "client1-1", demoEvent);
        }

        void onStop(@Observes ShutdownEvent event) {
            LOGGER.debug("onStop, event=" + event);
        }

        void onDemoEvent(@Observes DemoEvent event) {
            LOGGER.debug("onDemoEvent, event.data=" + event.getData());
        }

        void testConnection() throws Exception {
            // (default)
            try (StatefulRedisConnection<String, String> conn = client.connect()) {
                LOGGER.debugv("client, isOpen: {0}, isMulti: {1}", conn.isOpen(), conn.isMulti());
            } catch (Exception e) {
                LOGGER.error("client.getConnection", e);
                throw e;
            }

            // (1)
            try (StatefulRedisConnection<String, String> conn = client1.connect()) {
                LOGGER.debugv("client1, isOpen: {0}, isMulti: {1}", conn.isOpen(), conn.isMulti());
            } catch (Exception e) {
                LOGGER.error("client1.getConnection", e);
                throw e;
            }
        }

        void testAction() throws Exception {
            action.testBasic();
            action.testAsync();
            action.testCodec();
        }

        void testAction1() throws Exception {
            action1.testBasic();
            action1.testAsync();
            action1.testCodec();
        }
    }

    static class ServiceAction {

        final private LettuceRedisClient client;
        final private String prefix;
        final private Event<DemoEvent> demoEvent;

        public ServiceAction(LettuceRedisClient client, String prefix, Event<DemoEvent> demoEvent) {
            this.client = client;
            this.prefix = prefix;
            this.demoEvent = demoEvent;
        }

        void testBasic() throws Exception {
            try (StatefulRedisConnection<String, String> connection = client.connect()) {
                RedisStringCommands<String, String> sync = connection.sync();

                String reply = sync.set(prefix + ":basic-key", "basic-value");
                Assertions.assertEquals("OK", reply);

                String value = sync.get(prefix + ":basic-key");

                Assertions.assertEquals("basic-value", value);
                LOGGER.debugv("Successfully testBasic prefix: {0}, value: {1}", prefix, value);
            }
        }

        void testAsync() throws Exception {
            try (StatefulRedisConnection<String, String> connection = client.connect()) {
                RedisStringAsyncCommands<String, String> async = connection.async();

                RedisFuture<String> reply = async.set(prefix + ":async-key", "async-value");
                Assertions.assertEquals("OK", reply.get());

                RedisFuture<String> get = async.get(prefix + ":async-key");
                Assertions.assertEquals("async-value", get.get());

                LOGGER.debugv("Successfully testAsync prefix: {0}, value: {1}", prefix, get.get());
            }
        }

        void testCodec() throws Exception {
            try (StatefulRedisConnection<String, Demo> connection = client.connect(new DemoRedisCodec())) {
                RedisStringCommands<String, Demo> sync = connection.sync();

                String id = UUID.randomUUID().toString().replaceAll("-", "");
                Demo data = new Demo();
                data.setId(id);
                data.setAmount(new BigDecimal("12.15"));
                data.setName(prefix + "-demo");
                data.setCreatedAt(new Date());

                String reply = sync.set(prefix + ":demo-key", data);
                Assertions.assertEquals("OK", reply);

                Demo value = sync.get(prefix + ":demo-key");

                Assertions.assertEquals(data, value);

                LOGGER.debugv("Successfully testCodec prefix: {0}, value: {1}", prefix, value);

                demoEvent.fire(new DemoEventImpl(data));
            }
        }

        /**
         * @see io.lettuce.core.codec.StringCodec
         */
        static public class DemoRedisCodec implements RedisCodec<String, Demo> {
            final private Utf8StringCodec strCodec = new Utf8StringCodec();
            final private Jsonb jsonb;

            public DemoRedisCodec() {
                JsonbConfig config = new JsonbConfig().withFormatting(true);
                config.withBinaryDataStrategy(BinaryDataStrategy.BASE_64);
                this.jsonb = JsonbBuilder.create(config);
            }

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
}