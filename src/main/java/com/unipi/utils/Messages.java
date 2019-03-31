package com.unipi.utils;

import akka.actor.ActorRef;

public class Messages {
	
	public static final class StartLanding {
		  final ActorRef controlTower;
		  final long fuel;

		  public StartLanding(ActorRef controlTower, long fuel) {
			  this.controlTower = controlTower;
			  this.fuel = fuel;
		  }
		  
		  public ActorRef getControlTower() {
			  return controlTower;
		  }
		  
		  public long getFuel() {
			  return fuel;
		  }
	  }
	  
	  public static final class LandingRequest {
		  final String requestId;
		  final String flightId;

		  public LandingRequest(String requestId, String flightId) {
			  this.requestId = requestId;
			  this.flightId = flightId;
		  }
		  
		  public String getRequestId() {
			  return requestId;
		  }
		  
		  public String getFlightId() {
			  return flightId;
		  }
	  }
	  
	  public static final class EmergencyLandingRequest {
		  final String requestId;
		  final String flightId;

		  public EmergencyLandingRequest(String requestId, String flightId) {
			  this.requestId = requestId;
			  this.flightId = flightId;
		  }
		  
		  public String getRequestId() {
			  return requestId;
		  }
		  
		  public String getFlightId() {
			  return flightId;
		  }
	  }

	  public static final class RespondTime {
		  final String requestId;
		  final String flightId;
		  final long timeForLanding;

		  public RespondTime(String requestId, String flightId, long timeForLanding) {
			  this.requestId = requestId;
			  this.flightId = flightId;
			  this.timeForLanding = timeForLanding;
		  }
		  
		  public String getRequestId() {
			  return requestId;
		  }
		  
		  public String getFlightId() {
			  return flightId;
		  }
		  
		  public long getTimeForLanding() {
			  return timeForLanding;
		  }
	  }
	  
	  public static final class LandingConfirmation {
		  final String requestId;
		  final boolean value;
		  final String flightId;

		  public LandingConfirmation(String requestId, boolean value, String flightId) {
			  this.requestId = requestId;
			  this.value = value;
			  this.flightId = flightId;
		  }
		  
		  public String getRequestId() {
			  return requestId;
		  }
		  
		  public String getFlightId() {
			  return flightId;
		  }
		  
		  public boolean getValue() {
			  return value;
		  }
	  }
}
