package org.matsim.run.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.contrib.roadpricing.RoadPricingConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.episim.EpisimConfigGroup;
import org.matsim.episim.policy.FixedPolicy;

/**
 * Scenario based on the publicly available Santiago scenario (https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/cl/santiago/).
 */
public class SantiagoScenario extends AbstractModule {

	/**
	 * Activity names of the default params from {@link #addDefaultParams(EpisimConfigGroup)}.
	 */
	public static final String[] DEFAULT_ACTIVITIES = {
			//"pt", "work", "leisure", "edu", "shop", "errands", "business", "other", "freight", "home"
			"pt", "work", "leisure", "educ", "shop", "visiting", "business", "other", "healthR", "home"
	};

	/**
	 * Adds default parameters that should be valid for most scenarios.
	 */
	public static void addDefaultParams(EpisimConfigGroup config) {
		// pt
		config.addContainerParams(new EpisimConfigGroup.InfectionParams("pt", "tr"));
		// regular out-of-home acts:
		config.addContainerParams(new EpisimConfigGroup.InfectionParams("work"));
		config.addContainerParams(new EpisimConfigGroup.InfectionParams("leisure", "leis"));
		config.addContainerParams(new EpisimConfigGroup.InfectionParams("educ"));
		config.addContainerParams(new EpisimConfigGroup.InfectionParams("shop"));
		config.addContainerParams(new EpisimConfigGroup.InfectionParams("visiting", "visi"));
		config.addContainerParams(new EpisimConfigGroup.InfectionParams("business", "busi"));
		config.addContainerParams(new EpisimConfigGroup.InfectionParams("other", "othe"));
		config.addContainerParams(new EpisimConfigGroup.InfectionParams("healthR", "heal"));
		// freight act:
		//config.addContainerParams(new EpisimConfigGroup.InfectionParams("freight"));
		// home act:
		config.addContainerParams(new EpisimConfigGroup.InfectionParams("home"));
	}

	@Provides
	@Singleton
	public Config config() {

		Config config = ConfigUtils.loadConfig("scenarios/Chile/v2b/v2b/santiago/config_baseCase10pct_rev01.xml",new EpisimConfigGroup() );
		//Config config = ConfigUtils.createConfig( new EpisimConfigGroup());
		EpisimConfigGroup episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);
		RoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.class);
		rpConfig.setTollLinksFile("scenarios//Chile/v2b/v2b/santiago/input/gantries.xml");

		//It is better to download the events file and run it locally
		//String url =  "scenarios/Chile/v2b/v2b/santiago/output_It0/0.events.xml.gz";
		String url =  "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/cl/santiago/v2b/santiago/output/baseCase10pct/output_events.xml.gz";
		episimConfig.setInputEventsFile(url);

		episimConfig.setFacilitiesHandling(EpisimConfigGroup.FacilitiesHandling.bln);
		//episimConfig.setSampleSize(0.01);
		episimConfig.setSampleSize(0.1);
		episimConfig.setCalibrationParameter(0.00001);
		//  episimConfig.setOutputEventsFolder("events");

		long closingIteration = 40; //Shutdown activities completely after certain day.

		addDefaultParams(episimConfig);
	//original adapted
/*		episimConfig.setPolicy(FixedPolicy.class, FixedPolicy.config()
				.shutdown(closingIteration, "leisure", "educ") //add visiting and healthR
				.restrict(closingIteration, 0.2, "work", "business", "other")
				.restrict(closingIteration, 0.3, "shop", "errands")
				.restrict(closingIteration, 0.5, "pt")
				.open(closingIteration + 60, DEFAULT_ACTIVITIES)
				.build()
		);*/
		//base case-Chile
/*		episimConfig.setPolicy(FixedPolicy.class, FixedPolicy.config()
				.open(closingIteration , DEFAULT_ACTIVITIES)
				.build()
		);*/

		episimConfig.setPolicy(FixedPolicy.class, FixedPolicy.config()
				.shutdown(closingIteration, "pt", "work", "leisure", "educ", "shop", "visiting", "business", "other", "healthR", "home")
				.open(closingIteration + 60, DEFAULT_ACTIVITIES)
				.build()
		);

		return config;
	}

}
