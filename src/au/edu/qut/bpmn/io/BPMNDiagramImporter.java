package au.edu.qut.bpmn.io;

import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;

/**
 * Created by Adriano on 29/10/2015.
 */
public interface BPMNDiagramImporter {

    BPMNDiagram importBPMNDiagram(String BPMNModelPath) throws Exception;
}
