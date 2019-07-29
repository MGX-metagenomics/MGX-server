package de.cebitec.mgx.model.dao;

import de.cebitec.mgx.common.ToolScope;
import de.cebitec.mgx.controller.MGXController;
import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.core.TaskI;
import de.cebitec.mgx.model.db.Job;
import de.cebitec.mgx.model.db.Tool;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import de.cebitec.mgx.util.UnixHelper;
import de.cebitec.mgx.workers.DeleteTool;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public class ToolDAO extends DAO<Tool> {

    public ToolDAO(MGXController ctx) {
        super(ctx);
    }

    @Override
    Class getType() {
        return Tool.class;
    }

    private final static String CREATE = "INSERT INTO tool (author, description, name, url, version, file, scope) "
            + "VALUES (?,?,?,?,?,?,?) RETURNING id";

    @Override
    public long create(Tool obj) throws MGXException {
        // extract xml data
        String xmldata = obj.getFile();
        obj.setFile(null);

//        Long id = super.create(obj);
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(CREATE)) {
                stmt.setString(1, obj.getAuthor());
                stmt.setString(2, obj.getDescription());
                stmt.setString(3, obj.getName());
                stmt.setString(4, obj.getUrl());
                stmt.setFloat(5, obj.getVersion());
                stmt.setString(6, obj.getFile());
                stmt.setInt(7, obj.getScope().getValue());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        obj.setId(rs.getLong(1));
                    }
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }

        String fname = null;
        try {
            fname = getController().getProjectJobDirectory() + File.separator + obj.getId() + ".xml";
            try (Writer fw = new BufferedWriter(new FileWriter(fname))) {
                fw.write(xmldata);
                fw.flush();
            }
            UnixHelper.makeFileGroupWritable(fname);
        } catch (IOException ex) {

            // remove entity from DB
            try (Connection conn = getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM tool WHERE id=?")) {
                    stmt.setLong(1, obj.getId());
                    stmt.executeUpdate();
                }
            } catch (SQLException ex1) {
                Logger.getLogger(ToolDAO.class.getName()).log(Level.SEVERE, null, ex1);
            }

            File f = new File(fname);
            if (f.exists()) {
                f.delete();
            }
            throw new MGXException(ex.getMessage());
        }

        obj.setFile(fname);
        update(obj);

        return obj.getId();
    }

    @Override
    public Tool getById(long id) throws MGXException {
        if (id <= 0) {
            throw new MGXException("No/Invalid ID supplied.");
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id, author, description, name, url, version, file, scope FROM tool WHERE id=?")) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new MGXException("No object of type " + getClassName() + " for ID " + id + ".");
                    }
                    Tool t = new Tool();
                    t.setId(rs.getLong(1));
                    t.setAuthor(rs.getString(2));
                    t.setDescription(rs.getString(3));
                    t.setName(rs.getString(4));
                    t.setUrl(rs.getString(5));
                    t.setVersion(rs.getFloat(6));
                    t.setFile(rs.getString(7));
                    t.setScope(ToolScope.values()[rs.getInt(8)]);

                    return t;
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }

    public AutoCloseableIterator<Tool> getAll() throws MGXException {
        List<Tool> tools = getTools();
        return new ForwardingIterator<>(tools.iterator());
    }

    private List<Tool> getTools() throws MGXException {
        List<Tool> tools = new ArrayList<>();
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id, author, description, name, url, version, file, scope FROM tool")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Tool t = new Tool();
                        t.setId(rs.getLong(1));
                        t.setAuthor(rs.getString(2));
                        t.setDescription(rs.getString(3));
                        t.setName(rs.getString(4));
                        t.setUrl(rs.getString(5));
                        t.setVersion(rs.getFloat(6));
                        t.setFile(rs.getString(7));
                        t.setScope(ToolScope.values()[rs.getInt(8)]);
                        tools.add(t);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
        return tools;
    }

    private final static String SQL_BY_JOB = "SELECT t.id, t.author, t.description, t.name, t.url, t.version, t.file, t.scope "
            + "FROM job j LEFT JOIN tool t on (j.tool_id=t.id) WHERE j.id=?";

    public Tool byJob(long job_id) throws MGXException {

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(SQL_BY_JOB)) {
                stmt.setLong(1, job_id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Tool t = new Tool();
                        t.setId(rs.getLong(1));
                        t.setAuthor(rs.getString(2));
                        t.setDescription(rs.getString(3));
                        t.setName(rs.getString(4));
                        t.setUrl(rs.getString(5));
                        t.setVersion(rs.getFloat(6));
                        t.setFile(rs.getString(7));
                        t.setScope(ToolScope.values()[rs.getInt(8)]);
                        return t;
                    }
                }
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
        throw new MGXException("No object of type Job for ID " + job_id + ".");
    }

    private final static String UPDATE = "UPDATE tool SET author=?, description=?, name=?, url=?, version=?, file=?, scope=? "
            + "WHERE id=?";

    public void update(Tool obj) throws MGXException {
        if (obj.getId() == Tool.INVALID_IDENTIFIER) {
            throw new MGXException("Cannot update object of type " + getClassName() + " without an ID.");
        }
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE)) {
                stmt.setString(1, obj.getAuthor());
                stmt.setString(2, obj.getDescription());
                stmt.setString(3, obj.getName());
                stmt.setString(4, obj.getUrl());
                stmt.setFloat(5, obj.getVersion());
                stmt.setString(6, obj.getFile());
                stmt.setInt(7, obj.getScope().getValue());
                stmt.setLong(8, obj.getId());
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new MGXException(ex);
        }
    }

    public long installGlobalTool(Tool global) throws MGXException {

        Tool t = new Tool();

        // manual clone
        t.setName(global.getName());
        t.setDescription(global.getDescription());
        t.setVersion(global.getVersion());
        t.setAuthor(global.getAuthor());
        t.setUrl(global.getUrl());
        t.setFile(global.getFile());
        t.setScope(global.getScope());

        // persist to create the id
        long id = create(t);

        /*
         * copy over the graph description file in order to maintain a
         * consistent project state even if the global tool is updated; the
         * project directory is created on demand
         */
        StringBuilder targetName = null;
        try {
            targetName = new StringBuilder(getController().getProjectDirectory().getAbsolutePath())
                    .append(File.separator).append("jobs");
            File targetDir = new File(targetName.toString());
            if (!targetDir.exists()) {
                UnixHelper.createDirectory(targetDir);
            }
            targetName.append(File.separator).append(id);
            if (global.getFile().endsWith(".xml")) {
                targetName.append(".xml");
            } else {
                targetName.append(".cwl");
            }

            File src = new File(global.getFile());
            File dest = new File(targetName.toString());
            UnixHelper.copyFile(src, dest);
            UnixHelper.makeFileGroupWritable(dest.getAbsolutePath());
        } catch (IOException ex) {
            throw new MGXException(ex.getMessage());
        }

        // update graph description
        t.setFile(targetName.toString());
        update(t);

        return id;
    }

    public TaskI delete(long id) throws MGXException, IOException {
        List<TaskI> subtasks = new ArrayList<>();
        try (AutoCloseableIterator<Job> iter = getController().getJobDAO().byTool(id)) {
            while (iter.hasNext()) {
                Job d = iter.next();
                TaskI t = getController().getJobDAO().delete(d.getId());
                subtasks.add(t);
            }
        }

        return new DeleteTool(getController().getDataSource(), id,
                getController().getProjectName(), getController().getProjectJobDirectory().getAbsolutePath());
    }

    private final static String[] DEFAULT_TOOL_NAMES = new String[]{"MGX taxonomic classification"};

    public List<Tool> getDefaultTools(List<Tool> globalTools) throws MGXException {
        List<Tool> defaultTools = new ArrayList<>();

        List<Tool> projectTools = getTools();
        for (String toolName : DEFAULT_TOOL_NAMES) {
            Tool projectTool = findByName(toolName, projectTools);

            if (projectTool == null) {
                // tool missing, need to install from repo
                getController().log("Installing tool " + toolName + " from global repository.");
                Tool globalTool = findByName(toolName, globalTools);
                long projectToolId = installGlobalTool(globalTool);
                projectTool = getById(projectToolId);
            }

            defaultTools.add(projectTool);
        }

        return defaultTools;
    }

    private static Tool findByName(String name, List<Tool> tools) {
        for (Tool t : tools) {
            if (name.equals(t.getName())) {
                return t;
            }
        }
        return null;
    }
}
