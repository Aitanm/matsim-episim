/*-
 * #%L
 * MATSim Episim
 * %%
 * Copyright (C) 2020 matsim-org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.matsim.episim.model;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationUtils;
import org.matsim.episim.*;
import org.matsim.episim.policy.Restriction;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;

import java.util.Map;
import java.util.SplittableRandom;


/**
 * Base implementation for infection dynamics.
 */
public abstract class AbstractInfectionModel implements InfectionModel {

	protected final Scenario scenario = null;
	protected final SplittableRandom rnd;
	protected final EpisimConfigGroup episimConfig;
	protected final EpisimReporting reporting;
	protected int iteration;
	private Map<String, Restriction> restrictions;

	AbstractInfectionModel(SplittableRandom rnd, EpisimConfigGroup episimConfig, EpisimReporting reporting) {
		this.rnd = rnd;
		this.episimConfig = episimConfig;
		this.reporting = reporting;
	}

	private static boolean activityRelevantForInfectionDynamics(EpisimPerson person, EpisimConfigGroup episimConfig,
																Map<String, Restriction> restrictions, SplittableRandom rnd) {
		String act = person.getTrajectory().get(person.getCurrentPositionInTrajectory());

		// Check if person is home quarantined
		if (person.getQuarantineStatus() == EpisimPerson.QuarantineStatus.atHome && !act.startsWith("home"))
			return false;

		return actIsRelevant(act, episimConfig, restrictions, rnd);
	}

	private static boolean actIsRelevant(String act, EpisimConfigGroup episimConfig,
										 Map<String, Restriction> restrictions, SplittableRandom rnd) {

		EpisimConfigGroup.InfectionParams infectionParams = episimConfig.selectInfectionParams(act);
		Restriction r = restrictions.get(infectionParams.getContainerName());
		// avoid use of rnd if outcome is known beforehand
		if (r.getRemainingFraction() == 1)
			return true;
		if (r.getRemainingFraction() == 0)
			return false;

		return rnd.nextDouble() < r.getRemainingFraction();

	}

	private static boolean tripRelevantForInfectionDynamics(EpisimPerson person, EpisimConfigGroup episimConfig,
															Map<String, Restriction> restrictions, SplittableRandom rnd) {
		String lastAct = "";
		if (person.getCurrentPositionInTrajectory() != 0) {
			lastAct = person.getTrajectory().get(person.getCurrentPositionInTrajectory() - 1);
		}

		String nextAct = person.getTrajectory().get(person.getCurrentPositionInTrajectory());

		// TODO: tr is a hardcoded activity for "pt" .
		//  Aitan June 20`: One should know if the vehicle is from PT from other form. Maybe adding the list of vehicles from the config ?
		// Changing tr to pt
		// last activity is only considered if present
		return actIsRelevant("tr", episimConfig, restrictions, rnd) && actIsRelevant(nextAct, episimConfig, restrictions, rnd)
				&& (lastAct.isEmpty() || actIsRelevant(lastAct, episimConfig, restrictions, rnd));

	}

	/**
	 * Checks whether person is relevant for tracking or for infection dynamics.  Currently, "relevant for infection dynamics" is a subset of "relevant for
	 * tracking".  However, I am not sure if this will always be the case.  kai, apr'20
	 *
	 * @noinspection BooleanMethodIsAlwaysInverted
	 */
	static boolean personRelevantForTrackingOrInfectionDynamics(EpisimPerson person, EpisimContainer<?> container, EpisimConfigGroup episimConfig,
																Map<String, Restriction> restrictions, SplittableRandom rnd) {

		// Infected but not contagious persons are considered additionally
		if (!hasDiseaseStatusRelevantForInfectionDynamics(person) &&
				person.getDiseaseStatus() != EpisimPerson.DiseaseStatus.infectedButNotContagious)
			return false;

		if (person.getQuarantineStatus() == EpisimPerson.QuarantineStatus.full) {
			return false;
		}

		if (container instanceof InfectionEventHandler.EpisimFacility && activityRelevantForInfectionDynamics(person, episimConfig, restrictions, rnd)) {
			return true;
		}
		return container instanceof InfectionEventHandler.EpisimVehicle && tripRelevantForInfectionDynamics(person, episimConfig, restrictions, rnd);
	}

	private static boolean hasDiseaseStatusRelevantForInfectionDynamics(EpisimPerson personWrapper) {
		switch (personWrapper.getDiseaseStatus()) {
			case susceptible:
			case contagious:
				return true;

			case infectedButNotContagious:
			case showingSymptoms: //assume is at home
			case recovered:
			case seriouslySick: // assume is in hospital
			case critical:
				return false;

			default:
				throw new IllegalStateException("Unexpected value: " + personWrapper.getDiseaseStatus());
		}
	}

	/**
	 * This method checks whether person1 and person2 have relevant disease status for infection dynamics.
	 * If not or if both have the same disease status, the return value is false.
	 */
	static boolean personsCanInfectEachOther(EpisimPerson person1, EpisimPerson person2) {
		if (person1.getDiseaseStatus() == person2.getDiseaseStatus()) return false;
		return (hasDiseaseStatusRelevantForInfectionDynamics(person1) && hasDiseaseStatusRelevantForInfectionDynamics(person2));
	}

	/**
	 * Set the iteration number and restrictions that are in place.
	 */
	@Override
	public void setRestrictionsForIteration(int iteration, Map<String, Restriction> restrictions) {
		this.iteration = iteration;
		this.restrictions = restrictions;
	}

	/**
	 * Sets the infection status of a person and reports the event.
	 */
	void infectPerson(EpisimPerson personWrapper, EpisimPerson infector, double now, StringBuilder infectionType) {

		if (personWrapper.getDiseaseStatus() != EpisimPerson.DiseaseStatus.susceptible) {
			throw new IllegalStateException("Person to be infected is not susceptible. Status is=" + personWrapper.getDiseaseStatus());
		}
		if (infector.getDiseaseStatus() != EpisimPerson.DiseaseStatus.contagious) {
			throw new IllegalStateException("Infector is not contagious. Status is=" + infector.getDiseaseStatus());
		}
		if (personWrapper.getQuarantineStatus() == EpisimPerson.QuarantineStatus.full) {
			throw new IllegalStateException("Person to be infected is in full quarantine.");
		}
		if (infector.getQuarantineStatus() == EpisimPerson.QuarantineStatus.full) {
			throw new IllegalStateException("Infector is in ful quarantine.");
		}
		if (!personWrapper.getCurrentContainer().equals(infector.getCurrentContainer())) {
			throw new IllegalStateException("Person and infector are not in same container!");
		}

		// TODO: during iteration persons can get infected after 24h
		// this can lead to strange effects / ordering of events, because it is assumed one iteration is one day
		// now is overwritten to be at the end of day
		if (now >= EpisimUtils.getCorrectedTime(24 * 60 * 60, iteration)) {
			now = EpisimUtils.getCorrectedTime(24 * 60 * 60 - 1, iteration);
		}

		reporting.reportInfection(personWrapper, infector, now, infectionType.toString());
		personWrapper.setDiseaseStatus(now, EpisimPerson.DiseaseStatus.infectedButNotContagious);

		// TODO: Currently not in use, is it still needed?
		// Necessary for the otfvis visualization (although it is unfortunately not working).  kai, apr'20
		if (scenario != null) {
			final Person person = PopulationUtils.findPerson(personWrapper.getPersonId(), scenario);
			if (person != null) {
				person.getAttributes().putAttribute(AgentSnapshotInfo.marker, true);
			}
		}
	}

	public Map<String, Restriction> getRestrictions() {
		return restrictions;
	}
}
