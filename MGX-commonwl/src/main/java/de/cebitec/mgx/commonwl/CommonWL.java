/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.commonwl;

import de.cebitec.mgx.core.MGXException;
import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author sj
 */
public class CommonWL {

    public static AutoCloseableIterator<JobParameter> getParameters(String workflowContent) {
        List<JobParameter> parameters = new ArrayList<>();
        Yaml yaml = new Yaml();
        InputStream inputStream = new ByteArrayInputStream(workflowContent.getBytes());
        Map<String, List<Map<String, Object>>> obj = yaml.load(inputStream);
        List<Map<String, Object>> inputs = obj.get("inputs");
        for (Map<String, Object> m : inputs) {
            String id = (String) m.get("id");
            String type = (String) m.get("type");
            String description = null;
            if (m.containsKey("doc")) {
                description = (String) m.get("doc");
            }

            if (!type.contains("File") && !type.contains("Directory") && !isInternalParameter(id)) {
                JobParameter jp = new JobParameter();
                jp.setParameterName(id);
                jp.setOptional(type.endsWith("?"));
                jp.setClassName(type);
                jp.setDisplayName(description);
                jp.setUserName(description);
                parameters.add(jp);
            }
        }
        try {
            inputStream.close();
        } catch (IOException ex) {
        }

        return new ForwardingIterator<>(parameters.iterator());
    }

    private static boolean isInternalParameter(String id) {
        switch (id) {
            case "runIds":
            case "apiKey":
            case "projectName":
            case "hostURI":
                return true;

        }
        return false;
    }
}
