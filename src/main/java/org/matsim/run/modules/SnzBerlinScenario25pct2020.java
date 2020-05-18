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
package org.matsim.run.modules;

import com.google.inject.Provides;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.episim.EpisimConfigGroup;
import org.matsim.episim.model.FaceMask;
import org.matsim.episim.policy.FixedPolicy;
import org.matsim.episim.policy.Restriction;

import javax.inject.Singleton;

/**
 * Snz scenario for Berlin.
 *
 * @see AbstractSnzScenario
 */
public class SnzBerlinScenario25pct2020 extends AbstractSnzScenario2020 {
	private static final int alpha = 2; 

	/**
	 * The base policy based on actual restrictions in the past and google mobility data.
	 */
	public static FixedPolicy.ConfigBuilder basePolicy() {
		
		FixedPolicy.ConfigBuilder builder = FixedPolicy.config()
				.interpolate("2020-03-06", "2020-03-13", Restriction.of(1), Math.max( 0., 1. - alpha * 0.2 ), "work")
				.interpolate("2020-03-13", "2020-03-20", Restriction.of(Math.max( 0., 1. - alpha * 0.2 )), Math.max( 0., 1. - alpha * 0.5 ), "work")
				.interpolate("2020-03-20", "2020-03-27", Restriction.of(Math.max( 0., 1. - alpha * 0.5 )), Math.max( 0., 1. - alpha * 0.65 ), "work")
				.interpolate("2020-04-05", "2020-04-19", Restriction.of(Math.max( 0., 1. - alpha * 0.65 )), Math.max( 0., 1. - alpha * 0.45 ), "work")
				.interpolate("2020-04-20", "2020-04-27", Restriction.of(Math.max( 0., 1. - alpha * 0.45 )), Math.max( 0., 1. - alpha * 0.4 ), "work")

				.interpolate("2020-03-13", "2020-03-27", Restriction.of(Math.max( 0., 1. - alpha * 0.5 )), Math.max( 0., 1. - alpha * 0.9 ), "leisure", "visit", "shop_other")
				.restrict("2020-04-27", Restriction.of(0.1, FaceMask.CLOTH), "shop_other")
				
				.interpolate("2020-02-28", "2020-03-06", Restriction.of(1), Math.max( 0., 1. - alpha * 0.05 ), "shop_daily", "errands", "business")
				.interpolate("2020-03-06", "2020-03-13", Restriction.of(Math.max( 0., 1. - alpha * 0.05 )), Math.max( 0., 1. - alpha * 0.15 ), "shop_daily", "errands", "business")
				.interpolate("2020-03-13", "2020-03-20", Restriction.of(Math.max( 0., 1. - alpha * 0.15 )), Math.max( 0., 1. - alpha * 0.6 ), "shop_daily", "errands", "business")
				.interpolate("2020-04-20", "2020-04-27", Restriction.of(Math.max( 0., 1. - alpha * 0.6 )), Math.max( 0., 1. - alpha * 0.5 ), "shop_daily", "errands", "business")
				.interpolate("2020-04-28", "2020-05-04", Restriction.of(Math.max( 0., 1. - alpha * 0.5 ), FaceMask.CLOTH), Math.max( 0., 1. - alpha * 0.45 ), "shop_daily", "errands", "business")

				//saturday 14th of march, so the weekend before schools got closed..
				.restrict("2020-03-14", Math.max( 0., 1. - alpha * 0.9 ), "educ_primary", "educ_kiga") 
				.restrict("2020-03-14", 0., "educ_secondary", "educ_higher", "educ_tertiary", "educ_other")
				
				.restrict("2020-04-27", Restriction.of(1, FaceMask.CLOTH), "pt", "tr");

		return builder;
	}

	@Provides
	@Singleton
	public Config config() {

		Config config = getBaseConfig();

		EpisimConfigGroup episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);

		episimConfig.setInputEventsFile("../shared-svn/projects/episim/matsim-files/snz/BerlinV2/episim-input/be_2020_snz_episim_events_25pt.xml.gz");

		config.plans().setInputFile("../shared-svn/projects/episim/matsim-files/snz/BerlinV2/episim-input/be_2020_snz_entirePopulation_emptyPlans_withDistricts_25pt.xml.gz");

		episimConfig.setInitialInfections(50);
		episimConfig.setInitialInfectionDistrict("Berlin");
		episimConfig.setSampleSize(0.25);
		episimConfig.setCalibrationParameter(0.000_000_8);	
//		episimConfig.setCalibrationParameter(0.000_002_4);

		episimConfig.setStartDate("2020-02-13");
		episimConfig.setPolicy(FixedPolicy.class, basePolicy().build());

		config.controler().setOutputDirectory("./output-berlin-25pct-restricts-" + episimConfig.getStartDate() + "-" + alpha + "-" + episimConfig.getCalibrationParameter());


		return config;
	}

}
