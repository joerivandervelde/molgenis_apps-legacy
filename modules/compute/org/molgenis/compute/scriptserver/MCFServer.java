package org.molgenis.compute.scriptserver;

import org.gridgain.grid.Grid;
import org.gridgain.grid.GridNode;
import org.molgenis.compute.grid.GridStarter;
import org.molgenis.compute.pipelinemodel.Pipeline;
import org.molgenis.compute.resourcemanager.ResourceManager;
import org.molgenis.util.Ssh;
import org.molgenis.util.SshResult;


import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

/**
 * Created by IntelliJ IDEA.
 * User: georgebyelas
 * Date: Nov 18, 2010
 * Time: 9:21:22 AM
 * To change this template use File | Settings | File Templates.
 */

public class MCFServer implements MCF
{
    private Grid grid = null;
    private ResourceManager resourceManager = null;

    private Vector<Pipeline> pipelines = new Vector<Pipeline>();
    private Vector<Pipeline> archivePipelines = new Vector<Pipeline>();

    public MCFServer()
    {
        start();
    }

    public MCFServer(Grid grid)
    {
        this.grid = grid;
    }

    public void setPipeline(Pipeline pipeline)
    {
        //maybe temporary
        boolean remoteNodeExists = false;

        while (!remoteNodeExists)
        {
            Collection<GridNode> remoteNodes = grid.getRemoteNodes();

            if (remoteNodes.size() > 0)
            {
                remoteNodeExists = true;
            }
            else
            {
                System.out.println(">>> wait for remote node");
                try
                {
                    Thread.sleep(10000);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
        System.out.println(">>> start pipeline execution");


        pipelines.add(pipeline);

        //todo logging properly!!!
        PipelineThread pipelineThread = new PipelineThread(pipeline, grid);
        new PipelineExecutor().execute(pipelineThread);
    }

    public Pipeline getPipeline(String id)
    {
        for (int i = 0; i < pipelines.size(); i++)
        {
            String cID = pipelines.elementAt(i).getId();
            if (cID.equalsIgnoreCase(id))
                return pipelines.elementAt(i);
        }

        return null;
    }

    public Pipeline getArchivePipeline(int i)
    {
        return archivePipelines.elementAt(i);
    }

    public int getNumberActivePipelines()
    {
        Iterator it = pipelines.iterator();
        while (it.hasNext())
        {
            Pipeline p = (Pipeline) it.next();
            if (p.isFinished())
            {
                archivePipelines.addElement(p);
                pipelines.remove(p);
            }
        }

        return pipelines.size();
    }

    public Pipeline getActivePipeline(int i)
    {
        return pipelines.elementAt(i);
    }

    public int getNumberFinishedPipelines()
    {
        return archivePipelines.size();
    }

    public void start()
    {
        //start gridgain
        GridStarter gridStarter = new GridStarter();
        grid = gridStarter.startGrid();

        //start resource manager
        resourceManager = new ResourceManager();
        resourceManager.setGrid(grid);
        resourceManager.setSettings(8, 10);

        startRemoteNode();

    }

    private void startRemoteNode()
    {
        System.out.println("start remote node");

        try
        {
            Ssh ssh = new Ssh("millipede.service.rug.nl", "happy","chappy");
            SshResult result = ssh.executeCommand("qsub -q short /data/byelas/scripts/gridgain/manual_worker_script.sh");
            System.out.println("err: " + result.getStdErr());
            System.out.println("out: " + result.getStdOut());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void removePipeline(String id)
    {

    }

    public ExecutorService getExecutor()
    {
        return grid.newGridExecutorService();
    }


}
