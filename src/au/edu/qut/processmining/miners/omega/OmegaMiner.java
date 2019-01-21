package au.edu.qut.processmining.miners.omega;

import au.edu.qut.processmining.log.LogParser;
import au.edu.qut.processmining.log.SimpleLog;
import au.edu.qut.processmining.miners.splitminer.SplitMiner;
import au.edu.qut.processmining.miners.splitminer.dfgp.DirectlyFollowGraphPlus;
import au.edu.qut.processmining.miners.splitminer.ui.dfgp.DFGPUIResult;
import au.edu.unimelb.processmining.accuracy.abstraction.LogAbstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.ProcessAbstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.intermediate.AutomatonAbstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.subtrace.Subtrace;
import au.edu.unimelb.processmining.accuracy.abstraction.subtrace.SubtraceAbstraction;
import com.raffaeleconforti.conversion.bpmn.BPMNToPetriNetConverter;
import com.raffaeleconforti.conversion.petrinet.PetriNetToBPMNConverter;
import com.raffaeleconforti.log.util.LogImporter;
import de.drscc.automaton.Automaton;
import de.drscc.importer.ImportProcessModel;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIContext;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.bpmn.plugins.BpmnExportPlugin;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class OmegaMiner {

    private static int ORDER = 6;
    private static double DELTA = 0.05;

    private SplitMiner yam;
    private ImportProcessModel importer;

    private SimpleLog slog;
    private DirectlyFollowGraphPlus dfgp;
    private BPMNDiagram bpmn;
    private Object[] objects;

    private SubtraceAbstraction staLog;
    private SubtraceAbstraction staProcess;

    public OmegaMiner() {
        yam = new SplitMiner();
        importer = new ImportProcessModel();
    }

    public void mineAndExport(String logPath) {
        XLog xlog;
        try {
            xlog = LogImporter.importFromFile(new XFactoryNaiveImpl(), logPath);
        } catch (Exception e) {
            System.out.println("ERROR - impossible to load the log");
            return;
        }

        this.mine(xlog);
        exportBPMN(bpmn, ".\\om-bpmn_" + System.currentTimeMillis() + ".bpmn");
    }

    public BPMNDiagram mine(XLog xlog) {
        Set<Subtrace> subtraces;
        Set<String> subtracesAsStrings = new HashSet<>();

        double fitness;
        double precision;

        int enhancement;
        int reduction;
        long eTime;

        slog = LogParser.getSimpleLog(xlog, new XEventNameClassifier());
        staLog = LogAbstraction.subtrace(slog, ORDER);

        dfgp = new DirectlyFollowGraphPlus(slog, 1.00, 0.50, DFGPUIResult.FilterType.WTH, false);
        dfgp.buildDFGP();
        bpmn = yam.discoverFromDFGP(dfgp);
        staProcess = abstractProcessBehaviour(bpmn);

        try {
            fitness = staLog.minus(staProcess);
            precision = staProcess.minus(staLog);

            eTime = System.currentTimeMillis();
            System.out.println("INFO - fit & prec : " + fitness + " & " + precision);
            while (System.currentTimeMillis() - eTime < 300000) {
                if (precision > fitness + DELTA) {
//            unfitting subtraces detection and enhancement of the dfgp
                    subtracesAsStrings = staLog.getDifferences(staProcess, 1);
                    enhancement = dfgp.enhance(subtracesAsStrings);
                    if( enhancement == 0 ) break;
                    System.out.println("INFO - enhancement: +" + enhancement);
//            computing accuracy after enhancement
                    bpmn = yam.discoverFromDFGP(dfgp);
                    staProcess = abstractProcessBehaviour(bpmn);
                    fitness = staLog.minus(staProcess);
                    precision = staProcess.minus(staLog);
                    System.out.println("INFO - fit & prec : " + fitness + " & " + precision);
                } else if (fitness > precision + DELTA) {
//            unprecise subtraces detection and reduction of the dfgp
                    subtracesAsStrings = staProcess.getDifferences(staLog, 1);
                    reduction = dfgp.reduce(subtracesAsStrings);
                    if( reduction == 0 ) break;
                    System.out.println("INFO - reduction: -" + reduction);
//            computing accuracy after enhancement
                    bpmn = yam.discoverFromDFGP(dfgp);
                    staProcess = abstractProcessBehaviour(bpmn);
                    fitness = staLog.minus(staProcess);
                    precision = staProcess.minus(staLog);
                    System.out.println("INFO - fit & prec : " + fitness + " & " + precision);
                } else return bpmn;
            }
        } catch (Exception e){
            return bpmn;
        }
        return bpmn;
    }

    private SubtraceAbstraction abstractProcessBehaviour(BPMNDiagram diagram) {

        objects = BPMNToPetriNetConverter.convert(diagram);
        if(objects[1] == null) objects[1] = PetriNetToBPMNConverter.guessInitialMarking((Petrinet) objects[0]);

//        if(objects[1] == null) objects[1] = MarkingDiscoverer.constructInitialMarking(context, (Petrinet) objects[0]);
//        else MarkingDiscoverer.createInitialMarkingConnection(context, (Petrinet) objects[0], (Marking) objects[1]);

        try {
            Automaton automaton = importer.createFSMfromPetrinet((Petrinet) objects[0], (Marking) objects[1], null, null);
            AutomatonAbstraction automatonAbstraction = new AutomatonAbstraction(automaton, slog);

            return (new ProcessAbstraction(automatonAbstraction)).subtrace(ORDER);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR - impossible to parse the process object.");
            return null;
        }
    }

    private void exportBPMN(BPMNDiagram diagram, String path) {
        BpmnExportPlugin bpmnExportPlugin = new BpmnExportPlugin();
        UIContext context = new UIContext();
        UIPluginContext uiPluginContext = context.getMainPluginContext();
        try {
            bpmnExportPlugin.export(uiPluginContext, diagram, new File(path));
        } catch (Exception e) { System.out.println("ERROR - impossible to export the BPMN"); }
    }

}
