package de.cebitec.mgx.jobsubmitter.data.util;

import de.cebitec.mgx.jobsubmitter.data.impl.Choices;
import de.cebitec.mgx.jobsubmitter.data.impl.ConfigItem;
import de.cebitec.mgx.jobsubmitter.data.impl.Node;
import de.cebitec.mgx.jobsubmitter.data.impl.Store;
import de.cebitec.mgx.model.db.JobParameter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Sorgt fuer die Umwandlung zwischne Store und JobParameter.
 *
 * @author belmann
 */
public class Transform {

    /**
     * Giibt von einem Stoer die JobParameter zurueck.
     *
     * @param store
     * @return Liste von JobParametern
     */
    public static List<JobParameter> getFromNodeStoreJobParameter(Store store) {
        List<JobParameter> parameters = new ArrayList<>();
        Map.Entry<String, Node> nodeME;
        String nodeId;
        Iterator<Entry<String, Node>> nodeIterator = store.getIterator();
        Iterator<Entry<String, ConfigItem>> configItemIterator;
        while (nodeIterator.hasNext()) {
            nodeME = nodeIterator.next();
            nodeId = nodeME.getKey();
            Node node = nodeME.getValue();
            configItemIterator = node.getIterator();
            Entry<String, ConfigItem> configItemME;
            String configItemName;

            while (configItemIterator.hasNext()) {

                configItemME = configItemIterator.next();
                ConfigItem configItem = configItemME.getValue();
                configItemName = configItemME.getKey();

                JobParameter jobParameter = new JobParameter();
                jobParameter.setConfigItemName(configItemName);
                jobParameter.setConfigItemValue(configItem.getAnswer());
                jobParameter.setClassName(node.getClassName());
                jobParameter.setDisplayName(node.getDisplayName());
                jobParameter.setDefaultValue(configItem.getDefaultValue());
                jobParameter.setNodeId(Long.parseLong(nodeId));

                jobParameter.setOptional(configItem.isOptional());
                jobParameter.setType(configItem.getConfigType());
                jobParameter.setUserDescription(configItem.getUserDescription());
                jobParameter.setUserName(configItem.getUserName());
                jobParameter.setChoices(configItem.getChoice().getChoices());

                parameters.add(jobParameter);
            }
        }
        return parameters;
    }

    /**
     * Gibt von einer Liste von JobParametern einen NodeStore zurueck.
     *
     * @param parameters
     * @return NodeStore.
     */
    public static Store getFromJobParameterNodeStore(List<JobParameter> parameters) {
        Store store = new Store();

        for (JobParameter parameter : parameters) {
            boolean newNode = false;
            boolean newConfig = false;
            Node node;

            if (store.getNode(Long.toString(
                    parameter.getNodeId())) == null) {
                node = new Node(parameter.getClassName(),
                        Long.toString(parameter.getNodeId()));
                newNode = true;
            } else {
                node = store.getNode(Long.toString(parameter.getNodeId()));
            }
            node.setDisplayName(parameter.getDisplayName());

            ConfigItem configItem;

            if (node.getConfigItem(parameter.getConfigItemName()) == null) {


                configItem = new ConfigItem(parameter.getUserName(),
                        parameter.getUserDescription(), parameter.getConfigItemName());
                newConfig = true;

            } else {
                configItem = node.getConfigItem(parameter.getConfigItemName());
            }

            configItem.setChoice(new Choices(parameter.getChoices()));
            configItem.setConfigType(parameter.getType());
            configItem.setDefaultValue(parameter.getDefaultValue());
            configItem.setOptional(parameter.isOptional());
            configItem.setUserDescription(parameter.getUserDescription());

            if (newConfig) {
                node.addConfigItem(configItem);
            }

            if (newNode) {
                store.addNode(node);
            }


        }
        return store;
    }
}
