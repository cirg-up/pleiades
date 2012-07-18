/**
 * Pleiades
 * Copyright (C) 2011 - 2012
 * Computational Intelligence Research Group (CIRG@UP)
 * Department of Computer Science
 * University of Pretoria
 * South Africa
 */
package net.pleiades.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import net.pleiades.simulations.Simulation;

/**
 *
 * @author bennie
 */
public class CilibXMLTask implements Task, Serializable {
    private String cilibInput;
    private String id;
    private String results;
    private String progress;
    private Simulation parent;

    private CilibXMLTask(String cilibInput, String id, Simulation parent) {
        this.cilibInput = cilibInput;
        this.id = id;
        this.parent = parent;
    }

    public static CilibXMLTask of(String cilibInput, String id, Simulation parent) {
        return new CilibXMLTask(cilibInput, id, parent);
    }

    public static CilibXMLTask assignNewIdTo(CilibXMLTask task, String id) {
        return new CilibXMLTask(task.cilibInput, id, task.parent);
    }

    @Override
    public String execute(Properties p) {
        results = new String();

        String line;
        InputStream inputStream;
        BufferedReader reader;

        try {
            List<String> command = new LinkedList<String>();
            String c = p.getProperty("java_exec_command");
            StringTokenizer tokens = new StringTokenizer(c);
            while (tokens.hasMoreTokens()) {
                command.add(tokens.nextToken()
                        .replaceAll("\\$jar", id + ".run")
                        .replaceAll("\\$file", id));
            }

            Process shell = new ProcessBuilder(command).start();
            inputStream = shell.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));

            while ((line = reader.readLine()) != null) {
                progress = line.replaceAll("Progress ", "");
            }

            int exitValue = shell.waitFor();
            
            if (exitValue == 0) {
                File r = new File("pleiades/" + id + ".pleiades");
                InputStream rIn = new FileInputStream(r);
                results = convertStreamToStr(rIn);
                r.delete();
            } else {
                InputStream eIn = shell.getErrorStream();
                String error = convertStreamToStr(eIn);
                FileWriter writer = new FileWriter(new File(id + ".log"));
                writer.append(error);
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return results;
    }

    @Override
    public void writeFile() {
        try {
            File f = new File(id);
            f.deleteOnExit();

            FileWriter fileWriter = new FileWriter(f);

            fileWriter.write(cilibInput.toString().replaceAll("file=\"pleiades/.*pleiades", "file=\"pleiades/" + id + ".pleiades"));
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteFile() {
        File f = new File(id);
        f.delete();
    }

    @Override
    public String getResults() {
        return results;
    }

    @Override
    public String getProgress() {
        return progress;
    }

    @Override
    public Simulation getParent() {
        return parent;
    }

    @Override
    public String getId() {
        return id;
    }


    public static String convertStreamToStr(InputStream is) throws IOException {
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is,"UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }
}