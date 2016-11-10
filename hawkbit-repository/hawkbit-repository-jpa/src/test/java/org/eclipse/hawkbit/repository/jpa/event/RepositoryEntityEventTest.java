/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.event;

import static org.fest.assertions.Assertions.assertThat;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.hawkbit.repository.event.TenantAwareEvent;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.event.RepositoryEntityEventTest.RepositoryTestConfiguration;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.fest.assertions.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("Entity Events")
@SpringApplicationConfiguration(classes = RepositoryTestConfiguration.class)
public class RepositoryEntityEventTest extends AbstractJpaIntegrationTest {

    @Autowired
    private MyEventListener eventListener;

    @Before
    public void beforeTest() {
        eventListener.queue.clear();
    }

    @Test
    @Description("Verifies that the target created event is published when a target has been created")
    public void targetCreatedEventIsPublished() throws InterruptedException {
        final Target createdTarget = targetManagement.createTarget(entityFactory.generateTarget("12345"));

        final TargetCreatedEvent targetCreatedEvent = eventListener.waitForEvent(TargetCreatedEvent.class, 1,
                TimeUnit.SECONDS);
        assertThat(targetCreatedEvent).isNotNull();
        assertThat(targetCreatedEvent.getEntity().getId()).isEqualTo(createdTarget.getId());
    }

    @Test
    @Description("Verifies that the target update event is published when a target has been updated")
    public void targetUpdateEventIsPublished() throws InterruptedException {
        final Target createdTarget = targetManagement.createTarget(entityFactory.generateTarget("12345"));
        createdTarget.setName("updateName");
        targetManagement.updateTarget(createdTarget);

        final TargetUpdatedEvent targetUpdatedEvent = eventListener.waitForEvent(TargetUpdatedEvent.class, 1,
                TimeUnit.SECONDS);
        assertThat(targetUpdatedEvent).isNotNull();
        assertThat(targetUpdatedEvent.getEntity().getId()).isEqualTo(createdTarget.getId());
    }

    @Test
    @Description("Verifies that the target deleted event is published when a target has been deleted")
    public void targetDeletedEventIsPublished() throws InterruptedException {
        final Target createdTarget = targetManagement.createTarget(entityFactory.generateTarget("12345"));

        targetManagement.deleteTargets(createdTarget.getId());

        final TargetDeletedEvent targetDeletedEvent = eventListener.waitForEvent(TargetDeletedEvent.class, 1,
                TimeUnit.SECONDS);
        assertThat(targetDeletedEvent).isNotNull();
        assertThat(targetDeletedEvent.getEntityId()).isEqualTo(createdTarget.getId());
    }

    @Test
    @Description("Verifies that the distribution set created event is published when a distribution set has been created")
    public void distributionSetCreatedEventIsPublished() throws InterruptedException {
        final DistributionSet generateDistributionSet = entityFactory.generateDistributionSet();
        generateDistributionSet.setName("dsEventTest");
        generateDistributionSet.setVersion("1");
        final DistributionSet createDistributionSet = distributionSetManagement
                .createDistributionSet(generateDistributionSet);

        final DistributionSetCreatedEvent dsCreatedEvent = eventListener.waitForEvent(DistributionSetCreatedEvent.class,
                1, TimeUnit.SECONDS);
        assertThat(dsCreatedEvent).isNotNull();
        assertThat(dsCreatedEvent.getEntity().getId()).isEqualTo(createDistributionSet.getId());
    }

    @Test
    @Description("Verifies that the distribution set deleted event is published when a distribution set has been deleted")
    public void distributionSetDeletedEventIsPublished() throws InterruptedException {

        final DistributionSet generateDistributionSet = entityFactory.generateDistributionSet();
        generateDistributionSet.setName("dsEventTest");
        generateDistributionSet.setVersion("1");
        final DistributionSet createDistributionSet = distributionSetManagement
                .createDistributionSet(generateDistributionSet);

        distributionSetManagement.deleteDistributionSet(createDistributionSet);

        final DistributionSetDeletedEvent dsDeletedEvent = eventListener.waitForEvent(DistributionSetDeletedEvent.class,
                1, TimeUnit.SECONDS);
        assertThat(dsDeletedEvent).isNotNull();
        assertThat(dsDeletedEvent.getEntityId()).isEqualTo(createDistributionSet.getId());
    }

    public static class RepositoryTestConfiguration {

        @Bean
        public MyEventListener myEventListenerBean() {
            return new MyEventListener();
        }

    }

    private static class MyEventListener {

        private final BlockingQueue<TenantAwareEvent> queue = new LinkedBlockingQueue<>();

        @EventListener(classes = TenantAwareEvent.class)
        public void onEvent(final TenantAwareEvent event) {
            queue.offer(event);
        }

        public <T> T waitForEvent(final Class<T> eventType, final long timeout, final TimeUnit timeUnit)
                throws InterruptedException {
            TenantAwareEvent event = null;
            while ((event = queue.poll(timeout, timeUnit)) != null) {
                if (event.getClass().isAssignableFrom(eventType)) {
                    return (T) event;
                }
            }
            Assertions.fail("Missing event " + eventType + " within timeout " + timeout + " " + timeUnit);
            return null;
        }
    }

}