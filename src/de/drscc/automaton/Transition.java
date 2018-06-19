package de.drscc.automaton;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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

public class Transition {
	
	private static int UNIQUE_ID = 0;
	private int id = UNIQUE_ID++;
	public boolean explore = false;
	private State source;
	private State target;
	private int eventID;
	
	public Transition(State state, State state2, int eventID)
	{
		this.source = state;
		this.target = state2;
		this.eventID = eventID;
	}

	public State source()
	{
		return this.source;
	}
	
	public State target()
	{
		return this.target;
	}
	
	public int eventID()
	{
		return this.eventID;
	}
	
	public int id()
	{
		return this.id;
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Transition transition = (Transition) o;

        return new EqualsBuilder()
        		.append(this.source().id(), transition.source().id())
        		.append(this.target().id(), transition.target().id())
        		.append(this.eventID(), transition.eventID())
        		.isEquals();
        //return this.id() == transition.id();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17,37)
        		.append(this.source().id())
        		.append(this.target().id())
        		.append(this.eventID())
        		.toHashCode();
//    	return this.id;
    }
}
