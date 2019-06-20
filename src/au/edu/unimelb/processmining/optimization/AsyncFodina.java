package au.edu.unimelb.processmining.optimization;

import au.edu.qut.processmining.log.SimpleLog;
import org.processmining.fodina.Fodina;
import org.processmining.plugins.bpmnminer.types.MinerSettings;

import java.util.concurrent.Callable;

public class AsyncFodina implements Callable<Object[]> {

    private SimpleLog slog;
    private MinerSettings fodinaSettings;
    //    private SimpleDirectlyFollowGraph sdfg;

    public AsyncFodina(SimpleLog slog, double param0, double param1) {
        this.slog = slog;
        this.fodinaSettings = new MinerSettings();
        this.fodinaSettings.dependencyThreshold = param0;
        this.fodinaSettings.l1lThreshold = param1;
        this.fodinaSettings.l2lThreshold = param1;
    }

    @Override
    public Object[] call() throws Exception {
        Object[] result = new Object[1];
        Fodina fodina = new Fodina();

        try {
            result[0] = fodina.discoverSDFG(slog, fodinaSettings);
            System.out.println("THREAD - 4 fodina, done.");
        } catch(Exception e) {
            result[0] = null;
        } catch(Error e) {
            result[0] = null;
        }
        return result;
    }
}
