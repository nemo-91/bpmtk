package de.drscc.automaton;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import com.google.common.collect.BiMap;

import de.normalisiert.utils.graphs.ElementaryCyclesSearch;

/*
 * Copyright © 2009-2017 The Apromore Initiative.
 *
 * This file is part of "Apromore".
 *
 * "Apromore" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * "Apromore" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program.
 * If not, see <http://www.gnu.org/licenses/lgpl-3.0.html>.
 */

/**
 * @author Daniel Reissner,
 * @version 1.0, 01.02.2017
 */

public class Automaton {

	private Map<Integer, State> states;
	private BiMap<Integer, String> eventLabels;
	private BiMap<String, Integer> inverseEventLabels;
	private Map<Integer, Transition> transitions;
	private int source;
	private IntHashSet finalStates;
	private Set<IntIntHashMap> finalConfigurations;
	private int skipEvent = -2;
	public BiMap<IntArrayList, IntArrayList> caseTracesMapping;
	public Map<IntArrayList, Integer> caseFrequencies = new HashMap<IntArrayList, Integer>();
	private int minNumberOfModelMoves = Integer.MAX_VALUE;
	private Map<IntIntHashMap, List<IntArrayList>> configCaseMapping;
	public Map<Integer, String> caseIDs;
	
	public Automaton(Map<Integer, State> states, BiMap<Integer, String> labelMapping, BiMap<String, Integer> inverseLabelMapping, Map<Integer, Transition> transitions, 
			int initialState, IntHashSet FinalStates, BiMap<IntArrayList, IntArrayList> caseTracesMapping, Map<Integer, String> caseIDs) throws IOException
	{
		this.states = states;
		this.eventLabels = labelMapping;
		this.inverseEventLabels = inverseLabelMapping;
		this.transitions = transitions;
		this.source = initialState;
		this.finalStates = FinalStates;
		this.caseTracesMapping = caseTracesMapping;
		for(IntArrayList trace : caseTracesMapping.keySet())
			caseFrequencies.put(trace,  caseTracesMapping.get(trace).size());
		this.caseIDs = caseIDs;
		//this.toDot("/Users/daniel/Documents/workspace/dafsa/Road Traffic/test.dot");
		this.calculateLogFutures();
//		PrintWriter pw = new PrintWriter("/Users/daniel/Documents/workspace/Master thesis paper tests/Road Traffic/tracesContained.txt");
//		for(int finalState : this.finalStates.toArray())
//			for(Multiset<Integer> finalConfiguration : this.finalConfigurations.get(finalState))
//				for(IntArrayList potentialPath : this.states.get(finalState).potentialPathsAndTraceLabels().get(finalConfiguration).keySet())
//					if(tracesContained.containsKey(this.states.get(finalState).potentialPathsAndTraceLabels().get(finalConfiguration).get(potentialPath)))
//						tracesContained.replace(this.states.get(finalState).potentialPathsAndTraceLabels().get(finalConfiguration).get(potentialPath), true);
//		for(IntArrayList trace : tracesContained.keySet())
//				pw.println(trace + " is Contained? " + tracesContained.get(trace));
//		pw.close();
		
	}
	
	public Automaton(Map<Integer, State> states, BiMap<Integer, String> eventLabels, BiMap<String, Integer> inverseLabelMapping, Map<Integer, Transition> transitions,
			int initialState, IntHashSet FinalStates, int skipEvent) throws IOException
	{
		this.states = states;
		this.eventLabels = eventLabels;
		this.inverseEventLabels = inverseLabelMapping;
		this.transitions = transitions;
		this.source = initialState;
		this.finalStates = FinalStates;
		this.skipEvent = skipEvent;
		//this.toDot("test.dot");
//		this.discoverFinalConfigurationsForModel();
	}
	
	public Map<Integer, State> states()
	{
		return this.states;
	}
	
	public BiMap<Integer, String> eventLabels()
	{
		return this.eventLabels;
	}
	
	public BiMap<String, Integer> inverseEventLabels()
	{
		return this.inverseEventLabels;
	}
	
	public Map<Integer, Transition> transitions()
	{
		return this.transitions;
	}
	
	public State source()
	{
		return this.states.get(this.source);
	}
	
	public int sourceID()
	{
		return this.source;
	}
	
	public IntHashSet finalStates()
	{
		return this.finalStates;
	}
	
	public Set<IntIntHashMap> finalConfigurations()
	{
		return this.finalConfigurations;
	}
	
	public int skipEvent()
	{
		return this.skipEvent;
	}
	
	public Map<IntIntHashMap, List<IntArrayList>> configCasesMapping()
	{
		if(this.configCaseMapping==null)
			this.configCaseMapping = new UnifiedMap<IntIntHashMap, List<IntArrayList>>();
		return this.configCaseMapping;
	}
	
	private void discoverFinalConfigurationsForModel() throws FileNotFoundException//(boolean discoverPotentialPaths)
	{
		boolean[][] adjacencyMatrix = new boolean[Collections.max(this.states().keySet())+1][Collections.max(this.states().keySet())+1];
		for(Transition tr : this.transitions().values())
			adjacencyMatrix[tr.source().id()][tr.target().id()] = true;
		Object[] graphNodes = new Object[Collections.max(this.states().keySet())+1];
		for(int key : this.states().keySet())
			graphNodes[key] = this.states().get(key);
		@SuppressWarnings("unchecked")
		List<List<State>> cycles = (List<List<State>>) new ElementaryCyclesSearch(adjacencyMatrix, graphNodes).getElementaryCycles();
		Iterator<List<State>> it2 = cycles.iterator();
		while(it2.hasNext())
			if(it2.next().size()==1) it2.remove();
		for(int i = 0; i < Collections.max(this.states().keySet())+1; i++)
		{
			if(adjacencyMatrix[i][i])
			{
				List<State> selfLoop = new FastList<State>();
				selfLoop.add(this.states().get(i));
				cycles.add(selfLoop);
			}
		}
		UnifiedSet<IntIntHashMap> oldLoopLabels;
		UnifiedSet<IntIntHashMap> newLoopLabels = null;
		UnifiedSet<IntIntHashMap> elemLoopLabels = new UnifiedSet<IntIntHashMap>();
		for (int i = 0; i < cycles.size(); i++)
		{
			oldLoopLabels = new UnifiedSet<IntIntHashMap>();
			IntIntHashMap init = new IntIntHashMap();
			oldLoopLabels.add(init);
			List<State> cycle = cycles.get(i);
			for (int j = 0; j < cycle.size(); j++) 
			{
				State actState = ((State) cycle.get(j));
				newLoopLabels = new UnifiedSet<IntIntHashMap>();
				actState.isLoopState = true;
				actState.loops().add(i);
				for(IntIntHashMap element : oldLoopLabels)
				{
					int target = j+1;
					if(j==cycle.size()-1) target = 0;
					for(Transition tr : actState.outgoingTransitions())
						if(tr.target().equals((State) cycle.get(target)))
						{
							IntIntHashMap loopLabels = new IntIntHashMap(element);
							loopLabels.put(tr.eventID(), 1);
							newLoopLabels.add(loopLabels);
						}
				}
				oldLoopLabels = newLoopLabels;
				

//				String node = "" + ((State) cycle.get(j)).id();
//				if (j < cycle.size() - 1) {
//					System.out.print(node + " -> ");
//				} else {
//					System.out.print(node);
//				}
			}
//			System.out.println();
			//loopLabelsStatesMapping.put(loopLabels, cycle);
			elemLoopLabels.addAll(newLoopLabels);
			for (int j = 0; j < cycle.size(); j++)
				for(IntIntHashMap loopLabels : newLoopLabels)
					((State) cycle.get(j)).loopLabels().add(loopLabels);
		}
		//System.out.println(this.eventLabels());
		//System.out.println(elemLoopLabels);
//		this.calculateLoopCombinations(loopLabelsStatesMapping);
		
		TarjanSCC scc = new TarjanSCC(this);
		int m = scc.count();
		IntArrayList[] components = new IntArrayList[m];
		for (int i = 0; i < m; i++) 
		{
			components[i] = new IntArrayList();
		}
		for (State state : this.states().values()) {
			components[scc.id(state.id())].add(state.id());
			state.setComponent(scc.id(state.id()));
		}
		IntObjectHashMap<List<Transition>> compInArcs = new IntObjectHashMap<List<Transition>>();
		IntObjectHashMap<List<Transition>> compOutArcs = new IntObjectHashMap<List<Transition>>();
		IntObjectHashMap<IntIntHashMap> cLoopLabels = new IntObjectHashMap<IntIntHashMap>();
		List<Transition> inArcs;
		List<Transition> outArcs;
		IntIntHashMap cLabels;
		for(IntArrayList component : components)
		{
			for(int stID : component.toArray())
			{
				State state = this.states().get(stID);
				if((inArcs = compInArcs.get(state.component()))==null)
				{
					inArcs = new FastList<Transition>();
					compInArcs.put(state.component(), inArcs);
				}
				for(Transition in : state.incomingTransitions())
				{
					if(component.contains(in.source().id())) 
						{in.explore=false; continue;}
					inArcs.add(in);
				}
				if((outArcs = compOutArcs.get(state.component()))==null)
				{
					outArcs = new FastList<Transition>();
					compOutArcs.put(state.component(), outArcs);
				}
				for(Transition out : state.outgoingTransitions())
				{
					if(component.contains(out.target().id())) 
					{
						if((cLabels = cLoopLabels.get(state.component()) ) ==null )
						{
							cLabels = new IntIntHashMap();
							cLoopLabels.put(state.component(), cLabels);
						}
						cLabels.put(out.eventID(), 200);
					}
					outArcs.add(out);
				}
			}
		}
//		PrintWriter pw = new PrintWriter("scc.dot");
//		pw.println("digraph fsm {");
//		pw.println("rankdir=LR;");
//		pw.println("node [shape=circle,style=filled, fillcolor=white]");
//		for(int c = 0; c < m; c++)
//		{
//			pw.printf("%d [EID=\"%s\"];%n", c, c);
//			for(Transition tr : compInArcs.get(c))
//			{
//				pw.printf("%d -> %d [EID=\"%s\"];%n", tr.source().component(), c, this.eventLabels().get(tr.eventID()));
//			}
//		}
//		pw.println("}");
//		pw.close();
		
		//TODO:Implement Component futures -> done
        //visited and toBeVisited for component IDs
        IntArrayList toBeVisited = new IntArrayList();
        IntHashSet visited = new IntHashSet();
        IntIntHashMap possibleFuture;
        if(this.finalConfigurations == null)
        	this.finalConfigurations = new UnifiedSet<IntIntHashMap>();
        finalConfigurations.clear();
        this.states().values().forEach(state -> state.possibleFutures().clear());
        
        for(int finalState : this.finalStates().toArray())
		{
        	//this.transitions().values().forEach(transition -> transition.explore=true);
        	State fState = this.states().get(finalState);
        	for(List<Transition> trs : compInArcs.values())
        		for(Transition tr : trs) 
        			tr.explore = true;
			for(Transition tr : compInArcs.get(fState.component()))
			{
				tr.explore = false;
				State trSource = tr.source();
				if(trSource.isLoopState)
				{
					Set<IntIntHashMap> uniqueCycles = new UnifiedSet<IntIntHashMap>();
					for(int stID : components[trSource.component()].toArray())
					{
						State compState = this.states().get(stID);
						uniqueCycles.addAll(compState.loopLabels());
					}
					for(int stID : components[trSource.component()].toArray())
					{
						State compState = this.states().get(stID);
						compState.futureLoops().addAll(uniqueCycles);
					}
//					for(int stID : components[trSource.component()].toArray())
//					{
//						State compState = this.states().get(stID);
//						if((statePossibleFutures=compState.possibleFutures().get(finalState))==null)
//						{
//							statePossibleFutures = new UnifiedSet<IntIntHashMap>();
//							compState.possibleFutures().put(finalState, statePossibleFutures);
//						}
//						for(IntIntHashMap loopLabels : compState.loopLabels())
//						{
//							possibleFuture = new IntIntHashMap();
//							possibleFuture.addToValue(tr.eventID(), 1);
//							this.mapAddAllSpecial(possibleFuture, loopLabels);
//							statePossibleFutures.add(possibleFuture);
//						}
//					}
					IntIntHashMap loopLabels = cLoopLabels.get(trSource.component());
					for(int stID : components[trSource.component()].toArray())
					{
						State compState = this.states().get(stID);
						possibleFuture = new IntIntHashMap();
						possibleFuture.addToValue(tr.eventID(), 1);
						this.mapAddAllSpecial(possibleFuture, loopLabels);
						compState.possibleFutures().add(possibleFuture);
					}
				}
				else
				{
					possibleFuture = new IntIntHashMap();
					possibleFuture.addToValue(tr.eventID(), 1);
					trSource.possibleFutures().add(possibleFuture);
				}
				
				boolean compExplore = true;
				for(Transition out : compOutArcs.get(trSource.component()))
				{
					if(out.explore)
						compExplore =false;
				}
				if(compExplore)
					if(visited.add(trSource.component()))
						toBeVisited.add(trSource.id());
			}	
		}
        
        while(!toBeVisited.isEmpty())
		{
			State state = this.states().get(toBeVisited.removeAtIndex(0));
			if(!state.loopLabels().isEmpty())
				state.futureLoops().addAll(state.loopLabels());
			
			for(Transition tr : compInArcs.get(state.component()))
			{
				if(tr.explore)
				{
				tr.explore = false;
				State trSource = tr.source();
				if(trSource.isLoopState)
				{
					Set<IntIntHashMap> uniqueCycles = new UnifiedSet<IntIntHashMap>();
					for(int stID : components[trSource.component()].toArray())
					{
						State compState = this.states().get(stID);
						uniqueCycles.addAll(compState.loopLabels());
					}
					for(int stID : components[trSource.component()].toArray())
					{
						State compState = this.states().get(stID);
						compState.futureLoops().addAll(uniqueCycles);
					}
//					for(IntIntHashMap sourcePossibleFuture : state.possibleFutures().get(finalState))
//					{
//						for(int stID : components[trSource.component()].toArray())
//						{
//							State compState = this.states().get(stID);
//							if((statePossibleFutures = compState.possibleFutures().get(finalState))==null)
//							{
//								statePossibleFutures = new UnifiedSet<IntIntHashMap>();
//								compState.possibleFutures().put(finalState, statePossibleFutures);
//							}
//							for(IntIntHashMap loopLabels : compState.loopLabels())
//							{
//								possibleFuture = new IntIntHashMap(sourcePossibleFuture);
//								possibleFuture.addToValue(tr.eventID(), 1);
//								this.mapAddAllSpecial(possibleFuture,loopLabels);
//								statePossibleFutures.add(possibleFuture);
//							}
//						}
//					}
					for(IntIntHashMap sourcePossibleFuture : state.possibleFutures())
					{
						IntIntHashMap loopLabels = cLoopLabels.get(trSource.component());
						for(int stID : components[trSource.component()].toArray())
						{
							State compState = this.states().get(stID);
							possibleFuture = new IntIntHashMap(sourcePossibleFuture);
							possibleFuture.addToValue(tr.eventID(), 1);
							this.mapAddAllSpecial(possibleFuture,loopLabels);
							compState.possibleFutures().add(possibleFuture);
						}
					}
				}
				else
				{
					for(IntIntHashMap sourcePossibleFuture : state.possibleFutures())
					{
						possibleFuture = new IntIntHashMap(sourcePossibleFuture);
						if(!(possibleFuture.get(tr.eventID())==200))
							possibleFuture.addToValue(tr.eventID(), 1);
						trSource.possibleFutures().add(possibleFuture);
					}
				}
				
				if(state.hasLoopFuture())
					trSource.futureLoops().addAll(state.futureLoops());
				boolean compExplore = true;
				for(Transition out : compOutArcs.get(trSource.component()))
				{
					if(out.explore)
						compExplore =false;
				}
				if(compExplore)
					if(visited.add(trSource.component()))
						toBeVisited.add(trSource.id());
				}
			}
		}
		this.finalConfigurations().addAll(this.source().possibleFutures());
		//System.out.println(this.finalConfigurations().get(finalState));
        for(IntIntHashMap finalConfiguration : this.finalConfigurations)
    	{
    		IntIntHashMap test = new IntIntHashMap(finalConfiguration);
    		test.remove(this.skipEvent);
    		for(int key : test.keySet().toArray())
    			if(test.get(key)>=200)
    				test.put(key, 1);
    		this.minNumberOfModelMoves = Math.min(this.minNumberOfModelMoves, (int) test.sum());
    	}
	}
	
	private void calculateLogFutures()
	{
		this.configCasesMapping().clear();
		IntIntHashMap config;
		List<IntArrayList> cases = null;
		for(IntArrayList trace : this.caseTracesMapping.keySet())
		{
			config = new IntIntHashMap();
			for(int element : trace.distinct().toArray())
				config.put(element, trace.count(t -> t==element));
			if((cases = this.configCasesMapping().get(config))==null)
			{
				cases = new FastList<IntArrayList>();
				this.configCasesMapping().put(config, cases);
			}
			cases.add(trace);
		}
//		this.states().values().forEach(state -> state.possibleFutures().clear());
//		this.transitions().values().forEach(tr -> tr.explore=true);
//		
//		IntArrayList toBeVisited = new IntArrayList();
//		State trSource;
//		//boolean exploreState;
//		IntIntHashMap possibleFuture;
//		State f_state;
//		Set<IntIntHashMap> possibleFutures = null;
//		if(finalConfigurations==null)
//			this.finalConfigurations = new HashMap<Integer, Set<IntIntHashMap>>();
//		finalConfigurations.clear();
//		this.states().values().forEach(state -> state.possibleFutures().clear());
//		
//		for(int finalState : this.finalStates().toArray())
//		{
//			//IntHashSet visitedStates = new IntHashSet();
//			f_state = this.states().get(finalState); 
//			if(!f_state.possibleFutures().containsKey(finalState))
//				f_state.possibleFutures().put(finalState, new UnifiedSet<IntIntHashMap>());
//			for(Transition tr : f_state.incomingTransitions())
//			{
//				trSource = tr.source();
//				possibleFuture = new IntIntHashMap(); 
//				possibleFuture.addToValue(tr.eventID(), 1);
//				if((possibleFutures = trSource.possibleFutures().get(finalState)) == null)
//				{
//					possibleFutures = new UnifiedSet<IntIntHashMap>();
//					trSource.possibleFutures().put(finalState, possibleFutures);
//				}
//				possibleFutures.add(possibleFuture);
//				toBeVisited.add(trSource.id());
//			}
//			while(!toBeVisited.isEmpty())
//			{
//				State state = this.states().get(toBeVisited.removeAtIndex(0));
//				for(Transition tr : state.incomingTransitions())
//				{
//					trSource = tr.source();
//					for(IntIntHashMap futureExtensions : state.possibleFutures().get(finalState))
//					{
//						possibleFuture = new IntIntHashMap(futureExtensions);
//						possibleFuture.addToValue(tr.eventID(), 1);
//						if((possibleFutures = trSource.possibleFutures().get(finalState)) == null)
//						{
//							possibleFutures = new UnifiedSet<IntIntHashMap>();
//							trSource.possibleFutures().put(finalState, possibleFutures);
//						}
//						possibleFutures.add(possibleFuture);
//						if(!toBeVisited.contains(trSource.id()))
//							toBeVisited.add(trSource.id());
//					}
//				}
//			}
//			Set<IntIntHashMap> f_Configurations = this.source().possibleFutures().get(finalState);
//			finalConfigurations.put(finalState, f_Configurations);
//	}
//			int state;
//			Map<Integer, List<IntArrayList>> potentialPaths;
//			Map<IntArrayList, IntIntHashMap> visited;
//			IntArrayList newPotentialPath;
//			IntIntHashMap newSetOfLabels = null;
//			Set<IntIntHashMap> possibleFuturesTarget;
//			List<IntArrayList> relPotentialPaths;
//			for(IntIntHashMap finalConfiguration : f_Configurations)
//			{
//				potentialPaths = new HashMap<Integer, List<IntArrayList>>();
//				visited = new HashMap<IntArrayList, IntIntHashMap>();
//				boolean finalNodeFound = false;
//				potentialPaths.put(this.sourceID(), new FastList<IntArrayList>());
//				potentialPaths.get(this.sourceID()).add(new IntArrayList());
//				potentialPaths.get(this.sourceID()).iterator().next().add(this.sourceID());
//				visited.put(new IntArrayList(), new IntIntHashMap());
//				visited.keySet().iterator().next().add(this.sourceID());
//				
//				for(Transition tr : this.source().outgoingTransitions())
//				{
//					for(IntArrayList potentialPath : potentialPaths.get(tr.source().id()))
//					{
//						int targetID = tr.target().id();
//						exploreState = false;
//						finalNodeFound=false;
//						newSetOfLabels = new IntIntHashMap();
//						newSetOfLabels.addToValue(tr.eventID(), 1);
//						
//						if(targetID==finalState && newSetOfLabels.equals(finalConfiguration))
//							{finalNodeFound=true;}
//						else if((possibleFuturesTarget = tr.target().possibleFutures().get(finalState)) !=null)
//						{
//							for(IntIntHashMap possibleFutureTarget : possibleFuturesTarget)
//							{
//								IntIntHashMap possibleFinalConfiguration = new IntIntHashMap(possibleFutureTarget);
//								this.mapAddAll(possibleFinalConfiguration, newSetOfLabels);
//								if(finalConfiguration.equals(possibleFinalConfiguration))
//									{exploreState = true; break;}
//							}
//						}
//						if(!exploreState && !finalNodeFound) continue;
//						newPotentialPath = new IntArrayList();
//						newPotentialPath.addAll(potentialPath);
//						newPotentialPath.add(targetID);
//						
//						if((relPotentialPaths = potentialPaths.get(targetID)) == null)
//						{
//							relPotentialPaths = new FastList<IntArrayList>();
//							potentialPaths.put(targetID, relPotentialPaths);
//						}
//						relPotentialPaths.add(newPotentialPath);
//						visited.put(newPotentialPath, newSetOfLabels);
//						if(!toBeVisited.contains(targetID) && !finalNodeFound)
//							toBeVisited.add(targetID);
//					}
//				}
//				
//				while(!toBeVisited.isEmpty())
//				{
//					state = toBeVisited.removeAtIndex(0);
//					if(state == finalState) continue;
//
//					for(Transition tr : this.states().get(state).outgoingTransitions())
//					{
//						for(IntArrayList potentialPath : potentialPaths.get(tr.source().id()))
//						{
//							int targetID = tr.target().id();
//							exploreState = false;
//							finalNodeFound=false;
//							newSetOfLabels = new IntIntHashMap(visited.get(potentialPath));
//							newSetOfLabels.addToValue(tr.eventID(), 1);
//							
//							if(targetID==finalState && newSetOfLabels.equals(finalConfiguration))
//								{finalNodeFound=true;}
//							else if((possibleFuturesTarget = tr.target().possibleFutures().get(finalState)) !=null)
//							{
//								for(IntIntHashMap possibleFutureTarget : possibleFuturesTarget)
//								{
//									IntIntHashMap possibleFinalConfiguration = new IntIntHashMap(possibleFutureTarget);
//									this.mapAddAll(possibleFinalConfiguration, newSetOfLabels);
//									if(finalConfiguration.equals(possibleFinalConfiguration))
//										{exploreState = true; break;}
//								}
//							}
//							if(!exploreState && !finalNodeFound) continue;
//							newPotentialPath = new IntArrayList();
//							newPotentialPath.addAll(potentialPath);
//							newPotentialPath.add(targetID);
//							
//							if((relPotentialPaths = potentialPaths.get(targetID)) == null)
//							{
//								relPotentialPaths = new FastList<IntArrayList>();
//								potentialPaths.put(targetID, relPotentialPaths);
//							}
//							relPotentialPaths.add(newPotentialPath);
//							visited.put(newPotentialPath, newSetOfLabels);
//							if(!toBeVisited.contains(targetID) && !finalNodeFound)
//								toBeVisited.add(targetID);
//						}
//					}
//				}
//				f_state.potentialPathsAndTraceLabels().put(finalConfiguration, new UnifiedMap<IntArrayList, IntArrayList>()); //potentialPaths.get(finalState)
//				//System.out.println(finalState +" - " + finalConfiguration + " - " + potentialPaths.get(finalState));
//				IntArrayList relevantTraceLabels = null;
//				IntIntHashMap testSet;
//				for(IntArrayList potentialPath : potentialPaths.get(finalState))
//				{
//					relevantTraceLabels = new IntArrayList();
//					f_state.potentialPathsAndTraceLabels().get(finalConfiguration).put(potentialPath, relevantTraceLabels);
//					testSet = new IntIntHashMap(finalConfiguration);
//						
//					int lastState = -1;
//					for(int i=0; i<potentialPath.size(); i++)
//					{
//						state = potentialPath.get(i);
//						for(Transition tr: this.states().get(state).incomingTransitions())
//						{
//							if(tr.source().id()==lastState && testSet.containsKey(tr.eventID()))
//							{
//								testSet.addToValue(tr.eventID(), -1);
//								if(testSet.get(tr.eventID())==0) testSet.remove(tr.eventID());
//								relevantTraceLabels.add(tr.eventID());
//								break;
//							}
//						}
//						lastState = state;
//					}
//					if(!caseTracesMapping.containsKey(relevantTraceLabels))
//						System.out.println(relevantTraceLabels);
//				}
//			}
	}
	
	public void calculateLoopCombinations(Map<IntIntHashMap, List<State>> loopLabelsStatesMapping)
	{
		int size;
		IntIntHashMap combinedLoopLabels;
		List<IntIntHashMap> keyList;
		List<State> loop1 = null;
		List<State> loop2 = null;
		for(State state : this.states().values())
			if(state.isLoopState)
				if((size = state.loopLabels().size())>=2)
				{
					do
					{
						size = state.loopLabels().size();
						keyList = new FastList<IntIntHashMap>(state.loopLabels());
						for(int loopIt1 =0; loopIt1<keyList.size(); loopIt1++)
							for(int loopIt2 =0; loopIt2 < keyList.size(); loopIt2++)
							{
								if(loopIt1>=loopIt2) continue;
								combinedLoopLabels = new IntIntHashMap(keyList.get(loopIt1));
								this.mapAddAllSpecial(combinedLoopLabels, keyList.get(loopIt2));
								if(state.loopLabels().add(combinedLoopLabels))
								{
									if((loop1 = loopLabelsStatesMapping.get(keyList.get(loopIt1)))==null)
										loop1= new FastList<State>();
									Set<State> combinedStates = new UnifiedSet<State>(loop1);
									if((loop2 = loopLabelsStatesMapping.get(keyList.get(loopIt2)))!=null)
										combinedStates.addAll(loop2);
									for(State st : combinedStates)
										st.loopLabels().add(combinedLoopLabels);
								}
							}	
					}
					while(size<state.loopLabels().size());
				}
	}
	
	public void mapAddAll(IntIntHashMap base, IntIntHashMap addition)
	{
		for(int key : addition.keySet().toArray())
		{
			base.addToValue(key, addition.get(key));
		}
			
	}
	
	public void mapAddAllSpecial(IntIntHashMap base, IntIntHashMap addition)
	{
		int count;
		for(int key : addition.keySet().toArray())
		{
			count = base.get(key) + 200;
			if(count>200) count=200;
			base.put(key, count);
		}
	}
	
	public void toDot(PrintWriter pw) throws IOException {
		pw.println("digraph fsm {");
		pw.println("rankdir=LR;");
		pw.println("node [shape=circle,style=filled, fillcolor=white]");
		
		for(State n : this.states.values()) {
			if(n.isSource()) {
				pw.printf("%d [EID=\"%s\", fillcolor=\"gray\"];%n", n.id(), n.label());
				//pw.printf("%d [EID=\"%d\", fillcolor=\"gray\"];%n", n.id(), n.id());
			} else {
				pw.printf("%d [EID=\"%s\"];%n", n.id(), n.label());
				//pw.printf("%d [EID=\"%d\"];%n", n.id(), n.id());
			}
			
			for(Transition t : n.outgoingTransitions()) {
				pw.printf("%d -> %d [EID=\"%s\"];%n", n.id(), t.target().id(), this.eventLabels().get(t.eventID()));
			}

			if(n.isFinal()) {
				String comment = "";
				/*for(Set<Integer> finalConfiguration: this.finalConfigurations().get(n.id()))
				{
					comment = comment + "<br/>Final Configuration: ";
					for(int event : finalConfiguration)
						comment = comment + this.getEvents().get(event).EID() + ", ";
					comment = comment.substring(0, comment.length() -2);
				}*/
				pw.printf("%d [EID=<%s%s>, shape=doublecircle];%n", n.id(), n.label(), comment);
				//pw.printf("%d [EID=\"%d\", shape=doublecircle];%n", n.id(), n.id());
			}
		}
		pw.println("}");
	}
	
	public void toDot(String fileName) throws IOException {
		PrintWriter pw = new PrintWriter(fileName);
		toDot(pw);
		pw.close();
	}
	public int minNumberOfModelMoves()
	{
		return this.minNumberOfModelMoves;
	}
}
