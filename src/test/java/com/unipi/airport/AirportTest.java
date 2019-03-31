package com.unipi.airport;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;

import static org.junit.Assert.assertEquals;
import java.util.Optional;

public class AirportTest {
    static ActorSystem system;
    
    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }
    /*
    @Test
    public void testReplyWithEmptyReadingIfNoTemperatureIsKnown() {
      TestKit probe = new TestKit(system);
      ActorRef deviceActor = system.actorOf(Aircraft.props("device"));
      deviceActor.tell(new Aircraft.ObtainTime(42L), probe.getRef());
      Aircraft.RespondTime response = probe.expectMsgClass(Aircraft.RespondTime.class);
      assertEquals(42L, response.requestId);
      assertEquals(Optional.empty(), response.value);
    }
    */

/*
    @Test
    public void testAirport() {
      TestKit probe = new TestKit(system);
      ActorRef aircraft1 = system.actorOf(Aircraft.props("FR0001"));
      ActorRef controlTower = system.actorOf(ControlTower.props("CTA"));

      aircraft1.tell(new Aircraft.StartLanding(controlTower), controlTower);
      Aircraft.LandingRequest response = probe.expectMsgClass(Aircraft.LandingRequest.class);
      assertEquals(1L, response.requestId);
      assertEquals("FR0001", response.flightId);
    }*/
}