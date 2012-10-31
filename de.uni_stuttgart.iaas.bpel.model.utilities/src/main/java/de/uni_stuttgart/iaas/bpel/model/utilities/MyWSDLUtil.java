package de.uni_stuttgart.iaas.bpel.model.utilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;

import org.eclipse.bpel.model.Import;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.messageproperties.Property;
import org.eclipse.bpel.model.messageproperties.PropertyAlias;
import org.eclipse.bpel.model.partnerlinktype.PartnerLinkType;
import org.eclipse.bpel.model.util.WSDLUtil;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.Message;
import org.eclipse.wst.wsdl.PortType;
import org.eclipse.wst.wsdl.internal.impl.DefinitionImpl;
import org.eclipse.wst.wsdl.internal.util.WSDLResourceFactoryImpl;
import org.w3c.dom.Document;

/**
 * MyWSDLUtil can find all kinds of artifacts in WSDL definition with different
 * criterion.
 * 
 * @since Feb 15, 2012
 * @author Daojun Cui
 */
public class MyWSDLUtil extends WSDLUtil {
	/**
	 * Find propertyAlias in definition based on the three propertyAlias
	 * artifacts: propertyName, messageType, part.
	 * 
	 * @param definition
	 * @param propertyQName
	 * @param messageQName
	 * @param part
	 *            The part name
	 * @return the propertyAlias or null
	 */
	public static PropertyAlias findPropertyAlias(Definition definition, QName propertyQName,
			QName messageQName, String part) {

		if (definition == null || propertyQName == null || messageQName == null || part == null)
			throw new NullPointerException("argument is null");

		Iterator<?> it = definition.getExtensibilityElements().iterator();
		while (it.hasNext()) {
			Object e = it.next();
			if (e instanceof PropertyAlias) {
				PropertyAlias currentAlias = (PropertyAlias) e;

				// propertyName
				QName currentPropertyQName = ((Property) currentAlias.getPropertyName()).getQName();
				if (currentPropertyQName.getLocalPart().equals(propertyQName.getLocalPart()) == false)
					continue;

				// messageType
				QName currentMsgQName = ((Message) currentAlias.getMessageType()).getQName();
				if (currentMsgQName.getLocalPart().equals(messageQName.getLocalPart()) == false)
					continue;

				// part
				if (currentAlias.getPart().equals(part) == false)
					continue;

				return currentAlias;
			}
		}
		return null;

	}

	/**
	 * Find the propertyAlias that refer to the property given.
	 * 
	 * @param definition
	 * @param propertyQName
	 * @return The found propertyAliases, it is empty if nothing is found.
	 */
	public static PropertyAlias[] findPropertyAlias(Definition definition, QName propertyQName) {

		if (definition == null || propertyQName == null)
			throw new NullPointerException("argument is null");

		List<PropertyAlias> aliasList = new ArrayList<PropertyAlias>();

		Iterator<?> it = definition.getExtensibilityElements().iterator();
		while (it.hasNext()) {
			Object e = it.next();
			if (e instanceof PropertyAlias) {
				PropertyAlias currentAlias = (PropertyAlias) e;

				// propertyName
				QName currentPropertyQName = ((Property) currentAlias.getPropertyName()).getQName();
				if (currentPropertyQName.getLocalPart().equals(propertyQName.getLocalPart())) {
					aliasList.add(currentAlias);
				}

			}
		}

		return aliasList.toArray(new PropertyAlias[0]);

	}

	/**
	 * Find the property with the name given in the definition
	 * 
	 * @param defn
	 * @param propertyName
	 * @return
	 */
	public static Property findProperty(Definition defn, String propertyName) {
		if (defn == null || propertyName == null)
			throw new NullPointerException();

		Iterator<?> it = defn.getExtensibilityElements().iterator();
		while (it.hasNext()) {
			Object e = it.next();
			if (e instanceof Property && ((Property) e).getName().equals(propertyName)) {
				return (Property) e;
			}
		}
		return null;
	}

	/**
	 * Find partnerLinkType in the definition
	 * 
	 * @param definition
	 * @param partnerLinkTypeName
	 * @return the partnerLinkType or null
	 */
	public static PartnerLinkType findPartnerLinkType(Definition definition,
			String partnerLinkTypeName) {

		if (definition == null || partnerLinkTypeName == null)
			throw new NullPointerException("argument is null");

		Iterator<?> it = definition.getExtensibilityElements().iterator();
		while (it.hasNext()) {
			Object e = it.next();
			if (e instanceof PartnerLinkType) {
				PartnerLinkType plt = (PartnerLinkType) e;
				if (plt.getName().equals(partnerLinkTypeName)) {
					return plt;
				}
			}
		}
		return null;
	}

	/**
	 * Find portType in definition with name given
	 * 
	 * @param definition
	 * @param portTypeName
	 * @return
	 */
	public static PortType findPortType(Definition definition, String portTypeName) {
		List<PortType> portTypes = definition.getEPortTypes();
		for (PortType next : portTypes) {
			if (next.getQName().getLocalPart().equals(portTypeName))
				return next;
		}
		return null;
	}

	/**
	 * Get the wsdl definition via process given
	 * 
	 * @param process
	 * @throws WSDLException
	 * @throws IOException
	 */
	public static Definition getWSDLOf(Process process) throws WSDLException, IOException {
		Definition dfn = null;
		URI bpelURI = process.eResource().getURI();
		String bpelURIString = bpelURI.toFileString();

		List<Import> imps = process.getImports();
		for (Import imp : imps) {
			if (imp.getLocation().endsWith(".xsd")) {
				continue;// imp might be type.xsd, just skip it.
			}

			// read the wsdl anyway
			if (isAbsolutePath(imp.getLocation())) {
				dfn = readWSDL(imp.getLocation());
			} else {
				int endIndex = bpelURIString.lastIndexOf(process.getName() + ".bpel");
				String wsdlURIString = bpelURIString.substring(0, endIndex) + imp.getLocation();
				dfn = readWSDL(wsdlURIString);
			}

			// test whether it is the one we want
			Document document = dfn.getDocument();
			String name = document.getDocumentElement().getAttribute("name");
			if (name.equals(process.getName()))
				return dfn;

		}

		throw new WSDLException("Can not find WSDL.", "Can not find WSDL");

	}

	protected static boolean isAbsolutePath(String path) {
		if (path == null || path.isEmpty())
			return false;

		return path.contains(":");// it just works in windows

	}

	/**
	 * Print the WSDL definition to the console
	 * 
	 * @param dfn
	 * @throws WSDLException
	 * @throws IOException
	 */
	public static void print(Definition dfn) throws WSDLException, IOException {
		Resource resource = ((DefinitionImpl) dfn).eResource();

		String name = dfn.getQName().getLocalPart();
		if (name == null || name.isEmpty())
			throw new IllegalStateException();

		if (resource == null) {

			ResourceSet rs = new ResourceSetImpl();
			rs.getResourceFactoryRegistry().getExtensionToFactoryMap()
					.put("wsdl", new WSDLResourceFactoryImpl());
			resource = rs.createResource(URI.createFileURI(name + ".wsdl"));
			resource.getContents().add(dfn);
		}

		Map args = new HashMap();
		args.put("", "");
		resource.save(System.out, args);
	}
	
	/**
	 * Read the WSDL document into a definition
	 * 
	 * @param wsdlURI The WSDL URI
	 * @return
	 * @throws WSDLException
	 * @throws IOException
	 */
	public static Definition readWSDL(String wsdlURI) throws WSDLException, IOException {
		
		if ((wsdlURI == null) || wsdlURI.isEmpty()) {
			throw new IllegalArgumentException();
		}
		
		if (!wsdlURI.endsWith(".wsdl")) {
			throw new IllegalArgumentException("invalid wsdl uri. " + wsdlURI);
		}
		
		ResourceSet rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("wsdl", new WSDLResourceFactoryImpl());
		Resource resource = rs.createResource(URI.createFileURI(wsdlURI));
		resource.load(null);
		Definition root = (Definition) resource.getContents().iterator().next();
		return root;
	}

}
