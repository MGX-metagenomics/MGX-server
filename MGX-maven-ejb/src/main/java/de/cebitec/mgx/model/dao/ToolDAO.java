package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.db.Tool;
import de.cebitec.mgx.util.UnixHelper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 *
 * @author sjaenick
 */
public class ToolDAO<T extends Tool> extends DAO<T> {

    @Override
    Class getType() {
        return Tool.class;
    }

    @Override
    public long create(T obj) throws MGXException {
        // extract xml data
        String xmldata = obj.getXMLFile();
        obj.setXMLFile(null);

        Long id = super.create(obj);

        File jobDir = new File(getController().getProjectDirectory() + "jobs");
        if (!jobDir.exists()) {
            UnixHelper.createDirectory(jobDir);
        }

        String fname = getController().getProjectDirectory() + "jobs" + File.separator + id.toString() + ".xml";
        try {
            try (Writer fw = new BufferedWriter(new FileWriter(fname))) {
                fw.write(xmldata);
                fw.flush();
            }
            UnixHelper.makeFileGroupWritable(fname);
        } catch (Exception ex) {
            delete(id); // remove from database, aka rollback
            throw new MGXException(ex.getMessage());
        }

        obj.setXMLFile(fname);
        super.update(obj);

        return id;
    }

    public <U extends T> long installGlobalTool(Tool global, Class<U> clazz, String projectDir) throws MGXException {

        U t;
        try {
            t = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new MGXException(ex.getMessage());
        }

        // manual clone
        t.setName(global.getName());
        t.setDescription(global.getDescription());
        t.setVersion(global.getVersion());
        t.setAuthor(global.getAuthor());
        t.setUrl(global.getUrl());
        t.setXMLFile(global.getXMLFile());

        // persist to create the id
        long id = super.create(t);

        /*
         * copy over the graph description file in order to maintain a
         * consistent project state even if the global tool is updated; the
         * project directory is created on demand
         */
        StringBuilder targetName = new StringBuilder(projectDir).append("jobs");
        File targetDir = new File(targetName.toString());
        if (!targetDir.exists()) {
            UnixHelper.createDirectory(targetDir);
        }

        targetName.append(File.separator).append(id).append(".xml");

        try {
            File src = new File(global.getXMLFile());
            File dest = new File(targetName.toString());
            UnixHelper.copyFile(src, dest);
            UnixHelper.makeFileGroupWritable(dest.getAbsolutePath());
        } catch (IOException ex) {
            throw new MGXException(ex.getMessage());
        }

        // update graph description
        t.setXMLFile(targetName.toString());
        update(t);

        return id;
    }

    @Override
    public void delete(long id) throws MGXException {
        // remove the local copy of the graph definition before
        // deleting the instance in the database
        T t = getById(id);
        File graph = new File(t.getXMLFile());
        if (graph.exists()) {
            graph.delete();
        }
        super.delete(id);
    }
}
