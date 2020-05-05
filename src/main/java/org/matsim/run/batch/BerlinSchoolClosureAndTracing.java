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
package org.matsim.run.batch;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.episim.BatchRun;
import org.matsim.episim.EpisimConfigGroup;
import org.matsim.episim.TracingConfigGroup;
import org.matsim.episim.policy.FixedPolicy;
import org.matsim.run.modules.SnzScenario;
import org.matsim.run.modules.SnzScenario.LinearInterpolation;

import java.time.LocalDate;
import java.util.List;

/**
 * Batch run for Berlin executing different school closure and tracing options.
 */
public final class BerlinSchoolClosureAndTracing implements BatchRun<BerlinSchoolClosureAndTracing.Params> {

	public static final List<Option> OPTIONS = List.of(
			Option.of("Contact tracing", 67)
					.measure("Tracing Distance", "tracingDayDistance")
					.measure("Tracing Probability", "tracingProbability"),

			Option.of("Out-of-home activities limited", "By type and percent (%)", 67)
					.measure("Work activities", "remainingFractionWork")
					.measure("Other activities", "remainingFractionShoppingBusinessErrands")
					.measure("Leisure activities", "remainingFractionLeisure"),

			Option.of("Reopening of educational facilities", "Students returning (%)", 74)
					.measure("Going to primary school", "remainingFractionPrima")
					.measure("Going to kindergarten", "remainingFractionKiga")
					.measure("Going to secondary/univ.", "remainingFractionSeconHigher")
	);

	@Override
	public LocalDate startDate() {
		return LocalDate.of(2020, 2, 21);
	}

	@Override
	public Config baseCase(int id) {

		Config config = ConfigUtils.createConfig(new EpisimConfigGroup());
		config.plans().setInputFile("../be_v2_snz_entirePopulation_emptyPlans_withDistricts.xml.gz");


		EpisimConfigGroup episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);

		episimConfig.setInputEventsFile("../be_v2_snz_episim_events.xml.gz");
		episimConfig.setFacilitiesHandling(EpisimConfigGroup.FacilitiesHandling.snz);

		episimConfig.setSampleSize(0.25);
		episimConfig.setCalibrationParameter((0.000_002_8));
		episimConfig.setInitialInfections(50);
		episimConfig.setInitialInfectionDistrict("Berlin");

		SnzScenario.addParams(episimConfig);
		SnzScenario.setContactIntensities(episimConfig);

		return config;
	}

	@Override
	public List<Option> getOptions() {
		return OPTIONS;
	}

	@Override
	public Config prepareConfig(int id, BerlinSchoolClosureAndTracing.Params params) {

		Config config = baseCase(id);
		EpisimConfigGroup episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);
		int offset = params.offset;

		TracingConfigGroup tracingConfig = ConfigUtils.addOrGetModule(config, TracingConfigGroup.class);

		tracingConfig.setPutTraceablePersonsInQuarantineAfterDay(81 - offset);
		tracingConfig.setTracingProbability(params.tracingProbability);
		tracingConfig.setTracingDayDistance(params.tracingDayDistance);
		tracingConfig.setQuarantineHouseholdMembers(params.quarantineHouseholdMembers.contains("true"));


		episimConfig.setPolicy(FixedPolicy.class, buildPolicyBerlin(offset, params)	);
	
		String policyFileName = "input/policy" + id + ".conf";
		episimConfig.setOverwritePolicyLocation(policyFileName);

		return config;
	}

	private com.typesafe.config.Config buildPolicyBerlin(int offset, BerlinSchoolClosureAndTracing.Params params) {		
		FixedPolicy.ConfigBuilder builder = FixedPolicy.config();
		{
			final int firstDay = 16 - offset; //sat, 07.03.
			final int lastDay = 23 - offset; //sat, 14.03.
			LinearInterpolation interpolation = new LinearInterpolation( firstDay, 1., lastDay, 0.8 );
			for( int day = firstDay ; day <= lastDay ; day++ ){
				builder.restrict( day, interpolation.getValue( day ), "work" );
			}
		}
		{
			final int firstDay = 23 - offset; //sat, 14.03.
			final int lastDay = 30 - offset; //sat, 21.03.
			LinearInterpolation interpolation = new LinearInterpolation( firstDay, 0.8, lastDay, 0.5 );
			for( int day = firstDay ; day <= lastDay ; day++ ){
				builder.restrict( day, interpolation.getValue( day ), "work" );
			}
		}
		{
			final int firstDay = 30 - offset; //sat, 21.03.
			final int lastDay = 37 - offset; //sat, 28.03.
			LinearInterpolation interpolation = new LinearInterpolation( firstDay, 0.5, lastDay, 0.45 );
			for( int day = firstDay ; day <= lastDay ; day++ ){
				builder.restrict( day, interpolation.getValue( day ), "work" );
			}
		}
		{
			final int firstDay = 46 - offset; //mon, 06.04.
			final int lastDay = 60 - offset; //mon, 20.04.
			LinearInterpolation interpolation = new LinearInterpolation( firstDay, 0.45, lastDay, 0.55 );
			for( int day = firstDay ; day <= lastDay ; day++ ){
				builder.restrict( day, interpolation.getValue( day ), "work" );
			}
		}
		{
			final int firstDay = 24 - offset;
			final int lastDay = 38 - offset;
			LinearInterpolation interpolation = new LinearInterpolation( firstDay, 1., lastDay, 0.1 );
			for( int day = firstDay ; day <= lastDay ; day++ ){
				builder.restrict( day, interpolation.getValue( day ), "leisure" );
			}
		}
		{
			final int firstDay = 9 - offset;
			final int lastDay = 16 - offset;
			LinearInterpolation interpolation = new LinearInterpolation( firstDay, 1., lastDay, 0.95 );
			for( int day = firstDay ; day <= lastDay ; day++ ){
				builder.restrict( day, interpolation.getValue( day ), "shopping", "errands", "business" );
			}
		}
		{
			final int firstDay = 16 - offset;
			final int lastDay = 23 - offset;
			LinearInterpolation interpolation = new LinearInterpolation( firstDay, 0.95, lastDay, 0.85 );
			for( int day = firstDay ; day <= lastDay ; day++ ){
				builder.restrict( day, interpolation.getValue( day ), "shopping", "errands", "business" );
			}
		}
		{
			final int firstDay = 23 - offset;
			final int lastDay = 30 - offset;
			LinearInterpolation interpolation = new LinearInterpolation( firstDay, 0.85, lastDay, 0.4 );
			for( int day = firstDay ; day <= lastDay ; day++ ){
				builder.restrict( day, interpolation.getValue( day ), "shopping", "errands", "business" );
			}
		}
		builder
				//day 23 is the saturday 14th of march, so the weekend before schools got closed..
				.restrict(23 - offset, 0.1, "educ_primary", "educ_kiga")
				.restrict(23 - offset, 0., "educ_secondary", "educ_higher")
				.restrict(81 - offset, 0.1 + params.remainingFractionSchools, "educ_primary")
				.restrict(81 - offset, params.remainingFractionSchools, "educ_secondary")
				.restrict(81 - offset, params.remainingFractionKiga, "educ_kiga")
		       ;
		return builder.build();
	}

	public static final class Params {
		
		@IntParameter({-6})
		int offset;

		@Parameter({0.5, 0.1})
		double remainingFractionKiga;

		@Parameter({0.4, 0.2, 0.})
		double remainingFractionSchools;

		@Parameter({0.55, 0.65})
		double remainingFractionWork;

		@Parameter({0.4, 0.5})
		double remainingFractionShoppingBusinessErrands;

		@IntParameter({1, 3, 5})
		int tracingDayDistance;
		
		@IntParameter({0, 1, 3})
		int tracingDelay;

		@Parameter({1.0, 0.75, 0.5})
		double tracingProbability;
		
		@StringParameter({"true", "false"})
		String quarantineHouseholdMembers;

	}

}

