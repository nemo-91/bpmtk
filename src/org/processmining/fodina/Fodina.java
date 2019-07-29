package org.processmining.fodina;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import au.edu.qut.bpmn.helper.DiagramHandler;
import au.edu.qut.processmining.log.SimpleLog;
import au.edu.unimelb.processmining.optimization.SimpleDirectlyFollowGraph;
import com.raffaeleconforti.conversion.petrinet.PetriNetToBPMNConverter;
import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramImpl;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.bpmn.elements.Activity;
import org.processmining.models.graphbased.directed.bpmn.elements.Event;
import org.processmining.models.graphbased.directed.bpmn.elements.Flow;
import org.processmining.models.graphbased.directed.bpmn.elements.SubProcess;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.bpmnminer.converter.CausalNetToPetrinet;
import org.processmining.plugins.bpmnminer.dependencygraph.DependencyNet;
import org.processmining.plugins.bpmnminer.miner.FodinaMiner;
import org.processmining.plugins.bpmnminer.types.EventLogTaskMapper;
import org.processmining.plugins.bpmnminer.types.IntegerEventLog;
import org.processmining.plugins.bpmnminer.types.MinerSettings;

public class Fodina {

    public BPMNDiagram discoverBPMNDiagram(SimpleLog slog, MinerSettings settings) {
        return discoverFromSDFG(discoverSDFG(slog, settings), slog, settings);
    }

	public SimpleDirectlyFollowGraph discoverSDFG(SimpleLog slog, MinerSettings settings) {
		SimpleDirectlyFollowGraph sdfg;

        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {}
        }));

//		System.out.println("DEBUG - Fodina : from SLog > SDFG");

		DependencyNet dependencyGraph = getDependencyGraph(slog.getTraces(), settings);
		BitSet matrixDFG = dependencyNetToBitSet(dependencyGraph);

		sdfg = new SimpleDirectlyFollowGraph(matrixDFG, slog, dependencyGraph.getTasks().size());

        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
//        System.out.println("DEBUG - fodina, SDFG discovered.");
		return sdfg;
	}

	public BPMNDiagram discoverFromSDFG(SimpleDirectlyFollowGraph sdfg, SimpleLog slog, MinerSettings settings) {
		BPMNDiagram bpmn = null;

        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {}
        }));

//		System.out.println("DEBUG - Fodina : from SDFG > BPMN");

		DependencyNet depnet = bitSetToDependencyNet(sdfg.getMatrixDFG(), sdfg.size());
		Petrinet petrinet = (Petrinet) getPetriNet(slog.getTraces(), depnet, settings)[0];
		Marking initialMarking = PetriNetToBPMNConverter.guessInitialMarking(petrinet);
		Marking finalMarking = PetriNetToBPMNConverter.guessFinalMarking(petrinet);
		bpmn = PetriNetToBPMNConverter.convert(petrinet, initialMarking, finalMarking, false);

        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
//		System.out.println("DEBUG - fodina, BPMN discovered.");
		return updateLabels(slog.getEvents(), bpmn);
	}
	
	public IntegerEventLog simpleLogToIntegerEventLog(Map<String, Integer> simpleTraces) {
		IntegerEventLog ieLog = new IntegerEventLog();
		for (Entry<String, Integer> entry : simpleTraces.entrySet()) {
			String[] split = entry.getKey().split("::");
//			System.out.println("DEBUG - " + entry.getKey());
			int[] spliti = new int[split.length];
			for (int i = 1; i < split.length; i++) {
				spliti[i] = Integer.parseInt(split[i]);
				ieLog.setLabel(spliti[i], spliti[i] + "");
			}
			ieLog.addRow(spliti);
			ieLog.setRowCount(spliti, ieLog.getRowCount(spliti) + entry.getValue() - 1);
		}
		return ieLog;
	}

	public BitSet dependencyNetToBitSet(DependencyNet depnet) {
		int size = depnet.getTasks().size();
		BitSet dfg = new BitSet(size*size);
		for (int a : depnet.getTasks()) {
        	for (int b : depnet.getTasks()) {
        		if (!depnet.isArc(a, b)) continue;
        		int src = (a == -1) ? size - 1 : a;
        		int tgt = (b == -1) ? size - 1 : b;
        		dfg.set(src*size + tgt);
        	}
        }
        return dfg;
	}

	public DependencyNet bitSetToDependencyNet(BitSet dfg, int nrtasks) {
		DependencyNet net = new DependencyNet();
		for (int a = 0; a < nrtasks; a++) {
			for (int b = 0; b < nrtasks; b++) {
				if (!dfg.get(a*nrtasks + b)) continue;
				int src = (a == nrtasks-1) ? -1 : a;
        		int tgt = (b == nrtasks-1) ? -1 : b;
        		net.addTask(src);
        		net.addTask(tgt);
        		net.setArc(src, tgt, true);
        	}
        }
		net.setStartTask(0);
		net.setEndTask(-1);
        return net;
	}

	public DependencyNet getDependencyGraph(Map<String, Integer> simpleTraces, MinerSettings settings) {
		IntegerEventLog ieLog = simpleLogToIntegerEventLog(simpleTraces);
		return getDependencyGraph(ieLog, settings);
	}

	public DependencyNet getDependencyGraph(XLog log, MinerSettings settings) {
		EventLogTaskMapper mapper = new EventLogTaskMapper(log, settings.classifier);
		mapper.setup(settings.backwardContextSize,
				settings.forwardContextSize,
				settings.useUniqueStartEndTasks,
				settings.collapseL1l,
				settings.duplicateThreshold);
		IntegerEventLog ieLog = mapper.getIntegerLog();
		return getDependencyGraph(ieLog, settings);
	}

	public DependencyNet getDependencyGraph(IntegerEventLog ieLog, MinerSettings settings) {
		FodinaMiner miner = new FodinaMiner(ieLog, settings);
		miner.mineDependencyNet();
		DependencyNet depnet = miner.getDependencyNet();
		return depnet;
	}

	public Object[] getPetriNet(Map<String, Integer> simpleTraces, DependencyNet depnet, MinerSettings settings) {
		IntegerEventLog ieLog = simpleLogToIntegerEventLog(simpleTraces);
		return getPetriNet(ieLog, depnet, settings);
	}

	public Object[] getPetriNet(XLog log, DependencyNet depnet, MinerSettings settings) {
		EventLogTaskMapper mapper = new EventLogTaskMapper(log, settings.classifier);
		mapper.setup(settings.backwardContextSize,
				settings.forwardContextSize,
				settings.useUniqueStartEndTasks,
				settings.collapseL1l,
				settings.duplicateThreshold);
		IntegerEventLog ieLog = mapper.getIntegerLog();
		return getPetriNet(ieLog, depnet, settings);
	}

	public Object[] getPetriNet(IntegerEventLog ieLog, DependencyNet depnet, MinerSettings settings) {
		FodinaMiner miner = new FodinaMiner(ieLog, settings);
		miner.clear();
		miner.getDependencyNet().setStartTask(depnet.getStartTask());
		miner.getDependencyNet().setEndTask(depnet.getEndTask());
		for (int a : depnet.getTasks())
			for (int b : depnet.getTasks())
				miner.getDependencyNet().setArc(a, b, depnet.isArc(a, b));
		miner.mineCausalNet(false);
		return CausalNetToPetrinet.toPetrinet(miner.getCausalNet());
	}

	private BPMNDiagram updateLabels(Map<Integer, String> events, BPMNDiagram bpmnDiagram) {
//        this method just replace the labels of the activities in the BPMN diagram,
//        that so far have been numbers (in order to speed up the computation complexity)
		DiagramHandler helper = new DiagramHandler();
		BPMNDiagram duplicateDiagram = new BPMNDiagramImpl(bpmnDiagram.getLabel());
		HashMap<BPMNNode, BPMNNode> originalToCopy = new HashMap<>();
		BPMNNode src, tgt;
		BPMNNode copy;
		String label;

		for( BPMNNode n : bpmnDiagram.getNodes() ) {
			if( n instanceof Activity) label = events.get(Integer.valueOf(n.getLabel()));
			else if( n instanceof Event ) continue;
			else label = "";

			if(label.equalsIgnoreCase("autogen-start")) {
                copy = duplicateDiagram.addEvent( "",
                        Event.EventType.START,
                        Event.EventTrigger.NONE,
                        Event.EventUse.CATCH,
                        (SubProcess) null,
                        true,
                        null);
            } else if(label.equalsIgnoreCase("autogen-end")) {
                copy = duplicateDiagram.addEvent( "",
                        Event.EventType.END,
                        Event.EventTrigger.NONE,
                        Event.EventUse.THROW,
                        (SubProcess) null,
                        true,
                        null);
            } else copy = helper.copyNode(duplicateDiagram, n, label);

			if( copy != null ) originalToCopy.put(n, copy);
			else System.out.println("ERROR - diagram labels updating failed [1].");
		}

		for( Flow f : bpmnDiagram.getFlows() ) {
		    if(f.getSource() instanceof Event || f.getTarget() instanceof Event) continue;
			src = originalToCopy.get(f.getSource());
			tgt = originalToCopy.get(f.getTarget());

			if( src != null && tgt != null ) duplicateDiagram.addFlow(src, tgt, "");
			else System.out.println("ERROR - diagram labels updating failed [2].");
		}

		return duplicateDiagram;
	}

}
