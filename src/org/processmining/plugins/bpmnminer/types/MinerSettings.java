package org.processmining.plugins.bpmnminer.types;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.info.impl.XLogInfoImpl;

public class MinerSettings {
	public final static int DANGLING_PATTERN_ADD_XOR = 1;
	public final static int DANGLING_PATTERN_ADD_AND = 2;
	public final static int DANGLING_PATTERN_IGNORE = 3;
	
	public MinerSettings() {

	}
	
	public MinerSettings(int logSize) {
		double nom = logSize / ((double)logSize + (double)dependencyDivisor);
		if (nom <= 0D) nom = 0D;
		if (nom >= 0.9D) nom = 0.9D;
		dependencyThreshold = nom;
		l1lThreshold = nom;
		l2lThreshold = nom;
	}
	
	public XEventClassifier classifier = XLogInfoImpl.NAME_CLASSIFIER;
	
	public double dependencyThreshold = 0.90;		// [0, 1]
	public double l1lThreshold = 0.90;				// [0, 1]
	public double l2lThreshold = 0.90;				// [0, 1]
	public double longDistanceThreshold = 0.90;		// [0, 1]
	public int dependencyDivisor = 1;				// [0, n]
	public double causalityStrength = 0.80;			// [0, 1]
	public double duplicateThreshold = 0.10;		// [0, 1]

	public double patternThreshold = 0D;			// [-1, 1]
	
	public boolean useAllConnectedHeuristics = true;
	public boolean useOnlyNormalDependenciesForConnecting = false;
	public boolean useLongDistanceDependency = false;
	public boolean useUniqueStartEndTasks = true;

	public boolean collapseL1l = true;
	public boolean preferAndToL2l = false;
	public boolean preventL2lWithL1l = true;

	public int backwardContextSize = 0;				// Set to 1 to mine duplicates
	public int forwardContextSize = 0;				// Set to 1 to mine duplicates
	
	public boolean suppressFitnessReport = true;
	
	public int danglingPatternStrategy = DANGLING_PATTERN_ADD_XOR;
	
	public String organizationalField = XOrganizationalExtension.KEY_RESOURCE;
	
}
