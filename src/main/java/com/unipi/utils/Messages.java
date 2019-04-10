package com.unipi.utils;

import akka.actor.ActorRef;
import com.unipi.airport.*;

public class Messages {
	
	/* ============================================================== */
	/* =================== ATTERRAGGIO ============================== */
	/* ============================================================== */
	
	/* SOURCE: Aircraft
	 * DESTINATION: Control Tower
	 * MESSAGE: Posso atterrare?
	 */
	public static final class LandingRequest {
		  public final String flightId;

		  public LandingRequest(String flightId) {
			  this.flightId = flightId;
		  }
	  }
	  
	/* SOURCE: Aircraft
	 * DESTINATION: Control Tower
	 * MESSAGE: Posso effettuare un atterraggio d'emergenza?
	 */
	public static final class EmergencyLandingRequest {
		public final String flightId;

		public EmergencyLandingRequest(String flightId) {
			  this.flightId = flightId;
		  }
	  }
	
	/* SOURCE: Aircraft
	 * DESTINATION: Control Tower
	 * MESSAGE: Parcheggio pieno, impossibile ospitare nuovi aerei.
	 */  
	public static final class LandingDenial {
		  public final String flightId;

		  public LandingDenial(String flightId) {
			  this.flightId = flightId;
		  }
	  }

	/* SOURCE: Control Tower
	 * DESTINATION: Aircraft
	 * MESSAGE: Questo � il tempo calcolato per il tuo atterraggio.
	 */  
	public static final class RespondLandingTime {
		  public final String flightId;
		  public final long timeForLanding;

		  public RespondLandingTime(String flightId, long timeForLanding) {
			  this.flightId = flightId;
			  this.timeForLanding = timeForLanding;
		  }
	  }
	  
	/* SOURCE: Control Tower
	 * DESTINATION: Aircraft
	 * MESSAGE: E' stata aggiornata la coda d'atterraggio in seguito ad una richiesta d'emergenza.
	 * 			Questo � il nuovo tempo calcolato per il tuo atterraggio.
	 */   
	public static final class UpdateLandingTime {
		  public final String flightId;
		  public final long timeForLanding;

		  public UpdateLandingTime(String flightId, long timeForLanding) {
			  this.flightId = flightId;
			  this.timeForLanding = timeForLanding;
		  }
	  }
	  
	/* SOURCE: Aircraft
	 * DESTINATION: Control Tower
	 * MESSAGE: La mia decisione sull'atterraggio è contenuta nella variabile <value>.
	 */  
	public static final class EmergencyLandingConfirmation {
		  public final boolean value;
		  public final String flightId;
			public final int confirmationType; // 1 --> prima richiesta; 2 --> risposta ad aggiornamento code

		  public EmergencyLandingConfirmation(boolean value, String flightId, int confirmationType) {
			  this.value = value;
			  this.flightId = flightId;
				this.confirmationType = confirmationType;
		  }
	  }
	  
	/* SOURCE: Aircraft
	 * DESTINATION: Control Tower
	 * MESSAGE: La mia decisione sull'atterraggio è contenuta nella variabile <value>.
	 */   
	public static final class LandingConfirmation {
		public final boolean value;
		public final String flightId;
		public final int confirmationType; // 1 --> prima richiesta; 2 --> risposta ad aggiornamento code

		public LandingConfirmation(boolean value, String flightId, int confirmationType) {
			this.value = value;
			this.flightId = flightId;
			this.confirmationType = confirmationType;
		}
	}
	
	/* SOURCE: Control Tower
	 * DESTINATION: Aircraft
	 * MESSAGE: Puoi iniziare l'atterraggio.
	 */
	public static final class StartLanding {
		public final Runway runway;
		public final String flightId;

		public StartLanding(Runway runway, String flightId) {
			this.runway = runway;
			this.flightId = flightId;
		}
	}
	
	/* SOURCE: Aircraft
	 * DESTINATION: Control Tower
	 * MESSAGE: Inizio l'atterraggio.
	 */
	public static final class Landing {
		public final Runway runway;
		public final String flightId;

		public Landing(Runway runway, String flightId) {
			this.runway = runway;
			this.flightId = flightId;
		}
	}	
	
	/* SOURCE: Aircraft
	 * DESTINATION: Aircraft
	 * MESSAGE: Self message schedulato alla fine della'atterraggio.
	 */
	public static final class InLandingState {
		public final Runway runway;
		public final String flightId;
		public final ActorRef controlTower;

		public InLandingState(Runway runway, String flightId, ActorRef controlTower) {
			this.runway = runway;
			this.flightId = flightId;
			this.controlTower = controlTower;
		}
	}
	  
	/* SOURCE: Aircraft
	 * DESTINATION: Control Tower
	 * MESSAGE: Ho terminato l'atterraggio.
	 */   
	public static final class LandingComplete {
		public final Runway runway;
		public final String flightId;
		public final boolean inEmergency;
		
		public LandingComplete(Runway runway, String flightId, boolean inEmergency) {
			this.runway = runway;
			this.flightId = flightId;
			this.inEmergency = inEmergency;
		}
	}	
	
	/* ============================================================== */
	/* ======================= DECOLLO ============================== */
	/* ============================================================== */
	
	/* SOURCE: Aircraft
	 * DESTINATION: Aircraft
	 * MESSAGE: Self message schedulato per la prossima partenza.
	 */
	public static final class StartDeparturePhase {
		public final String flightId;
		public final ActorRef controlTower;

		public StartDeparturePhase(String flightId, ActorRef controlTower) {
			this.flightId = flightId;
			this.controlTower = controlTower;
		}
	}
	  
	/* SOURCE: Aircraft
	 * DESTINATION: Control Tower
	 * MESSAGE: Posso partire?
	 */
	public static final class DepartureRequest {
		  public final String flightId;

		  public DepartureRequest(String flightId) {
			  this.flightId = flightId;
		  }
	 }

	/* SOURCE: Control Tower
	 * DESTINATION: Aircraft
	 * MESSAGE: Questo è il tempo calcolato per il tuo decollo.
	 */  
	public static final class RespondDepartureTime {
		  public final String flightId;
		  public final long timeForDeparture;

		  public RespondDepartureTime(String flightId, long timeForDeparture) {
			  this.flightId = flightId;
			  this.timeForDeparture = timeForDeparture;
		  }
	  }
	  
	/* SOURCE: Control Tower
	 * DESTINATION: Aircraft
	 * MESSAGE: E' stata aggiornata la coda d'atterraggio o di decollo.
	 * 			Questo è il nuovo tempo calcolato per il tuo decollo.
	 */   
	public static final class UpdateDepartureTime {
		  public final String flightId;
		  public final long timeForDeparture;

		  public UpdateDepartureTime(String flightId, long timeForDeparture) {
			  this.flightId = flightId;
			  this.timeForDeparture = timeForDeparture;
		  }
	  }	
	
	/* SOURCE: Control Tower
	 * DESTINATION: Aircraft
	 * MESSAGE: Puoi iniziare il decollo.
	 */
	public static final class StartTakeOff {
		public final Runway runway;
		public final String flightId;

		public StartTakeOff(Runway runway, String flightId) {
			this.runway = runway;
			this.flightId = flightId;
		}
	}
	
	/* SOURCE: Aircraft
	 * DESTINATION: Control Tower
	 * MESSAGE: Inizio il decollo.
	 */
	public static final class TakingOff {
		public final Runway runway;
		public final String flightId;

		public TakingOff(Runway runway, String flightId) {
			this.runway = runway;
			this.flightId = flightId;
		}
	}	
	
	/* SOURCE: Aircraft
	 * DESTINATION: Aircraft
	 * MESSAGE: Self message schedulato alla fine del decollo.
	 */
	public static final class InTakeOffState {
		public final Runway runway;
		public final String flightId;
		public final ActorRef controlTower;

		public InTakeOffState(Runway runway, String flightId, ActorRef controlTower) {
			this.runway = runway;
			this.flightId = flightId;
			this.controlTower = controlTower;
		}
	}
	  
	/* SOURCE: Aircraft
	 * DESTINATION: Control Tower
	 * MESSAGE: Ho terminato il decollo.
	 */   
	public static final class TakeOffComplete {
		public final Runway runway;
		public final String flightId;
		
		public TakeOffComplete(Runway runway, String flightId) {
			this.runway = runway;
			this.flightId = flightId;
		}
	}
	
	/* ============================================================== */
	/* =================== TRAFFICO DI VOLO ========================= */
	/* ============================================================== */
	
	/* SOURCE: AirportSupervisor
	 * DESTINATION: AirportSupervisor
	 * MESSAGE: Genera un nuovo aereo.
	 */
	public static final class AircraftGenerator {

		  public AircraftGenerator() {
			  
		  }
	  }
	
	/* SOURCE: AirportSupervisor
	 * DESTINATION: Aircraft
	 * MESSAGE: Puoi richiedere l'atterraggio.
	 */
	public static final class StartLandingPhase {
		public final ActorRef controlTower;
		public final String flightId;

		  public StartLandingPhase(ActorRef controlTower, String flightId) {
			  this.controlTower = controlTower;
			  this.flightId = flightId;
		  }
	  }
	
	/* ============================================================== */
	/* ========================= CARBURANTE ========================= */
	/* ============================================================== */
	
	/* SOURCE: Aircraft
	 * DESTINATION: Aircraft
	 * MESSAGE: Carburante in riserva.
	 */
	public static final class FuelReserve {
		public final String flightId;
		public final ActorRef controlTower;

		public FuelReserve(String flightId, ActorRef controlTower) {
			this.flightId = flightId;
			this.controlTower = controlTower;
		}
	}
	
}
