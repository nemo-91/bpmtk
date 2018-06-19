package de.drscc.automaton;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

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

public class State {
	public static int UNIQUE_ID = 0;
	private int id = UNIQUE_ID++;
	private String label;
	private Set<Transition> outgoingTransitions;
	private Set<Transition> incomingTransitions;
	private boolean isSource;
	private boolean isFinal;
	private int component = -1;
	public boolean isLoopState = false;
	private IntHashSet loops;
	@SuppressWarnings("unused")
	private boolean explore = true;
	private boolean hasLoopFuture = false;
	private UnifiedSet<IntIntHashMap> futureLoops;
	private Set<IntIntHashMap> loopLabels;
	private Set<IntIntHashMap> possibleFutures;
	private Map<IntIntHashMap, Map<IntArrayList, IntArrayList>> potentialPathsAndTraceLabels;
	//private	Map<IntArrayList, IntArrayList> traceLabels;
	
	public State(int id, boolean isSource, boolean isFinal)
	{
//		if (!(id>=0)) {return;}
		this.id = id;
		this.label = "" + id;
		this.isSource = isSource;
		this.isFinal = isFinal;
		
	}
	
	public State(String label, boolean isSource, boolean isFinal)
	{
		this.label = label;
		if(this.label.contains("<html>") && this.label.contains("</html>"))
			this.label = this.label.substring(6, this.label.length()-7);
		this.isSource = isSource;
		this.isFinal = isFinal;
		
	}
	
	public int id()
	{
		return this.id;
	}
	
	public String label()
	{
		return this.label;
	}
	
	public Set<Transition> outgoingTransitions()
	{
		if (this.outgoingTransitions == null){this.outgoingTransitions = new UnifiedSet<Transition>();}
		return this.outgoingTransitions;
	}
	
	public Set<Transition> incomingTransitions()
	{
		if (this.incomingTransitions == null){this.incomingTransitions = new UnifiedSet<Transition>();}
		return this.incomingTransitions;
	}
	
	public boolean isSource()
	{
		return this.isSource;
	}
	
	public boolean isFinal()
	{
		return this.isFinal;
	}
	
	public Set<IntIntHashMap> possibleFutures()
	{
		if(this.possibleFutures == null)
			this.possibleFutures = new UnifiedSet<IntIntHashMap>();
		return this.possibleFutures;
	}
	
	public Map<IntIntHashMap, Map<IntArrayList, IntArrayList>> potentialPathsAndTraceLabels()
	{
		if(!this.isFinal())
			return null;
		if(this.potentialPathsAndTraceLabels == null)
			this.potentialPathsAndTraceLabels = new HashMap<IntIntHashMap, Map<IntArrayList, IntArrayList>>();
		return this.potentialPathsAndTraceLabels;
	}
	
	public void setComponent(int component)
	{
		this.component = component;
	}
	
	public int component()
	{
		return this.component;
	}
	
	public boolean explore()
	{
		for(Transition tr : this.outgoingTransitions())
		{
			if(tr.explore) return false;
		}
		return true;
	}
	
	public Set<IntIntHashMap> loopLabels()
	{
		if(this.loopLabels==null)
			this.loopLabels = new UnifiedSet<IntIntHashMap>();
		return this.loopLabels;
	}
	
	public IntHashSet loops()
	{
		if(this.loops==null)
				this.loops = new IntHashSet();
		return this.loops;
	}
	
	public UnifiedSet<IntIntHashMap> futureLoops()
	{
		if(this.futureLoops==null)
		{
			this.futureLoops = new UnifiedSet<IntIntHashMap>();
			this.hasLoopFuture=true;
		}
		return this.futureLoops;
	}
	
	public boolean hasLoopFuture()
	{
		return this.hasLoopFuture;
	}
	
//	public Map<IntArrayList, IntArrayList> traceLabels()
//	{
//		if(!this.isFinal())	return null;
//		if(this.traceLabels==null)
//			this.traceLabels = new HashMap<IntArrayList, IntArrayList>();
//		return this.traceLabels;
//	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        State state = (State) o;

//        return new EqualsBuilder()
//        		.append(this.id(), state.id())
//        		.append(this.EID(), state.EID())
//        		.append(this.outgoingTransitions(), state.outgoingTransitions())
//        		.append(this.isSource(), state.isSource())
//        		.append(this.isFinal(), state.isFinal())
//        		.isEquals();
        return this.id() == state.id();
    }

    @Override
    public int hashCode() {
//        return new HashCodeBuilder(17,37)
//        		.append(this.id())
//        		.append(this.EID())
//        		.append(this.isSource())
//        		.append(this.isFinal())
//        		.toHashCode();
    	return this.id();
    }
}
