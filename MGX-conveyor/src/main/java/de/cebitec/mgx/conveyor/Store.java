package de.cebitec.mgx.conveyor;

import de.cebitec.mgx.model.db.JobParameter;
import de.cebitec.mgx.util.AutoCloseableIterator;
import de.cebitec.mgx.util.ForwardingIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * Verwaltet die Nodes einer vom User möglichen Auswahl an Tools.
 *
 *
 * @author belmann
 */
public class Store implements Iterable<Entry<Long, Node>> {

    /**
     * Speichert die Nodes bzw. Tools in einer HashMap. Dabei stellen die id den
     * key dar.
     */
    private final Map<Long, Node> nodes = new TreeMap<>();

    @Override
    public AutoCloseableIterator<Entry<Long, Node>> iterator() {
        return new ForwardingIterator<>(nodes.entrySet().iterator());
    }

    public Set<Entry<Long, Node>> entrySet() {
        return nodes.entrySet();
    }

    public final Node getNode(Long nodeId) {
        return nodes.get(nodeId);
    }

    public final void addNode(Node node) {
        nodes.put(node.getId(), node);
    }

    /**
     * extracts parameters from store
     *
     * @return parameter iterator
     */
    public final AutoCloseableIterator<JobParameter> extractJobParameters() {
        List<JobParameter> parameters = new ArrayList<>();
        
        for (Entry<Long, Node> entry : nodes.entrySet()) {
            
            final Long nodeId = entry.getKey();
            final Node node = entry.getValue();
            
            for (Entry<String, ConfigItem> configItementry : node.entrySet()) {
                final ConfigItem configItem = configItementry.getValue();

                JobParameter jobParameter = new JobParameter()
                        .setId(-1L)
                    .setParameterName(configItementry.getKey())
                    .setParameterValue(configItem.getAnswer())
                    .setClassName(node.getClassName())
                    .setDisplayName(node.getDisplayName())
                    .setDefaultValue(configItem.getDefaultValue())
                    .setNodeId(nodeId)
                    .setOptional(configItem.isOptional())
                    .setType(configItem.getConfigType())
                    .setUserDescription(configItem.getUserDescription())
                    .setUserName(configItem.getUserName())
                    .setChoices(configItem.getChoice().getChoices());

                parameters.add(jobParameter);
            }
        }
        return new ForwardingIterator<>(parameters.iterator());
    }
}
