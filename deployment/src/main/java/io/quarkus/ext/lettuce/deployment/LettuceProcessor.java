package io.quarkus.ext.lettuce.deployment;

import java.util.Map.Entry;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.ext.lettuce.runtime.AbstractLettuceProducer;
import io.quarkus.ext.lettuce.runtime.AbstractLettuceProducer.RedisClientQualifier;
import io.quarkus.ext.lettuce.runtime.LettuceConfig;
import io.quarkus.ext.lettuce.runtime.LettuceItemConfig;
import io.quarkus.ext.lettuce.runtime.LettuceRedisClient;
import io.quarkus.ext.lettuce.runtime.LettuceTemplate;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

/**
 * Deployment Processor
 * 
 * <pre>
 * https://quarkus.io/guides/cdi-reference#supported_features
 * https://github.com/quarkusio/gizmo
 * </pre>
 * 
 * @author Leo Tu
 */
public class LettuceProcessor {
    private static final Logger log = Logger.getLogger(LettuceProcessor.class);

    private static final DotName REDIS_CLIENT_QUALIFIER = DotName.createSimple(RedisClientQualifier.class.getName());

    private final String lettuceProducerClassName = AbstractLettuceProducer.class.getPackage().getName()
            + ".LettuceProducer";

    /**
     * Register a extension capability and feature
     *
     * @return Lettuce feature build item
     */
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("lettuce");
    }
    
    @BuildStep
    BeanDefiningAnnotationBuildItem registerAnnotation() {
        return new BeanDefiningAnnotationBuildItem(REDIS_CLIENT_QUALIFIER);
    }

    @SuppressWarnings("unchecked")
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    BeanContainerListenerBuildItem build(RecorderContext recorder, LettuceTemplate template,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans, LettuceConfig lettuceConfig,
            BuildProducer<GeneratedBeanBuildItem> generatedBean) {
        if (isUnconfigured(lettuceConfig)) {
            return null;
        }
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, AbstractRedisClient.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, RedisClient.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, AbstractLettuceProducer.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, LettuceRedisClient.class));

        if (!isPresentUri(lettuceConfig.defaultConfig)) {
            log.warn("No default uri been defined");
        }

        createLettuceProducerBean(generatedBean, unremovableBeans, lettuceConfig);
        return new BeanContainerListenerBuildItem(template.addContainerCreatedListener(
                (Class<? extends AbstractLettuceProducer>) recorder.classProxy(lettuceProducerClassName),
                lettuceConfig));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void configureRedisClient(LettuceTemplate template,
            BuildProducer<LettuceInitializedBuildItem> lettuceInitialized, LettuceConfig lettuceConfig) {
        if (isUnconfigured(lettuceConfig)) {
            return;
        }
        lettuceInitialized.produce(new LettuceInitializedBuildItem());
    }

    private boolean isUnconfigured(LettuceConfig lettuceConfig) {
        if (!isPresentUri(lettuceConfig.defaultConfig) && lettuceConfig.namedConfig.isEmpty()) {
            // No Lettuce has been configured so bail out
            log.info("No Lettuce has been configured");
            return true;
        } else {
            return false;
        }
    }

    private void createLettuceProducerBean(BuildProducer<GeneratedBeanBuildItem> generatedBean,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans, LettuceConfig lettuceConfig) {
        ClassOutput classOutput = new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedBean.produce(new GeneratedBeanBuildItem(name, data));
            }
        };
        unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(lettuceProducerClassName));

        ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput).className(lettuceProducerClassName)
                .superClass(AbstractLettuceProducer.class).build();
        classCreator.addAnnotation(ApplicationScoped.class);

        LettuceItemConfig defaultConfig = lettuceConfig.defaultConfig;
        if (isPresentUri(defaultConfig)) {
            MethodCreator defaultRedisClientMethodCreator = classCreator.getMethodCreator("createDefaultRedisClient",
                    LettuceRedisClient.class);

            defaultRedisClientMethodCreator.addAnnotation(Singleton.class);
            defaultRedisClientMethodCreator.addAnnotation(Produces.class);
            defaultRedisClientMethodCreator.addAnnotation(Default.class);

            ResultHandle defaultConfigRH = defaultRedisClientMethodCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(AbstractLettuceProducer.class, "getDefaultItemConfig", Optional.class),
                    defaultRedisClientMethodCreator.getThis());

            defaultRedisClientMethodCreator.returnValue(
                    defaultRedisClientMethodCreator
                            .invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(AbstractLettuceProducer.class, "create",
                                            LettuceRedisClient.class, Optional.class),
                                    defaultRedisClientMethodCreator.getThis(), defaultConfigRH));
        }

        for (Entry<String, LettuceItemConfig> configEntry : lettuceConfig.namedConfig.entrySet()) {
            String named = configEntry.getKey();
            LettuceItemConfig namedConfig = configEntry.getValue();
            if (!isPresentUri(namedConfig)) {
                log.warnv("!isPresentUri(namedConfig), named: {0}, namedConfig: {1}", named, namedConfig);
                continue;
            }

            String suffix = HashUtil.sha1(named);
            MethodCreator namedRedisClientMethodCreator = classCreator
                    .getMethodCreator("createNamedRedisClient_" + suffix, LettuceRedisClient.class);

            namedRedisClientMethodCreator.addAnnotation(ApplicationScoped.class);
            namedRedisClientMethodCreator.addAnnotation(Produces.class);
            namedRedisClientMethodCreator.addAnnotation(AnnotationInstance.create(DotNames.NAMED, null,
                    new AnnotationValue[] { AnnotationValue.createStringValue("value", named) }));
            namedRedisClientMethodCreator.addAnnotation(AnnotationInstance.create(REDIS_CLIENT_QUALIFIER, null,
                    new AnnotationValue[] { AnnotationValue.createStringValue("value", named) }));

            ResultHandle namedRH = namedRedisClientMethodCreator.load(named);
            ResultHandle namedConfigRH = namedRedisClientMethodCreator.invokeVirtualMethod(MethodDescriptor
                    .ofMethod(AbstractLettuceProducer.class, "getNamedItemConfig", Optional.class, String.class),
                    namedRedisClientMethodCreator.getThis(), namedRH);

            namedRedisClientMethodCreator
                    .returnValue(
                            namedRedisClientMethodCreator.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(AbstractLettuceProducer.class, "create",
                                            LettuceRedisClient.class, Optional.class),
                                    namedRedisClientMethodCreator.getThis(), namedConfigRH));
        }

        classCreator.close();
    }

    private boolean isPresentUri(LettuceItemConfig itemConfig) {
        return itemConfig.uri != null && !itemConfig.uri.isEmpty();
    }
}