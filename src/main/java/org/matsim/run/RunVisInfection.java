/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.episim.InfectionEventHandler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import static org.matsim.core.config.groups.StrategyConfigGroup.*;

/**
* @author knagel
*/

public class RunVisInfection {
	private static final Logger log = Logger.getLogger( RunVisInfection.class );

	private static final String MODIFIED_TRANSIT_ENGINE_NAME = "modifiedTransitEngine";

	public static void main(String[] args) {

		for (String arg : args) {
			log.info( arg );
		}

		if ( args.length==0 ) {
			args = new String[] {"scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml"}  ;
		}

		Config config = RunBerlinScenario.prepareConfig( args ) ;

		config.global().setNumberOfThreads( 4 );
		config.qsim().setNumberOfThreads( 6 );

		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );

		config.controler().setFirstIteration( 0 );
		config.controler().setLastIteration( 1000 );

		config.controler().setWritePlansUntilIteration( -1 ); // accelerate startup

		final OTFVisConfigGroup otfVisConfigGroup = ConfigUtils.addOrGetModule( config, OTFVisConfigGroup.class );
		otfVisConfigGroup.setDrawTransitFacilityIds( false );
		otfVisConfigGroup.setDrawTransitFacilities( false );
		otfVisConfigGroup.setLinkWidth( 10.f );
		otfVisConfigGroup.setColoringScheme( OTFVisConfigGroup.ColoringScheme.infection );
		
		for ( final PlanCalcScoreConfigGroup.ActivityParams params : config.planCalcScore().getActivityParams() ) {
			if ( params.getActivityType().endsWith( "interaction" ) ) {
				params.setScoringThisActivityAtAll( false ) ;
			}
		}
		// yyyyyy why is this needed at all?  bug?  kai, mar'20

		config.qsim().setSnapshotStyle( SnapshotStyle.kinematicWaves );
		config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves );

		config.transit().setUsingTransitInMobsim( true );

		config.strategy().clearStrategySettings();
		config.strategy().addStrategySettings( new StrategySettings().setStrategyName( DefaultSelector.KeepLastSelected ).setSubpopulation( "person" ).setWeight( 1. ) );
		config.strategy().addStrategySettings( new StrategySettings().setStrategyName( DefaultSelector.KeepLastSelected ).setSubpopulation( "freight" ).setWeight( 1. ) );

		VspExperimentalConfigGroup vsp = ConfigUtils.addOrGetModule(config, VspExperimentalConfigGroup.class);
		vsp.setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

//		QSimComponentsConfigGroup qsimComponentsConfig = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );
//		List<String> components = qsimComponentsConfig.getActiveComponents();
//		components.remove( TransitEngineModule.TRANSIT_ENGINE_NAME );
//		components.add( MODIFIED_TRANSIT_ENGINE_NAME );
//		qsimComponentsConfig.setActiveComponents( components );

		// ---
		
		Scenario scenario = RunBerlinScenario.prepareScenario( config );

		// ---
		
		Controler controler = RunBerlinScenario.prepareControler( scenario ) ;

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				this.addEventHandlerBinding().to( InfectionEventHandler.class );
			}
		} );

//		controler.addOverridingQSimModule( new AbstractQSimModule(){
//			@Override protected void configureQSim(){
//				bind( TransitQSimEngine.class ).asEagerSingleton();
//
//				this.addQSimComponentBinding( MODIFIED_TRANSIT_ENGINE_NAME ).to(  TransitQSimEngine.class );
//
//				if ( this.getConfig().transit().isUseTransit() && this.getConfig().transit().isUsingTransitInMobsim() ) {
//					bind( TransitStopHandlerFactory.class ).to( ComplexTransitStopHandlerFactory.class ) ;
//				} else {
//					// Explicit bindings are required, so although it may not be used, we need provide something.
//					bind( TransitStopHandlerFactory.class ).to( SimpleTransitStopHandlerFactory.class );
//				}
//
//				bind( UmlaufBuilder.class ).to( GreedyUmlaufBuilderImpl.class );
//
//			}
//		} );
		
//		controler.addOverridingModule( new OTFVisLiveModule() ) ;
//		controler.addOverridingModule(new DrtSpeedUpModule());
		
		// ---
		
		controler.run() ;
	}

}

