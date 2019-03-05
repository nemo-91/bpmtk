package au.edu.qut.bpmn.io.impl;

import au.edu.qut.bpmn.io.BPMNDiagramImporter;
import com.raffaeleconforti.context.FakePluginContext;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramFactory;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.bpmn.elements.Swimlane;
import org.processmining.plugins.bpmn.Bpmn;
import org.processmining.plugins.bpmn.dialogs.BpmnSelectDiagramDialog;
import org.processmining.plugins.bpmn.parameters.BpmnSelectDiagramParameters;
import org.processmining.plugins.bpmn.plugins.BpmnImportPlugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BPMNDiagramImporterImpl implements BPMNDiagramImporter {

    public BPMNDiagramImporterImpl() {}

    @Override
    public BPMNDiagram importBPMNDiagram(String BPMNModelPath) throws Exception {
        FakePluginContext context = new FakePluginContext();
        Bpmn bpmn = (Bpmn) new BpmnImportPlugin().importFile(context, BPMNModelPath);
        BpmnSelectDiagramParameters parameters = new BpmnSelectDiagramParameters();
        @SuppressWarnings("unused")
        BpmnSelectDiagramDialog dialog = new BpmnSelectDiagramDialog(bpmn.getDiagrams(), parameters);
        BPMNDiagram bpmnDiagram = BPMNDiagramFactory.newBPMNDiagram("");String result;
        Map<String, BPMNNode> id2node = new HashMap<String, BPMNNode>();
        Map<String, Swimlane> id2lane = new HashMap<String, Swimlane>();
        if (parameters.getDiagram() == BpmnSelectDiagramParameters.NODIAGRAM) {
            bpmn.unmarshall(bpmnDiagram, id2node, id2lane);
        } else {
            Collection<String> elements = parameters.getDiagram().getElements();
            bpmn.unmarshall(bpmnDiagram, elements, id2node, id2lane);
        }

        return bpmnDiagram;
    }
}
