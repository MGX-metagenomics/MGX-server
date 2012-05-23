/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.jobsubmitter.data.util;



import de.cebitec.mgx.jobsubmitter.data.impl.Choices;
import de.cebitec.mgx.jobsubmitter.data.impl.ConfigItem;
import de.cebitec.mgx.jobsubmitter.data.impl.Node;
import de.cebitec.mgx.jobsubmitter.data.impl.Store;
import de.cebitec.mgx.util.JobParameter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author belmann
 */
public class Transform {

   public static List<JobParameter> getFromNodeStoreJobParameter(Store store) {
	List<JobParameter> parameters = new ArrayList<JobParameter>();

	Iterator nodeIterator = store.getIterator();
	Map.Entry nodeME;
	String nodeId;
	Iterator configItemIterator;
	while (nodeIterator.hasNext()) {

	   nodeME = (Map.Entry) nodeIterator.next();
	   nodeId = (String) nodeME.getKey();
           Node node = (Node) nodeME.getValue();
	   configItemIterator = (node).getIterator();
	   Map.Entry configItemME;

	 
	   String configItemName;


	   while (configItemIterator.hasNext()) {

		configItemME = (Map.Entry) configItemIterator.next();
		ConfigItem configItem = (ConfigItem) configItemME.getValue();
		configItemName = (String) configItemME.getKey();

		JobParameter jobParameter = new JobParameter();
		jobParameter.setConfigItemName(configItemName);
		jobParameter.setConfigItemValue(configItem.getAnswer());
                jobParameter.setClassName(node.getClassName());
                jobParameter.setDisplayName(node.getDisplayName());
		jobParameter.setDefaultValue(configItem.getDefaultValue());
		jobParameter.setNodeId(Long.getLong(nodeId));
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
                newNode=true;
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

           if(newConfig){
           node.addConfigItem(configItem);
           }
           
           if(newNode){
           store.addNode(node);
           }
           
           
	}
	return store;
   }
}
