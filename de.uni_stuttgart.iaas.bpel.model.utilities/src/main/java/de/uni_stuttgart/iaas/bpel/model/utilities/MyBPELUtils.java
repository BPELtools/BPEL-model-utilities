package org.bpel4chor.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bpel4chor.utils.exceptions.AmbiguousPropertyForLinkException;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.CorrelationSet;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Sources;
import org.eclipse.bpel.model.Target;
import org.eclipse.bpel.model.Targets;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.Variables;
import org.eclipse.bpel.model.messageproperties.Property;
import org.eclipse.bpel.model.resource.BPELResource;
import org.eclipse.bpel.model.resource.BPELResourceFactoryImpl;
import org.eclipse.bpel.model.util.BPELUtils;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.wst.wsdl.internal.util.WSDLResourceFactoryImpl;
import org.eclipse.xsd.util.XSDResourceFactoryImpl;

/**
 * MyBPELUtils is useful in the BPEL world, e.g. to find stuffs or resolve
 * stuffs.
 * 
 * @since Feb 19, 2012
 * @author Daojun Cui
 */
public class MyBPELUtils extends BPELUtils {
	
	/**
	 * Get the first property in the process's correlation set
	 * 
	 * @param process
	 * @return the property or null
	 */
	public static Property findFirstProperty(Process process) {
		
		if (process == null) {
			throw new NullPointerException();
		}
		
		if (process.getCorrelationSets() == null) {
			return null;
		}
		if (process.getCorrelationSets().getChildren().size() == 0) {
			return null;
		}
		
		Property firstProperty = null;
		
		CorrelationSet firstCorreSet = process.getCorrelationSets().getChildren().get(0);
		
		if (firstCorreSet.getProperties().size() == 0) {
			return null;
		}
		
		firstProperty = firstCorreSet.getProperties().get(0);
		
		return firstProperty;
	}
	
	/**
	 * Find the flow where the link given resides.
	 * <p>
	 * The parent will be found either from the source or the target.
	 * <p>
	 * The search path looks like:
	 * <ul>
	 * <li>link.getSource -> source -> source.eContainer -> activity ->
	 * activity.eContainer -> flow
	 * <li>link.getTarget -> target -> target.eContainer -> activity ->
	 * activity.eContainer -> flow
	 * </ul>
	 * 
	 * @param link The link in the source or target of activity
	 * @return the flow or null
	 */
	public static Flow findFirstParentFlow(Link link) {
		
		EObject found = null;
		
		if (link == null) {
			throw new NullPointerException();
		}
		
		// try source first
		if (link.getSources().size() > 0) {
			Source source = link.getSources().get(0);
			Activity activity = source.getActivity();
			found = activity.eContainer();
			
			if (found instanceof Flow) {
				return (Flow) found;
			}
		}
		
		// if no luck by the source, try the target
		if (link.getTargets().size() > 0) {
			Target target = link.getTargets().get(0);
			Activity activity = target.getActivity();
			found = activity.eContainer();
			
			if (found instanceof Flow) {
				return (Flow) found;
			}
		}
		
		return null;
	}
	
	/**
	 * Find the link in flow with the name given
	 * 
	 * @param flow
	 * @param linkName
	 * @return the link or null
	 */
	public static Link resolveLink(Flow flow, String linkName) {
		
		if ((flow == null) || (linkName == null)) {
			throw new NullPointerException();
		}
		
		for (Link link : flow.getLinks().getChildren()) {
			if (link.getName().equals(linkName)) {
				return link;
			}
		}
		
		return null;
	}
	
	/**
	 * Look up the link that has the given name and resides in activity's
	 * target.
	 * 
	 * <p>
	 * This method embraces the principle that we find the perfect match link,
	 * or we throw error to the caller. Multiple matches will raise
	 * {@link AmbiguousPropertyForLinkException}.
	 * 
	 * @param linkName The name to find link with
	 * @return The found link or <tt>null</tt>
	 * @throws AmbiguousPropertyForLinkException if multiple links that fit the
	 *             description are found
	 */
	public static Link findLinkInActivityTarget(String linkName, Process process) throws AmbiguousPropertyForLinkException {
		
		if ((linkName == null) || (process == null)) {
			throw new NullPointerException("argument is null");
		}
		if (linkName.isEmpty()) {
			throw new IllegalArgumentException("argument is empty");
		}
		
		ActivityIterator actIterator = new ActivityIterator(process);
		List<Link> found = new ArrayList<Link>();
		
		// iterate through all the activities
		while (actIterator.hasNext()) {
			Activity activity = actIterator.next();
			Targets targets = activity.getTargets();
			if (targets != null) {
				for (Target target : targets.getChildren()) {
					Link linkInTarget = target.getLink();
					if ((linkInTarget != null) && linkInTarget.getName().equals(linkName)) {
						found.add(linkInTarget);
					}
				}
			}
		}
		
		// nothing found
		if (found.size() == 0) {
			return null;
		}
		// found too many
		if (found.size() > 1) {
			throw new AmbiguousPropertyForLinkException("Ambiguous link name:" + linkName + ", multiple instances are found.");
		}
		
		// perfect match, single one
		return found.get(0);
		
	}
	
	/**
	 * Look up the link that has the given name and resides in activity's
	 * source.
	 * 
	 * @param linkName
	 * @param process
	 * @return
	 * @throws AmbiguousPropertyForLinkException
	 */
	public static Link findLinkInActivitySource(String linkName, Process process) throws AmbiguousPropertyForLinkException {
		if ((linkName == null) || (process == null)) {
			throw new NullPointerException("argument is null");
		}
		if (linkName.isEmpty()) {
			throw new IllegalArgumentException("argument is empty");
		}
		
		ActivityIterator actIterator = new ActivityIterator(process);
		List<Link> found = new ArrayList<Link>();
		
		// iterate through all the activities
		while (actIterator.hasNext()) {
			Activity activity = actIterator.next();
			Sources sources = activity.getSources();
			if (sources != null) {
				for (Source source : sources.getChildren()) {
					Link linkInSource = source.getLink();
					if ((linkInSource != null) && linkInSource.getName().equals(linkName)) {
						found.add(linkInSource);
					}
				}
			}
		}
		
		// nothing found
		if (found.size() == 0) {
			return null;
		}
		// found too many
		if (found.size() > 1) {
			throw new AmbiguousPropertyForLinkException("Ambiguous link name:" + linkName + ", multiple instances are found.");
		}
		
		// perfect match, single one
		return found.get(0);
	}
	
	/**
	 * Look up the activity with the name given in the process
	 * 
	 * @param actName Activity name
	 * @param process The process
	 * @return The found activity or null
	 */
	public static Activity resolveActivity(String actName, Process process) {
		
		if ((actName == null) || (process == null)) {
			throw new NullPointerException();
		}
		
		// ActivityFinder finder = new ActivityFinder(process);
		//
		// try {
		// Activity found = finder.find(actName);
		// return found;
		// } catch (ActivityNotFoundException e) {
		// return null;
		// }
		
		ActivityIterator actIterator = new ActivityIterator(process);
		while (actIterator.hasNext()) {
			Activity act = actIterator.next();
			if (act.getName().equals(actName)) {
				return act;
			}
		}
		return null;
	}
	
	/**
	 * Look up the variable with the name given in the process.
	 * 
	 * @param varName
	 * @param process
	 * @return Variable or null
	 */
	public static Variable resolveVariable(String varName, Process process) {
		if ((varName == null) || (process == null)) {
			throw new NullPointerException();
		}
		
		Variables variables = process.getVariables();
		for (Variable var : variables.getChildren()) {
			if (var.getName().equals(varName)) {
				return var;
			}
		}
		
		return null;
	}
	
	/**
	 * Print the BPEL process to console
	 * <p>
	 * Note that this method can only print out the processes that contain no
	 * WSDL imports.
	 * 
	 * @param process
	 * @throws IOException
	 */
	public static void print(Process process) throws IOException {
		// init
		BPELPlugin bpelPlugin = new BPELPlugin();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpel", new BPELResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("wsdl", new WSDLResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xsd", new XSDResourceFactoryImpl());
		
		boolean useNSPrefix = false;
		ResourceSet resourceSet = new ResourceSetImpl();
		// "processName.bpel" is only a work-around to bypass the need of URI in
		// BPELResource
		URI uri = URI.createFileURI(process.getName() + ".bpel");
		BPELResource resource = (BPELResource) resourceSet.createResource(uri);
		resource.setOptionUseNSPrefix(useNSPrefix);
		resource.getContents().add(process);
		
		Map argsMap = new HashMap();
		// this args map prevents a NPE in the release 1.0
		argsMap.put("", "");
		resource.save(System.out, argsMap);
	}
	
	/**
	 * Look up the correlation set in the BPEL process with the name given
	 * 
	 * @param process
	 * @param correlationSetName
	 * @return
	 */
	public static CorrelationSet resolveCorrelationSet(Process process, String correlationSetName) {
		
		if ((process == null) || (correlationSetName == null)) {
			throw new NullPointerException();
		}
		if (process.getCorrelationSets() == null) {
			return null;
		}
		
		List<CorrelationSet> correlSets = process.getCorrelationSets().getChildren();
		for (CorrelationSet correlSet : correlSets) {
			if (correlSet.getName().equals(correlationSetName)) {
				return correlSet;
			}
		}
		return null;
	}
}
