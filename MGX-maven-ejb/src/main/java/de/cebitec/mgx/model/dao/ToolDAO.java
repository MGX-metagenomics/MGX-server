package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.controller.MGXException;
import de.cebitec.mgx.model.db.Tool;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author sjaenick
 */
public class ToolDAO<T extends Tool> extends DAO<T> {

    @Override
    Class getType() {
        return Tool.class;
    }

    public <U extends T> long installGlobalTool(Tool global, Class<U> clazz, String projectDir) throws MGXException {
        
        U t;
        try {
            t = clazz.newInstance();
        } catch (Exception ex) {
            throw new MGXException(ex.getMessage());
        }
        
        t.setName(global.getName());
        t.setDescription(global.getDescription());
        t.setVersion(global.getVersion());
        t.setAuthor(global.getAuthor());
        t.setUrl(global.getUrl());
        t.setXMLFile(global.getXMLFile());

        // persist to create the id
        long id = create(t);
        
        /*
         * copy over the graph description file in order to maintain a
         * consistent project state even if the global tool is updated;
         * the project directory is created on demand
         */
        StringBuilder targetName = new StringBuilder(projectDir)
                .append(File.separator)
                .append("jobs");
        new File(targetName.toString()).mkdirs();
        
        targetName.append(File.separator)
                .append(id)
                .append(".xml");
        
        try {
            File src = new File(global.getXMLFile());
            File dest = new File(targetName.toString());
            copyFile(src, dest);
        } catch (IOException ex) {
            throw new MGXException(ex.getMessage());
        }
        
        // update graph description
        t.setXMLFile(targetName.toString());
        update(t);

        return id;
    }
    
    @Override
    public void delete(Long id) throws MGXException {
        // remove the local copy of the graph definition before
        // deleting the instance in the database
        T t = getById(id);
        File graph = new File(t.getXMLFile());
        if (graph.exists())
            graph.delete();
        super.delete(id);
    }
}
