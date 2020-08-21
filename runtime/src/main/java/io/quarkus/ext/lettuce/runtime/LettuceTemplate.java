package io.quarkus.ext.lettuce.runtime;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Lettuce Template class (runtime)
 * 
 * @author Leo Tu
 */
@Recorder
public class LettuceTemplate {
    private static final Logger log = Logger.getLogger(LettuceTemplate.class);

    /**
     * Build Time
     */
    public BeanContainerListener addContainerCreatedListener(
            Class<? extends AbstractLettuceProducer> lettuceProducerClassName, LettuceConfig lettuceConfig) {
        return new BeanContainerListener() {

            /**
             * Runtime Time
             */
            @Override
            public void created(BeanContainer beanContainer) { // Arc.container()
                AbstractLettuceProducer lettuceProducer = beanContainer.instance(lettuceProducerClassName);
                if (lettuceProducer == null) {
                    log.warn("(lettuceProducer == null)");
                } else {
                    log.debugv("lettuceProducer.class: {0}", lettuceProducer.getClass().getName());
                    lettuceProducer.setBuildTimeLettuceConfig(lettuceConfig);
                }
            }
        };
    }

}
