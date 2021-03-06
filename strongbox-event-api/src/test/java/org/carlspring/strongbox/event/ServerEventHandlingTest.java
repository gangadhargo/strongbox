package org.carlspring.strongbox.event;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.carlspring.strongbox.config.EventsConfig;
import org.carlspring.strongbox.event.server.ServerEvent;
import org.carlspring.strongbox.event.server.ServerEventListener;
import org.carlspring.strongbox.event.server.ServerEventListenerRegistry;
import org.carlspring.strongbox.event.server.ServerEventTypeEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author mtodorov
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class ServerEventHandlingTest
{

    @org.springframework.context.annotation.Configuration
    @Import({ EventsConfig.class })
    public static class SpringConfig
    {
    }

    @Inject
    ServerEventListenerRegistry serverEventListenerRegistry;


    @Test
    public void testEventDispatchingAndHandling()
    {
        DummyServerEventListener listener = new DummyServerEventListener();

        serverEventListenerRegistry.addListener(listener);

        ServerEvent artifactEvent = new ServerEvent(ServerEventTypeEnum.EVENT_SERVER_STARTED.getType());

        serverEventListenerRegistry.dispatchEvent(artifactEvent);

        assertEquals("Failed to catch event!", true, listener.eventCaught);
    }

    private class DummyServerEventListener
            implements ServerEventListener
    {

        boolean eventCaught = false;

        @Override
        public void handle(ServerEvent event)
        {
            System.out.println("Caught server event type " + event.getType() + ".");

            eventCaught = true;
        }

    }

}
