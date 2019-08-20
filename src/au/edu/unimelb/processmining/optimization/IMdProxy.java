package au.edu.unimelb.processmining.optimization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import au.edu.qut.bpmn.helper.DiagramHandler;
import au.edu.qut.bpmn.helper.Petrinet2BPMNConverter;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.framework.packages.PackageManager.Canceller;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramImpl;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.bpmn.elements.Activity;
import org.processmining.models.graphbased.directed.bpmn.elements.Event;
import org.processmining.models.graphbased.directed.bpmn.elements.Flow;
import org.processmining.models.graphbased.directed.bpmn.elements.SubProcess;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.efficienttree.EfficientTreeReduce.ReductionFailedException;
import org.processmining.plugins.InductiveMiner.efficienttree.UnknownTreeNodeException;
import org.processmining.plugins.inductiveminer2.withoutlog.MiningParametersWithoutLog;
import org.processmining.plugins.inductiveminer2.withoutlog.dfgmsd.DfgMsdImpl;
import org.processmining.plugins.inductiveminer2.withoutlog.variants.MiningParametersIMWithoutLog;


public class IMdProxy {

    public static boolean debug = false;

	public BPMNDiagram discoverFromSDFG(SimpleDirectlyFollowGraph sdfg) throws UnknownTreeNodeException, ReductionFailedException {
			AcceptingPetriNet petrinet = DFG2ModelWithIMd(sdfg);
			Marking initialMarking = petrinet.getInitialMarking();
			Marking finalMarking = (new ArrayList<>(petrinet.getFinalMarkings())).get(0);
			BPMNDiagram bpmn = Petrinet2BPMNConverter.getBPMN(petrinet.getNet(), initialMarking, finalMarking);
			return updateLabels(sdfg.getSimpleLog().getEvents(), bpmn, sdfg.size()-1);
	}

	public AcceptingPetriNet DFG2ModelWithIMd(SimpleDirectlyFollowGraph sdfg)
            throws UnknownTreeNodeException, ReductionFailedException {

        String[] activities;

        if(debug) {
            activities = new String[sdfg.size()];
            for(int ai=0; ai<sdfg.size(); ai++) activities[ai] = Integer.toString(ai);
//            System.out.println("DEBUG - identified activities: " + Arrays.toString(activities));
        }

        DfgMsdImpl graph = new DfgMsdImpl();
        for (int ai = 0; ai < sdfg.size(); ai++) graph.addActivity(Integer.toString(ai));

        graph.getStartActivities().add(0);
//        System.out.println("IMd: start activities: " + graph.getStartActivities());

        graph.getEndActivities().add(sdfg.size()-1);
//        System.out.println("IMd: end activities: " + graph.getEndActivities());


        for(int src = 0; src < sdfg.size(); src++)
            for(int tgt=0; tgt < sdfg.size(); tgt++)
                if(sdfg.isEdge(src, tgt)) graph.getDirectlyFollowsGraph().addEdge(src, tgt, 1);

        MiningParametersWithoutLog parameters = new MiningParametersIMWithoutLog();
        ((MiningParametersIMWithoutLog) parameters).setDebug(false);

        return org.processmining.plugins.inductiveminer2.plugins.InductiveMinerWithoutLogPlugin.minePetriNet(graph,
                parameters, new Canceller() {
                    public boolean isCancelled() {
                        return false;
                    }
                });
	}

    private BPMNDiagram updateLabels(Map<Integer, String> events, BPMNDiagram bpmnDiagram, int endcode) {
//        this method just replace the labels of the activities in the BPMN diagram,
//        that so far have been numbers (in order to speed up the computation complexity)
        DiagramHandler helper = new DiagramHandler();
        BPMNDiagram duplicateDiagram = new BPMNDiagramImpl(bpmnDiagram.getLabel());
        HashMap<BPMNNode, BPMNNode> originalToCopy = new HashMap<>();
        BPMNNode src, tgt;
        BPMNNode copy;
        String label;
        events.put(0, "autogen-start");
        events.put(endcode, "autogen-end");

        for( BPMNNode n : bpmnDiagram.getNodes() ) {
            if( n instanceof Activity) label = events.get(Integer.valueOf(n.getLabel()));
            else if( n instanceof Event) continue;
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
