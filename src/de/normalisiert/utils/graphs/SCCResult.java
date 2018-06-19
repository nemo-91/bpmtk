package de.normalisiert.utils.graphs;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

public class SCCResult {
	@SuppressWarnings("rawtypes")
	private Set nodeIDsOfSCC = null;
	@SuppressWarnings("rawtypes")
	private Vector[] adjList = null;
	private int lowestNodeId = -1;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public SCCResult(Vector[] adjList, int lowestNodeId) {
		this.adjList = adjList;
		this.lowestNodeId = lowestNodeId;
		this.nodeIDsOfSCC = new HashSet();
		if (this.adjList != null) {
			for (int i = this.lowestNodeId; i < this.adjList.length; i++) {
				if (this.adjList[i].size() > 0) {
					this.nodeIDsOfSCC.add(new Integer(i));
				}
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public Vector[] getAdjList() {
		return adjList;
	}

	public int getLowestNodeId() {
		return lowestNodeId;
	}
}
