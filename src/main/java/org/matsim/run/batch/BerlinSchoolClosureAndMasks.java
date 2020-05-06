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
import org.matsim.episim.BatchRun.Option;
import org.matsim.episim.BatchRun.Parameter;
import org.matsim.episim.model.FaceMask;
import org.matsim.episim.policy.FixedPolicy;
import org.matsim.episim.policy.Restriction;
import org.matsim.run.modules.SnzScenario;
import org.matsim.run.modules.SnzScenario.LinearInterpolation;

import java.time.LocalDate;
import java.util.List;

/**
 * Batch run for Berlin using different school closure timing and mask options.
 */
public final class BerlinSchoolClosureAndMasks implements BatchRun<BerlinSchoolClosureAndMasks.Params> {

	public static final List<Option> OPTIONS = List.of(
			Option.of("Worn masks", 81)
					.measure("Mask type", "mask")
					.measure("Mask compliance", "maskCompliance"),

					Option.of("Additional acitivities", "By type and percent (%)", 74)
					.measure("Work, business, shopping and errands", "additionalFractionWorkShoppingBusinessErrands")
					.measure("Leisure", "additionalFractionLeisure"),

			Option.of("Additional educational activities", "Students returning (%)", 81)
					.measure("Going to kindergarten", "additionalFractionKiga")
					.measure("Going to schools", "additionalFractionSchools")
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
		episimConfig.setCalibrationParameter(0.000_002_8);
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
	public Config prepareConfig(int id, BerlinSchoolClosureAndMasks.Params params) {

		Config config = baseCase(id);
		EpisimConfigGroup episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);
		int offset = params.offset;
		FaceMask wornMask = FaceMask.valueOf(params.mask);
		episimConfig.setMaskCompliance(params.maskCompliance);

		episimConfig.setPolicy(FixedPolicy.class, buildPolicyBerlin(offset, params, wornMask)	);
	
		String policyFileName = "input/policy" + id + ".conf";
		episimConfig.setOverwritePolicyLocation(policyFileName);

		return config;
	}

	private com.typesafe.config.Config buildPolicyBerlin(int offset, Params params, FaceMask wornMask) {
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
				.restrict(74 - offset, 0.55 + params.additionalFractionWorkShoppingBusinessErrands, "work")
				.restrict(74 - offset, 0.4 + params.additionalFractionWorkShoppingBusinessErrands, "shopping", "errands", "business")
				.restrict(74 - offset, 0.1 + params.additionalFractionLeisure, "leisure")
				.restrict(81 - offset, Restriction.of(0.55 + params.additionalFractionWorkShoppingBusinessErrands, wornMask), "work")
				.restrict(81 - offset, Restriction.of(0.4 + params.additionalFractionWorkShoppingBusinessErrands, wornMask), "shopping", "errands", "business")
				.restrict(81 - offset, Restriction.of(0.1 + params.additionalFractionLeisure, wornMask), "leisure")
				//day 23 is the saturday 14th of march, so the weekend before schools got closed..
				.restrict(23 - offset, 0.1, "educ_primary", "educ_kiga")
				.restrict(23 - offset, 0., "educ_secondary", "educ_higher")
				.restrict(81 - offset, Restriction.of(0.1 + params.additionalFractionSchools, wornMask), "educ_primary")
				.restrict(81 - offset, Restriction.of(0. + params.additionalFractionSchools, wornMask), "educ_secondary")
				.restrict(81 - offset, Restriction.of(0.1 + params.additionalFractionKiga, wornMask), "educ_kiga")
				.restrict(81 - offset, Restriction.of(1, wornMask), "pt", "tr")
		       ;
		return builder.build();
	}

	public static final class Params {

		@IntParameter({-6})
		int offset;
		
		@Parameter({0., 0.4})
		double additionalFractionKiga;

		@Parameter({0., 0.2, 0.4})
		double additionalFractionSchools;

		@Parameter({0., 0.2})
		double additionalFractionWorkShoppingBusinessErrands;
		
		@Parameter({0., 0.2})
		double additionalFractionLeisure;

		@StringParameter({"NONE", "CLOTH", "SURGICAL"})
		String mask;

		@Parameter({0., 0.5, 0.9, 1.})
		double maskCompliance;

	}

}
