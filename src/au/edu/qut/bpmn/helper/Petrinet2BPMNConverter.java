package au.edu.qut.bpmn.helper;

import au.edu.unimelb.processmining.optimization.AutomatedProcessDiscoveryOptimizer;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramImpl;
import org.processmining.models.graphbased.directed.bpmn.BPMNEdge;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.bpmn.elements.Activity;
import org.processmining.models.graphbased.directed.bpmn.elements.Event;
import org.processmining.models.graphbased.directed.bpmn.elements.Gateway;
import org.processmining.models.graphbased.directed.bpmn.elements.SubProcess;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.kutoolbox.utils.FakePluginContext;
import org.processmining.plugins.pnml.importing.PnmlImportNet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Petrinet2BPMNConverter {

    public static void main(String[] args) {
        BPMNDiagram diagram;

        try {
            PnmlImportNet imp = new PnmlImportNet();
            Object[] object = (Object[]) imp.importFile(new FakePluginContext(), "test.pnml");
            Petrinet pnet = (Petrinet) object[0];

            diagram = getBPMN(pnet);
            AutomatedProcessDiscoveryOptimizer.exportBPMN(diagram, ".\\conversion-test.bpmn");

        } catch(Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public static BPMNDiagram getBPMN(Petrinet pnet) {
        BPMNDiagram bpmn = new BPMNDiagramImpl("BPMN from PN");
        Event event;
        Activity activity;
        Gateway gateway;


//        Map<Transition, BPMNNode> transitionsToBPMNNodes = new HashMap<>();
//        Map<Place, BPMNNode> placesToBPMNNodes = new HashMap<>();
        Map<PetrinetNode, BPMNNode> PNnodesBPMNnodes = new HashMap<>();

        int sources = 0;
        int sinks = 0;
        for( Place p : pnet.getPlaces() ) {
            if (pnet.getInEdges(p).size() == 0) {
                sources++;
                event = bpmn.addEvent( "start", Event.EventType.START, Event.EventTrigger.NONE, Event.EventUse.CATCH, (SubProcess) null, true,null);
//                placesToBPMNNodes.put(p,event);
                PNnodesBPMNnodes.put(p, event);
            } else if (pnet.getOutEdges(p).size() == 0) {
                sinks++;
                event = bpmn.addEvent( "end", Event.EventType.END, Event.EventTrigger.NONE, Event.EventUse.THROW, (SubProcess) null, true,null);
//                placesToBPMNNodes.put(p,event);
                PNnodesBPMNnodes.put(p, event);
            } else {
                gateway = bpmn.addGateway("tau", Gateway.GatewayType.DATABASED);
//                placesToBPMNNodes.put(p,gateway);
                PNnodesBPMNnodes.put(p, gateway);
            }
        }

        if( sources != 1 || sinks != 1 ) {
            System.out.println("WARNING - the petrinet contains multiple sources and sinks places: " + sources + " - "+ sinks);
        }

        for(Transition t : pnet.getTransitions()) {
            if( t.isInvisible() || t.getLabel().contains("tau") ) {
                gateway = bpmn.addGateway("tau", Gateway.GatewayType.PARALLEL);
//                transitionsToBPMNNodes.put(t, gateway);
                PNnodesBPMNnodes.put(t, gateway);
            } else {
                activity = bpmn.addActivity(t.getLabel(), false, false, false, false, false, (SubProcess) null);
//                transitionsToBPMNNodes.put(t, activity);
                PNnodesBPMNnodes.put(t, activity);
            }
        }

        PetrinetNode src, tgt;
        for( PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> e : pnet.getEdges() ) {
            src = e.getSource();
            tgt = e.getTarget();
            bpmn.addFlow(PNnodesBPMNnodes.get(src), PNnodesBPMNnodes.get(tgt), "");
        }

        BPMNEdge<? extends BPMNNode, ? extends BPMNNode> in;
        BPMNEdge<? extends BPMNNode, ? extends BPMNNode> out;
        for( Gateway g : new ArrayList<>(bpmn.getGateways()) )
            if( bpmn.getInEdges(g).size() == 1 && bpmn.getOutEdges(g).size() == 1 ) {
                in = new ArrayList<>(bpmn.getInEdges(g)).get(0);
                out = new ArrayList<>(bpmn.getOutEdges(g)).get(0);

                bpmn.addFlow(in.getSource(), out.getTarget(), "");
                bpmn.removeEdge(in);
                bpmn.removeEdge(out);
                bpmn.removeNode(g);
            }

        return bpmn;
    }
}
