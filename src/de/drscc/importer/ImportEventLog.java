package de.drscc.importer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.in.XesXmlGZIPParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.drscc.automaton.Automaton;
import de.drscc.automaton.State;
import de.drscc.automaton.Transition;
import name.kazennikov.dafsa.AbstractIntDAFSA;
import name.kazennikov.dafsa.IntDAFSAInt;

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

public class ImportEventLog {
	private final String conceptname = "concept:name";
	private BiMap<Integer, String> labelMapping;
	private BiMap<String, Integer> inverseLabelMapping;
	private BiMap<Integer, State> stateMapping;
	private BiMap<Integer, Transition> transitionMapping;
	private IntHashSet finalStates;
	//private Map<IntArrayList, Boolean> tracesContained;
	private BiMap<IntArrayList, IntArrayList> caseTracesMapping;
	//private IntObjectHashMap<String> traceIDtraceName;
	
	public XLog importEventLog(String fileName) throws Exception
	{
		File xesFileIn = new File(fileName);
		XesXmlParser parser = new XesXmlParser(new XFactoryNaiveImpl());
        if (!parser.canParse(xesFileIn)) {
        	parser = new XesXmlGZIPParser();
        	if (!parser.canParse(xesFileIn)) {
        		throw new IllegalArgumentException("Unparsable log file: " + xesFileIn.getAbsolutePath());
        	}
        }
        List<XLog> xLogs = parser.parse(xesFileIn);

       return xLogs.remove(0);
	}
	
	public Automaton convertLogToAutomatonFrom(String fileName) throws Exception {
		//long start = System.nanoTime();
		File xesFileIn = new File(fileName);
		XesXmlParser parser = new XesXmlParser(new XFactoryNaiveImpl());
        if (!parser.canParse(xesFileIn)) {
        	parser = new XesXmlGZIPParser();
        	if (!parser.canParse(xesFileIn)) {
        		throw new IllegalArgumentException("Unparsable log file: " + xesFileIn.getAbsolutePath());
        	}
        }
        List<XLog> xLogs = parser.parse(xesFileIn);
        XLog xLog = xLogs.remove(0);
        /*
        while (xLogs.size() > 0) {
        	xLog.addAll(xLogs.remove(0));
        }
        */
        //long end = System.nanoTime();
        //System.out.println("Log import: " + TimeUnit.SECONDS.markovian((end - start), TimeUnit.NANOSECONDS) + "s");
        return this.createDAFSAfromLog(xLog);
	}
	
	public Automaton createDAFSAfromLog(XLog xLog) throws IOException
	{	
		//long start = System.nanoTime();
		//tracesContained = new UnifiedMap<IntArrayList, Boolean>();
		caseTracesMapping = HashBiMap.create();
		Map<Integer, String> caseIDs = new UnifiedMap<Integer, String>();
		IntArrayList traces;
		//traceIDtraceName = new IntObjectHashMap<String>();
		labelMapping = HashBiMap.create();
		inverseLabelMapping = HashBiMap.create();
		String eventName;
		String traceID;
		int translation = 0;
		int iTransition = 0;
		IntArrayList tr;
		IntDAFSAInt fsa = new IntDAFSAInt();
		Integer key = null;
		UnifiedSet<IntArrayList> visited = new UnifiedSet<IntArrayList>();
		int it = 0;
		
		XTrace trace;
		int i, j;
		for (i = 0; i < xLog.size(); i++)
		{
			trace = xLog.get(i);
			traceID = ((XAttributeLiteral) trace.getAttributes().get(conceptname)).getValue();
			tr = new IntArrayList(trace.size());
			for (j = 0; j < trace.size(); j++)
			{
				eventName = ((XAttributeLiteral) trace.get(j).getAttributes().get(conceptname)).getValue();//xce.extractName(event);
				if((key = (inverseLabelMapping.get(eventName))) == null)
				{
					//labelMapping.put(translation, eventName);
					inverseLabelMapping.put(eventName, translation);
					key = translation;
					translation++;
				}
				tr.add(key);
			}
			
			if(visited.add(tr))
				fsa.addMinWord(tr);
			//caseTracesMapping.put(tr, tracesLabelsMapping.get(trace));
			if((traces = caseTracesMapping.get(tr))==null)
			{
				traces = new IntArrayList();
				caseTracesMapping.put(tr, traces);
			}
			traces.add(it);
			caseIDs.put(it, traceID);
			it++;
			//listTraces.add(traceLabels);
			
//			if((traces = tracesLabelsMapping.get(traceLabels))==null)
//			{
//				traces = new IntArrayList();
//				tracesLabelsMapping.put(traceLabels, traces);
//			}
//			traces.add(it);
		}
		labelMapping = inverseLabelMapping.inverse();
//		UnifiedSet<List<String>> uniqueTraceLabels = new UnifiedSet<List<String>>(listTraces);
//		for(List<String> trace : uniqueTraceLabels)
//		{
//			tr = new IntArrayList();
//			for(String eventName : trace)
//			{
//				if ((key = inverseLabelMapping.get(eventName)) == null)
//				{
//					inverseLabelMapping.put(eventName, translation);
//					labelMapping.put(translation, eventName);
//					key = translation;
//					translation++;
//				}
//				tr.add(key);
//			}
//			//if(visited.add(tr))
//			fsa.addMinWord(tr);
//			caseTracesMapping.put(tr, tracesLabelsMapping.get(trace));
////			if(!caseTracesMapping.containsKey(traceLabels))
////				caseTracesMapping.put(tr, new IntArrayList());
////			caseTracesMapping.get(tr).add(it);
//		}
		
		
//		if((traceIDs = tracesContained.get(tr))==null)
//		{
//			traceIDs = new ArrayList<String>();
//			tracesContained.put(tr, traceIDs);
//		}
//		traceIDs.add(xce.extractName(trace));
//		if(!tracesContained.containsKey(tr))
//			tracesContained.put(tr, false);
		
		//traceIDtraceName.put(it, xce.extractName(trace));
		
		//String eventName = xce.extractName(event);
		//traceList);
		
		//long automaton = System.nanoTime();
		//fsa.toDot("DAFSA_Mapping_for_Log_" + ImportLog.logName.substring(0, ImportLog.logName.length()-4) + ".dot");
		
		
		int idest=0;
		int ilabel=0;
		int initialState = 0;
		stateMapping = HashBiMap.create();
		transitionMapping = HashBiMap.create();
		finalStates = new IntHashSet(); 
		for(AbstractIntDAFSA.State n : fsa.getStates())
		{	
			if(!(n.outbound()==0 && (!fsa.isFinalState(n.getNumber()))))
			{
				if(!stateMapping.containsKey(n.getNumber()))
					stateMapping.put(n.getNumber(), new State(n.getNumber(), fsa.isSource(n.getNumber()), fsa.isFinalState(n.getNumber())));
				if(initialState !=0 && fsa.isSource(n.getNumber())){initialState = n.getNumber();}
				if(fsa.isFinalState(n.getNumber())){finalStates.add(n.getNumber());}
				for(i = 0; i < n.outbound(); i++) 
				{
					idest = AbstractIntDAFSA.decodeDest(n.next.get(i));
					ilabel = AbstractIntDAFSA.decodeLabel(n.next.get(i));
				
					if (!stateMapping.containsKey(idest))
						stateMapping.put(idest, new State(idest, fsa.isSource(idest), fsa.isFinalState(AbstractIntDAFSA.decodeDest(n.next.get(i)))));
					iTransition++;
					Transition t = new Transition(stateMapping.get(n.getNumber()), stateMapping.get(idest), ilabel);
					transitionMapping.put(iTransition, t);
					stateMapping.get(n.getNumber()).outgoingTransitions().add(t);
					stateMapping.get(idest).incomingTransitions().add(t);
				}
			}
		}
		
		Automaton logAutomaton = new Automaton(stateMapping, labelMapping, inverseLabelMapping, transitionMapping, initialState, finalStates, caseTracesMapping, caseIDs);//, concurrencyOracle);
		//long conversion = System.nanoTime();
		//System.out.println("Log Automaton creation: " + TimeUnit.MILLISECONDS.markovian((automaton - start), TimeUnit.NANOSECONDS) + "ms");
		//System.out.println("Log Automaton conversion: " + TimeUnit.MILLISECONDS.markovian((conversion - automaton), TimeUnit.NANOSECONDS) + "ms");
		return logAutomaton;
	}
}