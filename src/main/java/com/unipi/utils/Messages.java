package com.unipi.utils;

import akka.actor.ActorRef;

public class Messages {
	
	public static final class StartLanding {
		public final ActorRef controlTower;
		public final long fuel;

		  public StartLanding(ActorRef controlTower, long fuel) {
			  this.controlTower = controlTower;
			  this.fuel = fuel;
		  }
	  }
	  
	  public static final class LandingRequest {
		  public final String requestId;
		  public final String flightId;

		  public LandingRequest(String requestId, String flightId) {
			  this.requestId = requestId;
			  this.flightId = flightId;
		  }
	  }
	  
	  public static final class EmergencyLandingRequest {
		  public final String requestId;
		  public final String flightId;

		  public EmergencyLandingRequest(String requestId, String flightId) {
			  this.requestId = requestId;
			  this.flightId = flightId;
		  }
	  }

	  public static final class RespondLandingTime {
		  public final String requestId;
		  public final String flightId;
		  public final long timeForLanding;

		  public RespondLandingTime(String requestId, String flightId, long timeForLanding) {
			  this.requestId = requestId;
			  this.flightId = flightId;
			  this.timeForLanding = timeForLanding;
		  }
	  }
	  
	  public static final class UpdateLandingTime {
		  //public final String flightId;
		  public final long timeForLanding;

		  public UpdateLandingTime(long timeForLanding) {
			  //this.flightId = flightId;
			  this.timeForLanding = timeForLanding;
		  }
	  }
	  
	  public static final class EmergencyLandingConfirmation {
		  public final boolean value;
		  public final String flightId;

		  public EmergencyLandingConfirmation(boolean value, String flightId) {
			  this.value = value;
			  this.flightId = flightId;
		  }
	  }
	  
	  public static final class LandingConfirmation {
		  public final boolean value;
		  public final String flightId;

		  public LandingConfirmation(boolean value, String flightId) {
			  this.value = value;
			  this.flightId = flightId;
		  }
	  }
}
