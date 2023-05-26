/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.commonwl;

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

    @SuppressWarnings("unchecked")
    public static AutoCloseableIterator<JobParameter> getParameters(String workflowContent) {
        List<JobParameter> parameters = new ArrayList<>();
        Yaml yaml = new Yaml();
        InputStream inputStream = new ByteArrayInputStream(workflowContent.getBytes());
        Map<String, List<Map<String, Object>>> obj = yaml.load(inputStream);

        if (obj.containsKey("inputs")) {
            // workflows generated with the rabix composer have the inputs as a 
            // top level key
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
                    jp.setNodeId(-1);
                    jp.setParameterName(id);
                    jp.setOptional(type.endsWith("?"));
                    jp.setClassName(type);
                    jp.setDisplayName(description);
                    jp.setUserName(id);
                    jp.setUserDescription(description);
                    parameters.add(jp);
                }
            }
        } else if (obj.containsKey("$graph")) {
            // cwltool --pack followed by
            // python -c 'import sys, yaml, json; yaml.safe_dump(json.load(sys.stdin), sys.stdout, default_flow_style=False)' < packed.cwl > packed.yml
            List<Map<String, Object>> tmp = obj.get("$graph");
            for (Map<String, Object> map : tmp) {
                if (map.containsKey("inputs") && map.containsKey("class") && "Workflow".equals(map.get("class")) && map.get("inputs") instanceof List) {
                    List<Map<String, Object>> inputs = (List<Map<String, Object>>) map.get("inputs");

                    for (Map<String, Object> m : inputs) {
                        String id = (String) m.get("id");
                        if (id.contains("/")) {
                            id = id.substring(id.lastIndexOf("/") + 1);
                        }
                        String description = (String) (m.containsKey("doc") ? m.get("doc") : null);
                        String type = null;
                        Object t = m.get("type");
                        if (t instanceof String) {
                            type = (String) t;
                        } else if (t instanceof Map) {
                            Map<String, Object> cmplxType = (Map<String, Object>) t;
                            if (cmplxType.containsKey("type") && "array".equals(cmplxType.get("type"))) {
                                type = cmplxType.get("items") + "[]";
                            }
                        } else {
                            return null;
                        }

                        if (type != null && !type.contains("File") && !type.contains("Directory") && !isInternalParameter(id)) {
                            JobParameter jp = new JobParameter();
                            jp.setNodeId(-1);
                            jp.setParameterName(id);
                            jp.setOptional(type.endsWith("?"));
                            jp.setClassName(type);
                            jp.setDisplayName(description);
                            jp.setUserName(id);
                            jp.setUserDescription(description);
                            parameters.add(jp);
                        }
                    }
                }
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
