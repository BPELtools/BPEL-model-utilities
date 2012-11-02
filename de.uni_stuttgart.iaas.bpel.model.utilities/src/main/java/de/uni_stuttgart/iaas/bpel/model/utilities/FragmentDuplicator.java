package de.uni_stuttgart.iaas.bpel.model.utilities;

import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Compensate;
import org.eclipse.bpel.model.CompensateScope;
import org.eclipse.bpel.model.Condition;
import org.eclipse.bpel.model.Copy;
import org.eclipse.bpel.model.Correlation;
import org.eclipse.bpel.model.CorrelationSet;
import org.eclipse.bpel.model.Correlations;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.Exit;
import org.eclipse.bpel.model.Expression;
import org.eclipse.bpel.model.ExtensionActivity;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.ForEach;
import org.eclipse.bpel.model.From;
import org.eclipse.bpel.model.If;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Links;
import org.eclipse.bpel.model.MessageExchange;
import org.eclipse.bpel.model.OpaqueActivity;
import org.eclipse.bpel.model.PartnerLink;
import org.eclipse.bpel.model.Pick;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.RepeatUntil;
import org.eclipse.bpel.model.Reply;
import org.eclipse.bpel.model.Rethrow;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Sources;
import org.eclipse.bpel.model.Target;
import org.eclipse.bpel.model.Targets;
import org.eclipse.bpel.model.Throw;
import org.eclipse.bpel.model.To;
import org.eclipse.bpel.model.Validate;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.Wait;
import org.eclipse.bpel.model.While;
import org.eclipse.bpel.model.impl.BPELFactoryImpl;
import org.eclipse.bpel.model.messageproperties.MessagepropertiesFactory;
import org.eclipse.bpel.model.messageproperties.Property;
import org.eclipse.bpel.model.partnerlinktype.PartnerLinkType;
import org.eclipse.bpel.model.partnerlinktype.PartnerlinktypeFactory;
import org.eclipse.bpel.model.partnerlinktype.Role;
import org.eclipse.bpel.model.proxy.MessageProxy;
import org.eclipse.bpel.model.util.BPELUtils;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.Input;
import org.eclipse.wst.wsdl.Message;
import org.eclipse.wst.wsdl.Operation;
import org.eclipse.wst.wsdl.Output;
import org.eclipse.wst.wsdl.Part;
import org.eclipse.wst.wsdl.PortType;
import org.eclipse.wst.wsdl.WSDLFactory;
import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.XSDFactory;
import org.eclipse.xsd.XSDTypeDefinition;
import org.w3c.dom.Element;

/**
 * The FragmentDuplicator helps copying BPEL Activity, getting variables that
 * get used in activity and getting all activities assigned to the given
 * participant.
 * 
 * @since Dec 18, 2011
 * @author Daojun Cui
 */
public class FragmentDuplicator {
	
	protected static Logger logger = Logger.getLogger(FragmentDuplicator.class);
	
	
	/**
	 * Copy the original activity and return a new one.
	 * 
	 * <p>
	 * 1. Note that this copy is not recursively, because we just need a copy of
	 * the activity currently given, not its children.
	 * <p>
	 * 2. Note that the referred partnerLink, portType, operation, and message
	 * should be resolved from the fragment process/definition, NOT from the
	 * non-split process/definition.
	 * <p>
	 * 3. Note that if the activity is PartnerActivity, then the possible
	 * correlation will be copied too.
	 * 
	 * @param origAct The original activity
	 * @param fragProc The fragment process
	 * @param fragDefn The fragment definition
	 * @return The new activity
	 */
	public static Activity copyActivity(Activity origAct, Process fragProc, Definition fragDefn) {
		
		if (origAct == null) {
			throw new NullPointerException("argument is null");
		}
		
		Activity newActivity = null;
		
		if (origAct instanceof Empty) {
			newActivity = FragmentDuplicator.copyEmpty((Empty) origAct);
		} else if (origAct instanceof Invoke) {
			newActivity = FragmentDuplicator.copyInvoke((Invoke) origAct, fragProc, fragDefn);
		} else if (origAct instanceof Assign) {
			newActivity = FragmentDuplicator.copyAssign((Assign) origAct);
		} else if (origAct instanceof Reply) {
			newActivity = FragmentDuplicator.copyReply((Reply) origAct, fragProc, fragDefn);
		} else if (origAct instanceof Receive) {
			newActivity = FragmentDuplicator.copyReceive((Receive) origAct, fragProc, fragDefn);
		} else if (origAct instanceof Wait) {
			newActivity = FragmentDuplicator.copyWait((Wait) origAct);
		} else if (origAct instanceof Throw) {
			newActivity = FragmentDuplicator.copyThrow((Throw) origAct);
		} else if (origAct instanceof Exit) {
			newActivity = FragmentDuplicator.copyExit((Exit) origAct);
		} else if (origAct instanceof Flow) {
			newActivity = FragmentDuplicator.copyFlow((Flow) origAct);
		} else if (origAct instanceof If) {
			newActivity = FragmentDuplicator.copyIf((If) origAct);
		} else if (origAct instanceof While) {
			newActivity = FragmentDuplicator.copyWhile((While) origAct);
		} else if (origAct instanceof Sequence) {
			newActivity = FragmentDuplicator.copySequence((Sequence) origAct);
		} else if (origAct instanceof Pick) {
			newActivity = FragmentDuplicator.copyPick((Pick) origAct);
		} else if (origAct instanceof Scope) {
			newActivity = FragmentDuplicator.copyScope((Scope) origAct);
		} else if (origAct instanceof Compensate) {
			newActivity = FragmentDuplicator.copyCompensate((Compensate) origAct);
		} else if (origAct instanceof CompensateScope) {
			newActivity = FragmentDuplicator.copyCompensateScope((CompensateScope) origAct);
		} else if (origAct instanceof Rethrow) {
			newActivity = FragmentDuplicator.copyRethrow((Rethrow) origAct);
		} else if (origAct instanceof OpaqueActivity) {
			newActivity = FragmentDuplicator.copyOpaqueActivity((OpaqueActivity) origAct);
		} else if (origAct instanceof ForEach) {
			newActivity = FragmentDuplicator.copyForEach((ForEach) origAct);
		} else if (origAct instanceof RepeatUntil) {
			newActivity = FragmentDuplicator.copyRepeatUntil((RepeatUntil) origAct);
		} else if (origAct instanceof Validate) {
			newActivity = FragmentDuplicator.copyValidate((Validate) origAct);
		} else if (origAct instanceof ExtensionActivity) {
			newActivity = FragmentDuplicator.copyExtensionActivity((ExtensionActivity) origAct);
		}
		
		return newActivity;
		
	}
	
	public static Empty copyEmpty(Empty act) {
		return BPELFactory.eINSTANCE.createEmpty();
	}
	
	/**
	 * Copy the original invoke activity and return a new one.
	 * <p>
	 * Note that to setup the configuration of the new activity, the referred
	 * variable, partnerLink, operation, portType must be resolved from the
	 * fragment process/definition.
	 * 
	 * @param origAct
	 * @param fragProc
	 * @param fragDefn
	 * @return
	 */
	public static Invoke copyInvoke(Invoke origAct, Process fragProc, Definition fragDefn) {
		
		if (origAct == null) {
			return null;
		}
		
		Invoke newInvoke = null;
		
		// To copy the invoke, (1) partner link (2) operation (3) portType (4)
		// variable: inputVariable, outputVariable (5) correlation (6)
		// compensationHandler (6) name (7) suppressJoinFailure (8)
		// standard-elements(targets/sources)
		newInvoke = BPELFactory.eINSTANCE.createInvoke();
		
		FragmentDuplicator.copyStandardAttributes(origAct, newInvoke);
		
		// partnerLink
		String partnerLinkName = origAct.getPartnerLink().getName();
		PartnerLink newPlink = MyBPELUtils.getPartnerLink(fragProc, partnerLinkName);
		if (newPlink == null) {
			newPlink = FragmentDuplicator.copyPartnerLink(origAct.getPartnerLink());
		}
		newInvoke.setPartnerLink(newPlink);
		
		// portType
		QName ptQName = origAct.getPortType().getQName();
		PortType newPt = MyWSDLUtil.resolvePortType(fragDefn, ptQName);
		if (newPt == null) {
			newPt = FragmentDuplicator.copyPortType(origAct.getPortType());
		}
		newInvoke.setPortType(newPt);
		
		// operation
		String opName = origAct.getOperation().getName();
		Operation newOp = MyWSDLUtil.findOperation(newPt, opName);
		if (newOp == null) {
			newOp = FragmentDuplicator.copyOperation(origAct.getOperation());
		}
		newInvoke.setOperation(newOp);
		
		// inputVariable
		if (origAct.getInputVariable() != null) {
			String inputVarName = origAct.getInputVariable().getName();
			Variable inputVar = MyBPELUtils.resolveVariable(inputVarName, fragProc);
			if (inputVar == null) {
				throw new IllegalStateException("Variable " + inputVarName + " is not found in fragment process " + fragProc.getName());
			}
			newInvoke.setInputVariable(inputVar);
		}
		
		// outputVariable
		if (origAct.getOutputVariable() != null) {
			String outputVarName = origAct.getOutputVariable().getName();
			Variable outputVar = MyBPELUtils.resolveVariable(outputVarName, fragProc);
			if (outputVar == null) {
				throw new IllegalStateException("Variable " + outputVarName + " is not found in fragment process " + fragProc.getName());
			}
			
			newInvoke.setOutputVariable(origAct.getOutputVariable());
		}
		
		// correlations
		if (origAct.getCorrelations() != null) {
			
			Correlations newCorrelations = FragmentDuplicator.copyCorrelations(origAct.getCorrelations(), fragProc);
			newInvoke.setCorrelations(newCorrelations);
		}
		
		// compensationHandler
		if (origAct.getCompensationHandler() != null) {
			// TODO
			newInvoke.setCompensationHandler(origAct.getCompensationHandler());
		}
		
		FragmentDuplicator.copyStandardElements(origAct, newInvoke);
		
		return newInvoke;
	}
	
	/**
	 * Copy the correlations
	 * 
	 * @param origCorrelations
	 * @return
	 */
	public static Correlations copyCorrelations(Correlations origCorrelations, Process fragProc) {
		
		Correlations newCorrelations = BPELFactory.eINSTANCE.createCorrelations();
		for (Correlation origCorrel : origCorrelations.getChildren()) {
			
			Correlation newCorrel = BPELFactory.eINSTANCE.createCorrelation();
			CorrelationSet newCorrelSet = fragProc.getCorrelationSets().getChildren().get(0);
			
			if (newCorrelSet == null) {
				throw new IllegalStateException("the fragment process does not contain any correlation set, check out the ProcessFragmenter.addCorrelation.");
			}
			
			newCorrel.setSet(newCorrelSet);
			newCorrel.setInitiate(origCorrel.getInitiate());
			newCorrelations.getChildren().add(newCorrel);
			
		}
		
		return newCorrelations;
	}
	
	/**
	 * Copy the original assign activity and return a new one.
	 * <p>
	 * Note that to setup the configuration of the new assign activity, the
	 * referred variable must be resolved from the fragment process/definition.
	 * 
	 * @param act
	 * @return
	 */
	public static Assign copyAssign(Assign act) {
		
		if (act == null) {
			return null;
		}
		
		Assign newAssign = null;
		
		// To copy the assign,
		// 1. standard-attributes
		// 2. standard-elements(targets/sources)
		// 3. copies
		newAssign = BPELFactory.eINSTANCE.createAssign();
		FragmentDuplicator.copyStandardAttributes(act, newAssign);
		
		List<Copy> copyList = act.getCopy();
		for (Copy copy : copyList) {
			newAssign.getCopy().add(FragmentDuplicator.copyCopy(copy));
		}
		
		FragmentDuplicator.copyStandardElements(act, newAssign);
		return newAssign;
	}
	
	/**
	 * Copy the original reply activity and return a new one.
	 * <p>
	 * Note that to setup the configuration of the new activity, the referred
	 * variable, partnerLink, operation, portType must be resolved from the
	 * fragment process/definition.
	 * 
	 * @param origAct
	 * @return
	 */
	public static Reply copyReply(Reply origAct, Process fragProc, Definition fragDefn) {
		
		if (origAct == null) {
			return null;
		}
		
		Reply newReply = null;
		// To copy reply: (1) partnerLink (2) portType (3) variable (4)
		// faultName (5) messageExchange (6) correlations (7) toParts (8)
		// standard-elements(targets/sources) (9) standard-attributes
		
		newReply = BPELFactory.eINSTANCE.createReply();
		FragmentDuplicator.copyStandardAttributes(origAct, newReply);
		
		// partnerLink
		String partnerLinkName = origAct.getPartnerLink().getName();
		PartnerLink newPlink = MyBPELUtils.getPartnerLink(fragProc, partnerLinkName);
		if (newPlink == null) {
			newPlink = FragmentDuplicator.copyPartnerLink(origAct.getPartnerLink());
		}
		newReply.setPartnerLink(newPlink);
		
		// portType
		QName ptQName = origAct.getPortType().getQName();
		PortType newPt = MyWSDLUtil.resolvePortType(fragDefn, ptQName);
		if (newPt == null) {
			newPt = FragmentDuplicator.copyPortType(origAct.getPortType());
		}
		newReply.setPortType(newPt);
		
		// operation
		String opName = origAct.getOperation().getName();
		Operation newOp = MyWSDLUtil.findOperation(newPt, opName);
		if (newOp == null) {
			newOp = FragmentDuplicator.copyOperation(origAct.getOperation());
		}
		newReply.setOperation(newOp);
		
		// variable
		if (origAct.getVariable() != null) {
			String varName = origAct.getVariable().getName();
			Variable var = MyBPELUtils.resolveVariable(varName, fragProc);
			if (var == null) {
				throw new IllegalStateException("Variable " + varName + " is not found in fragment process " + fragProc.getName());
			}
			newReply.setVariable(var);
		}
		
		// correlations
		if (origAct.getCorrelations() != null) {
			
			Correlations newCorrelations = FragmentDuplicator.copyCorrelations(origAct.getCorrelations(), fragProc);
			newReply.setCorrelations(newCorrelations);
		}
		
		// TODO message exchange
		if (origAct.getMessageExchange() != null) {
			newReply.setMessageExchange(origAct.getMessageExchange());
		}
		
		FragmentDuplicator.copyStandardElements(origAct, newReply);
		
		return newReply;
	}
	
	/**
	 * Copy the original reply activity and return a new one.
	 * <p>
	 * Note that to setup the configuration of the new activity, the referred
	 * variable, partnerLink, operation, portType must be resolved from the
	 * fragment process/definition.
	 * 
	 * @param origAct
	 * @return
	 */
	public static Receive copyReceive(Receive origAct, Process fragProc, Definition fragDefn) {
		
		if (origAct == null) {
			return null;
		}
		
		Receive newReceive = null;
		// To copy receive: (1) partner link (2) portType (3)operation (4)
		// variable (5) createInstance (6) messageExchange (7) correlations (8)
		// fromParts (9) standard-elements(targets/sources)
		
		newReceive = BPELFactory.eINSTANCE.createReceive();
		FragmentDuplicator.copyStandardAttributes(origAct, newReceive);
		
		// partnerLink
		String partnerLinkName = origAct.getPartnerLink().getName();
		PartnerLink newPlink = MyBPELUtils.getPartnerLink(fragProc, partnerLinkName);
		if (newPlink == null) {
			newPlink = FragmentDuplicator.copyPartnerLink(origAct.getPartnerLink());
		}
		newReceive.setPartnerLink(newPlink);
		
		// portType
		QName ptQName = origAct.getPortType().getQName();
		PortType newPt = MyWSDLUtil.resolvePortType(fragDefn, ptQName);
		if (newPt == null) {
			newPt = FragmentDuplicator.copyPortType(origAct.getPortType());
		}
		newReceive.setPortType(newPt);
		
		// operation
		String opName = origAct.getOperation().getName();
		Operation newOp = MyWSDLUtil.findOperation(newPt, opName);
		if (newOp == null) {
			newOp = FragmentDuplicator.copyOperation(origAct.getOperation());
		}
		newReceive.setOperation(newOp);
		
		// variable
		if (origAct.getVariable() != null) {
			String varName = origAct.getVariable().getName();
			Variable var = MyBPELUtils.resolveVariable(varName, fragProc);
			if (var == null) {
				throw new IllegalStateException("Variable " + varName + " is not found in fragment process " + fragProc.getName());
			}
			newReceive.setVariable(var);
		}
		
		// correlations
		if (origAct.getCorrelations() != null) {
			
			Correlations newCorrelations = FragmentDuplicator.copyCorrelations(origAct.getCorrelations(), fragProc);
			newReceive.setCorrelations(newCorrelations);
		}
		
		// TODO message exchange
		if (origAct.getMessageExchange() != null) {
			newReceive.setMessageExchange(origAct.getMessageExchange());
		}
		
		FragmentDuplicator.copyStandardElements(origAct, newReceive);
		
		return newReceive;
	}
	
	public static Wait copyWait(Wait act) {
		throw new IllegalStateException("copyWait is not yet implemented");
	}
	
	public static Throw copyThrow(Throw act) {
		throw new IllegalStateException("copyThrow is not yet implemented");
	}
	
	public static Exit copyExit(Exit act) {
		throw new IllegalStateException("copyExit is not yet implemented");
	}
	
	/**
	 * Copy the flow and return a new one
	 * <p>
	 * <b>Note</b>: only the flow structure is copied, the children activities
	 * will NOT be copied together.
	 * 
	 * @param act The flow activity
	 * @return The new flow activity
	 */
	public static Flow copyFlow(Flow act) {
		// To copy flow :
		// 1. standard attributes
		// 2. links, needs to be initialised
		// 3. children should NOT be copied
		// 4. standard elements
		
		Flow newFlow = BPELFactoryImpl.eINSTANCE.createFlow();
		FragmentDuplicator.copyStandardAttributes(act, newFlow);
		
		Links links = BPELFactoryImpl.eINSTANCE.createLinks();
		newFlow.setLinks(links);
		
		for (Link link : act.getLinks().getChildren()) {
			Link newLink = FragmentDuplicator.copyLink(link);
			links.getChildren().add(newLink);
		}
		
		FragmentDuplicator.copyStandardElements(act, newFlow);
		return newFlow;
	}
	
	/**
	 * Copy the while activity, but only the name.
	 * 
	 * @param act The while activity
	 * @return The new copy of while, with the same name, but child not yet
	 *         copied.
	 */
	public static While copyWhile(While act) {
		While newWhile = BPELFactory.eINSTANCE.createWhile();
		
		newWhile.setName(act.getName());
		
		return newWhile;
	}
	
	public static Scope copyScope(Scope act) {
		throw new IllegalStateException("copyScope is not yet implemented");
	}
	
	public static Sequence copySequence(Sequence act) {
		
		throw new IllegalStateException("copySequence is not yet implemented");
	}
	
	public static If copyIf(If act) {
		
		throw new IllegalStateException("copyIf is not yet implemented");
	}
	
	/**
	 * <B>NOTE</B>: The OnMessage and OnAlarm Branches in Pick do not have name,
	 * but they will contain a <b style=color:blue>wsu:id</b>, it must be copied
	 * too.
	 * 
	 * @param act
	 * @return
	 */
	public static Pick copyPick(Pick act) {
		throw new IllegalStateException("copyPick is not yet implemented");
	}
	
	public static Compensate copyCompensate(Compensate act) {
		throw new IllegalStateException("copyCompensate is not yet implemented");
	}
	
	public static CompensateScope copyCompensateScope(CompensateScope act) {
		throw new IllegalStateException("copyCompensateScope is not yet implemented");
	}
	
	public static Rethrow copyRethrow(Rethrow act) {
		throw new IllegalStateException("copyRethrow is not yet implemented");
	}
	
	public static OpaqueActivity copyOpaqueActivity(OpaqueActivity act) {
		throw new IllegalStateException("copyOpaqueActivity is not yet implemented");
	}
	
	public static ForEach copyForEach(ForEach act) {
		throw new IllegalStateException("copyForEach is not yet implemented");
	}
	
	public static RepeatUntil copyRepeatUntil(RepeatUntil act) {
		throw new IllegalStateException("copyRepeatUntil is not yet implemented");
	}
	
	public static Validate copyValidate(Validate act) {
		throw new IllegalStateException("copyValidate is not yet implemented");
	}
	
	public static ExtensionActivity copyExtensionActivity(ExtensionActivity act) {
		throw new IllegalStateException("copyExtensionActivity is not yet implemented");
	}
	
	/**
	 * Copy the standard attributes from original activity into the new one.
	 * 
	 * <p>
	 * Each activity has two optional standard attributes: the name of the
	 * activity and suppressJoinFailure
	 * 
	 * @param origAct The original activity
	 * @param newAct The new activity
	 */
	public static void copyStandardAttributes(Activity origAct, Activity newAct) {
		
		if ((origAct == null) || (newAct == null)) {
			throw new NullPointerException("argument is null, origAct==null:" + (origAct == null) + " newAct==null:" + (newAct == null));
		}
		
		// 1. name="NCName"?
		if (origAct.getName() != null) {
			newAct.setName(origAct.getName());
		}
		
		// 2. suppressionJoinFailure="yes|no"?
		if (origAct.getSuppressJoinFailure() != null) {
			newAct.setSuppressJoinFailure(origAct.getSuppressJoinFailure());
		}
	}
	
	/**
	 * Copy the standard elements from original activity into the new one.
	 * 
	 * <p>
	 * Each activity has optional containers "sources" and "targets", which
	 * contain standard elements "source" and "target" respectively.
	 * 
	 * @param origAct The original activity
	 * @param newAct The new activity
	 */
	public static void copyStandardElements(Activity origAct, Activity newAct) {
		
		if ((origAct == null) || (newAct == null)) {
			throw new NullPointerException("Null parameter error! origAct==null:" + (origAct == null) + " newAct==null:" + (newAct == null));
		}
		
		// sources
		if (origAct.getSources() != null) {
			FragmentDuplicator.copySources(origAct.getSources(), newAct);
		}
		
		// targets
		if (origAct.getTargets() != null) {
			FragmentDuplicator.copyTargets(origAct.getTargets(), newAct);
		}
	}
	
	/**
	 * Copy the original sources and insert into the given container activity
	 * <p>
	 * 
	 * <pre>
	 *  <sources>? 
	 * 	<source linkName="NCName">+ 
	 * 		<transitionCondition expressionLanguage="anyURI"?>?  
	 * 			bool-expr 
	 * 		</transitionCondition> 
	 * 		</source> 
	 * </sources>
	 * </pre>
	 * 
	 * @param origSources The original sources
	 * @param container The container of the new sources
	 */
	public static void copySources(Sources origSources, Activity container) {
		if ((origSources == null) || (container == null)) {
			throw new NullPointerException("argument is null. origSources == null:" + (origSources == null) + " container == null:" + (container == null));
		}
		
		Sources newSources = BPELFactory.eINSTANCE.createSources();
		container.setSources(newSources);
		
		for (Source origSource : origSources.getChildren()) {
			
			Source newSource = BPELFactory.eINSTANCE.createSource();
			
			// a source contains link, container activity and
			// transitionCondition.
			newSource.setLink(FragmentDuplicator.copyLink(origSource.getLink()));
			if (origSource.getTransitionCondition() != null) {
				newSource.setTransitionCondition(FragmentDuplicator.copyCondition(origSource.getTransitionCondition()));
			}
			// BE CAREFUL! In fact, the newSource.setActivity(container) do
			// something like inserting the "newSource" into the sources list of
			// the "container". So do NOT assume that you can use the method
			// newSource.getActivity() to get the pre-set container, well, it
			// will not work.
			newSource.setActivity(container);
			
		}
		
	}
	
	/**
	 * Copy the original Targets and insert it into the new container
	 * 
	 * <p>
	 * The syntax of targets:
	 * 
	 * <pre>
	 * <targets>? 
	 * 		<joinCondition expressionLanguage="anyURI"?>?  
	 * 			bool-expr 
	 * 	</joinCondition> 
	 * 	<target linkName="NCName" />+ 
	 * </targets>
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param origTargets The original Targets
	 * @param container The container activity of the new Targets
	 * @return A new targets
	 */
	public static void copyTargets(Targets origTargets, Activity container) {
		
		if ((origTargets == null) || (container == null)) {
			throw new NullPointerException("argument is null. origTargets == null:" + (origTargets == null) + " container == null:" + (container == null));
		}
		
		Targets newTargets = BPELFactory.eINSTANCE.createTargets();
		
		// a Targets contains joinCondition AND a list of target
		if (origTargets.getJoinCondition() != null) {
			newTargets.setJoinCondition(FragmentDuplicator.copyCondition(origTargets.getJoinCondition()));
		}
		
		container.setTargets(newTargets);
		
		for (Target origTarget : origTargets.getChildren()) {
			// a target contains link and the container activity
			Target newTarget = BPELFactory.eINSTANCE.createTarget();
			
			if (origTarget.getLink() != null) {
				newTarget.setLink(FragmentDuplicator.copyLink(origTarget.getLink()));
			}
			// BE CAREFUL! In fact, the newTarget.setActivity(container) do
			// something like inserting the "newTarget" into the targets list of
			// the "container". And do NOT assume that you can use the
			// newTarget.getActivity() to get the pre-set container, well, it
			// will not work.
			newTarget.setActivity(container);
		}
		
	}
	
	/**
	 * Copy the original link to a new link
	 * 
	 * <p>
	 * <b>Note</b>: Because there is no information to tell whether the target
	 * activity of the link is in the same fragment as the source activity. We
	 * just copy the name, afterwards, we can take the name of the link, and
	 * figure out in the main process, whether to split it or to keep it in the
	 * fragment.
	 * 
	 * @param origLink The original link
	 * @return A new link with the same name as the original one, but the
	 *         "targets" and "sources" will not be copied.
	 */
	public static Link copyLink(Link origLink) {
		
		// link contains : name, targets, sources
		if (origLink == null) {
			throw new NullPointerException("argument is null.");
		}
		
		Link newLink = BPELFactory.eINSTANCE.createLink();
		newLink.setName(origLink.getName());
		
		return newLink;
	}
	
	public static Copy copyCopy(Copy copy) {
		
		if (copy == null) {
			throw new NullPointerException("argument is null.");
		}
		
		Copy newCopy = BPELFactory.eINSTANCE.createCopy();
		newCopy.setFrom(FragmentDuplicator.copyFrom(copy.getFrom()));
		newCopy.setTo(FragmentDuplicator.copyTo(copy.getTo()));
		
		return newCopy;
		
	}
	
	public static Condition copyCondition(Condition condition) {
		if (condition == null) {
			throw new NullPointerException("argument is null.");
		}
		
		Condition newCondition = BPELFactoryImpl.eINSTANCE.createCondition();
		newCondition.setBody(condition.getBody());
		return newCondition;
	}
	
	public static From copyFrom(From from) {
		
		if (from == null) {
			throw new NullPointerException("argument is null.");
		}
		
		From newFrom = BPELFactory.eINSTANCE.createFrom();
		
		if (from.getLiteral() != null) {
			newFrom.setLiteral(from.getLiteral());
		}
		if (from.getType() != null) {
			newFrom.setType(from.getType());
		}
		if (from.getVariable() != null) {
			newFrom.setVariable(FragmentDuplicator.copyVariable(from.getVariable()));
		}
		if (from.getPart() != null) {
			newFrom.setPart(FragmentDuplicator.copyPart(from.getPart()));
		}
		if (from.getProperty() != null) {
			newFrom.setProperty(FragmentDuplicator.copyProperty(from.getProperty()));
		}
		if (from.getQuery() != null) {
			newFrom.setQuery(FragmentDuplicator.copyQuery(from.getQuery()));
		}
		if (from.getLiteral() != null) {
			newFrom.setLiteral(from.getLiteral());
		}
		if (from.getExpression() != null) {
			newFrom.setExpression(FragmentDuplicator.copyExpression(from.getExpression()));
		}
		if (from.getPartnerLink() != null) {
			newFrom.setPartnerLink(FragmentDuplicator.copyPartnerLink(from.getPartnerLink()));
		}
		return newFrom;
	}
	
	public static To copyTo(To to) {
		
		if (to == null) {
			throw new NullPointerException("argument is null.");
		}
		
		To newTo = BPELFactory.eINSTANCE.createTo();
		
		if (to.getVariable() != null) {
			newTo.setVariable(FragmentDuplicator.copyVariable(to.getVariable()));
		}
		if (to.getPart() != null) {
			newTo.setPart(FragmentDuplicator.copyPart(to.getPart()));
		}
		if (to.getProperty() != null) {
			newTo.setProperty(FragmentDuplicator.copyProperty(to.getProperty()));
		}
		if (to.getQuery() != null) {
			newTo.setQuery(FragmentDuplicator.copyQuery(to.getQuery()));
		}
		if (to.getExpression() != null) {
			newTo.setExpression(FragmentDuplicator.copyExpression(to.getExpression()));
		}
		if (to.getPartnerLink() != null) {
			newTo.setPartnerLink(FragmentDuplicator.copyPartnerLink(to.getPartnerLink()));
		}
		return newTo;
	}
	
	public static org.eclipse.bpel.model.Query copyQuery(org.eclipse.bpel.model.Query query) {
		if (query == null) {
			throw new NullPointerException("argument is null.");
		}
		
		org.eclipse.bpel.model.Query newQuery = BPELFactory.eINSTANCE.createQuery();
		
		if (query.getQueryLanguage() != null) {
			newQuery.setQueryLanguage(query.getQueryLanguage());
		}
		if (query.getValue() != null) {
			newQuery.setValue(query.getValue());
		}
		
		return newQuery;
		
	}
	
	public static Expression copyExpression(Expression expression) {
		
		if (expression == null) {
			throw new NullPointerException("argument is null.");
		}
		
		Expression newExpression = BPELFactory.eINSTANCE.createExpression();
		
		if (expression.getExpressionLanguage() != null) {
			newExpression.setExpressionLanguage(expression.getExpressionLanguage());
		}
		if (expression.getBody() != null) {
			newExpression.setBody(expression.getBody());
		}
		
		return newExpression;
		
	}
	
	/**
	 * Get a new copy of the message given
	 * 
	 * @param message
	 * @return
	 */
	public static Message copyMessage(Message message) {
		
		if (message == null) {
			throw new NullPointerException("argument is null.");
		}
		
		Message newMessage = WSDLFactory.eINSTANCE.createMessage();
		
		QName newQName = FragmentDuplicator.copyQName(message.getQName());
		newMessage.setQName(newQName);
		
		List<Part> parts = message.getEParts();
		
		for (Part part : parts) {
			Part newPart = FragmentDuplicator.copyPart(part);
			newMessage.addPart(newPart);
		}
		
		return newMessage;
	}
	
	/**
	 * Get a new copy of the message exchange
	 * 
	 * @param mex
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static MessageExchange copyMessageExchange(MessageExchange mex) {
		MessageExchange newMex = BPELFactory.eINSTANCE.createMessageExchange();
		
		if (mex.getDocumentation() != null) {
			newMex.setDocumentation(mex.getDocumentation());
		}
		
		newMex.setName(mex.getName());
		
		// TODO: Test me please !!
		if (mex.getDocumentationElement() != null) {
			newMex.setDocumentationElement(mex.getDocumentationElement());
		}
		
		if (mex.getElement() != null) {
			newMex.setElement(mex.getElement());
		}
		
		if (mex.getEnclosingDefinition() != null) {
			newMex.setEnclosingDefinition(mex.getEnclosingDefinition());
		}
		
		if (mex.getExtensionAttributes() != null) {
			newMex.getExtensionAttributes().putAll(mex.getExtensionAttributes());
		}
		
		return newMex;
	}
	
	/**
	 * Get a new copy of the message part
	 * 
	 * @param origPart
	 * @return
	 */
	public static Part copyPart(Part origPart) {
		
		if (origPart == null) {
			throw new NullPointerException("argument is null.");
		}
		
		Part newPart = WSDLFactory.eINSTANCE.createPart();
		
		if (origPart.getName() != null) {
			newPart.setName(origPart.getName());
		}
		
		if (origPart.getTypeName() != null) {
			newPart.setTypeName(FragmentDuplicator.copyQName(origPart.getTypeName()));
		}
		
		if (origPart.getElementDeclaration() != null) {
			FragmentDuplicator.logger.warn("The message part \"" + origPart.getName() + "\" type is element declaration!");
		}
		
		return newPart;
	}
	
	/**
	 * Get a new copy of the given portType
	 * 
	 * @param portType
	 * @return
	 */
	public static PortType copyPortType(PortType portType) {
		
		if (portType == null) {
			throw new NullPointerException("argument is null.");
		}
		
		PortType newPortType = WSDLFactory.eINSTANCE.createPortType();
		
		newPortType.setQName(portType.getQName());
		
		List operations = portType.getOperations();
		Iterator iterator = operations.iterator();
		
		while (iterator.hasNext()) {
			Operation origOperation = (Operation) iterator.next();
			Operation newOperation = FragmentDuplicator.copyOperation(origOperation);
			newPortType.getOperations().add(newOperation);
		}
		
		return newPortType;
	}
	
	/**
	 * Get a new copy of the operation
	 * 
	 * @param origOperation
	 * @return
	 */
	public static Operation copyOperation(Operation origOperation) {
		
		if (origOperation == null) {
			throw new NullPointerException("argument is null.");
		}
		
		Operation newOperation = WSDLFactory.eINSTANCE.createOperation();
		
		newOperation.setName(origOperation.getName());
		if (origOperation.getInput() != null) {
			newOperation.setInput(FragmentDuplicator.copyInput(origOperation.getEInput()));
		}
		if (origOperation.getOutput() != null) {
			newOperation.setOutput(FragmentDuplicator.copyOutput(origOperation.getEOutput()));
		}
		
		return newOperation;
	}
	
	/**
	 * Copy operation regarding whether the fragment definition has already had
	 * the artifacts e.g. message in the operation.
	 * 
	 * @param origOperation
	 * @param fragDefn
	 * @return
	 */
	public static Operation copyOperation(Operation origOperation, Definition fragDefn) {
		if ((origOperation == null) || (fragDefn == null)) {
			throw new NullPointerException();
		}
		
		Operation newOperation = WSDLFactory.eINSTANCE.createOperation();
		
		newOperation.setName(origOperation.getName());
		
		Input origInput = (Input) origOperation.getInput();
		
		if (origInput != null) {
			Input newInput = WSDLFactory.eINSTANCE.createInput();
			newInput.setName(origInput.getName());
			
			Message origMessage = origInput.getEMessage();
			Message newMessage = MyWSDLUtil.resolveMessage(fragDefn, origMessage.getQName());
			
			if (newMessage == null) {
				throw new IllegalStateException("Can not find message in fragment definition: " + origMessage.getQName());
			}
			
			newInput.setMessage(newMessage);
			newOperation.setInput(newInput);
		}
		
		Output origOutput = (Output) origOperation.getOutput();
		
		if (origOperation.getOutput() != null) {
			Output newOutput = WSDLFactory.eINSTANCE.createOutput();
			newOutput.setName(origOutput.getName());
			
			Message origMessage = origOutput.getEMessage();
			Message newMessage = MyWSDLUtil.resolveMessage(fragDefn, origMessage.getQName());
			
			if (newMessage == null) {
				throw new IllegalStateException("Can not find message in fragment definition: " + origMessage.getQName());
			}
			
			newOutput.setEMessage(newMessage);
			newOperation.setOutput(newOutput);
		}
		
		return newOperation;
	}
	
	public static Input copyInput(Input input) {
		Input newInput = WSDLFactory.eINSTANCE.createInput();
		if (input.getName() != null) {
			newInput.setName(input.getName());
		}
		if (input.getEMessage() != null) {
			newInput.setMessage(FragmentDuplicator.copyMessage(input.getEMessage()));
		}
		return newInput;
	}
	
	public static Output copyOutput(Output output) {
		Output newOutput = WSDLFactory.eINSTANCE.createOutput();
		if (output.getName() != null) {
			newOutput.setName(output.getName());
		}
		if (output.getMessage() != null) {
			newOutput.setMessage(FragmentDuplicator.copyMessage(output.getEMessage()));
		}
		
		return newOutput;
	}
	
	/**
	 * Get a new copy of the given partnerLink, including the partnerLinkType,
	 * role, portType.
	 * 
	 * @param origPartnerLink
	 * @return
	 */
	public static PartnerLink copyPartnerLink(PartnerLink origPartnerLink) {
		
		if (origPartnerLink == null) {
			throw new NullPointerException("argument is null.");
		}
		
		PartnerLink newPartnerLink = BPELFactory.eINSTANCE.createPartnerLink();
		
		// set name
		newPartnerLink.setName(origPartnerLink.getName());
		
		// copy partnerlinkType
		PartnerLinkType oldPLT = origPartnerLink.getPartnerLinkType();
		PartnerLinkType newPLT = FragmentDuplicator.copyPartnerLinkType(oldPLT);
		newPartnerLink.setPartnerLinkType(newPLT);
		
		// myRole
		Role origMyRole = origPartnerLink.getMyRole();
		if (origMyRole != null) {
			String roleName = origMyRole.getName();
			Role newMyRole = MyWSDLUtil.findRole(newPLT, roleName);
			if (newMyRole == null) {
				throw new NullPointerException();
			}
			newPartnerLink.setMyRole(newMyRole);
		}
		
		// partnerRole
		Role origPartnerRole = origPartnerLink.getPartnerRole();
		if (origPartnerRole != null) {
			String roleName = origPartnerRole.getName();
			Role newPartnerRole = MyWSDLUtil.findRole(newPLT, roleName);
			if (newPartnerRole == null) {
				throw new NullPointerException();
			}
			newPartnerLink.setPartnerRole(newPartnerRole);
		}
		
		// initialise
		newPartnerLink.setInitializePartnerRole(origPartnerLink.getInitializePartnerRole());
		
		return newPartnerLink;
	}
	
	/**
	 * Get a new copy of the given partnerLinkType, including role, portType.
	 * 
	 * @param origPartnerLinkType
	 * @return
	 */
	public static PartnerLinkType copyPartnerLinkType(PartnerLinkType origPartnerLinkType) {
		
		if (origPartnerLinkType == null) {
			throw new NullPointerException("argument is null.");
		}
		
		PartnerLinkType newPLT = PartnerlinktypeFactory.eINSTANCE.createPartnerLinkType();
		
		newPLT.setName(origPartnerLinkType.getName());
		
		for (Role origRole : origPartnerLinkType.getRole()) {
			Role newRole = FragmentDuplicator.copyRole(origRole);
			newPLT.getRole().add(newRole);
		}
		
		return newPLT;
		
	}
	
	/**
	 * Get a copy of the original role, including the portType.
	 * 
	 * @param origRole
	 * @return
	 */
	public static Role copyRole(Role origRole) {
		
		Role newRole = PartnerlinktypeFactory.eINSTANCE.createRole();
		
		newRole.setName(origRole.getName());
		
		PortType origPT = (PortType) origRole.getPortType();
		PortType newPT = WSDLFactory.eINSTANCE.createPortType();
		
		newPT.setQName(FragmentDuplicator.copyQName(origPT.getQName()));
		newRole.setPortType(newPT);
		
		return newRole;
		
	}
	
	/**
	 * Get a new copy of the given correlationSet
	 * 
	 * @param origCorrelationSet
	 * @return
	 */
	public static CorrelationSet copyCorrelationSet(CorrelationSet origCorrelationSet) {
		
		if (origCorrelationSet == null) {
			throw new NullPointerException("argument is null.");
		}
		
		CorrelationSet newCorrelationSet = BPELFactory.eINSTANCE.createCorrelationSet();
		
		newCorrelationSet.setName(origCorrelationSet.getName());
		List<Property> origProperties = origCorrelationSet.getProperties();
		
		for (Property origProperty : origProperties) {
			newCorrelationSet.getProperties().add(FragmentDuplicator.copyProperty(origProperty));
		}
		return newCorrelationSet;
	}
	
	/**
	 * Get a copy of the given property
	 * 
	 * @param property
	 * @return
	 */
	public static Property copyProperty(Property property) {
		
		if (property == null) {
			throw new NullPointerException("argument is null.");
		}
		
		Property newProperty = MessagepropertiesFactory.eINSTANCE.createProperty();
		
		if (property.getName() != null) {
			newProperty.setName(property.getName());
		}
		
		if (property.getQName() != null) {
			newProperty.setQName(FragmentDuplicator.copyQName(property.getQName()));
		}
		
		newProperty.setRequired(property.getRequired());
		
		if (property.getElementType() != null) {
			newProperty.setElementType(FragmentDuplicator.copyQName(property.getElementType()));
		}
		
		if (property.getType() != null) {
			newProperty.setType(property.getType());
		}
		
		// if (property.getEnclosingDefinition() != null)
		// newProperty.setEnclosingDefinition(property.getEnclosingDefinition());
		
		return newProperty;
	}
	
	public static QName copyQName(QName qName) {
		
		if (qName == null) {
			throw new NullPointerException("argument is null.");
		}
		
		String uri = qName.getNamespaceURI();
		String localPart = qName.getLocalPart();
		String prefix = qName.getPrefix();
		
		QName newQName = new QName(uri, localPart, prefix);
		
		return newQName;
		
	}
	
	/**
	 * Get a new copy of the given variable, including the message.
	 * 
	 * @param variable
	 * @return
	 * @throws NullPointerException if argument is null
	 */
	public static Variable copyVariable(Variable variable) {
		
		if (variable == null) {
			throw new NullPointerException("argument is null.");
		}
		
		Element variableElement = variable.getElement();
		Variable newVariable = BPELFactory.eINSTANCE.createVariable();
		
		// name
		newVariable.setName(variable.getName());
		
		// xsd type
		if (variable.getType() != null) {
			XSDTypeDefinition type = XSDFactory.eINSTANCE.createXSDSimpleTypeDefinition();
			type.setName(variable.getType().getName());
			type.setTargetNamespace(variable.getType().getTargetNamespace());
			newVariable.setType(type);
		}
		
		// messageType
		Message origMsg = variable.getMessageType();
		if (origMsg != null) {
			if (origMsg instanceof MessageProxy) {
				QName qName = BPELUtils.createAttributeValue(variableElement, "messageType");
				Message messageType = new MessageProxy(((MessageProxy) origMsg).eProxyURI(), qName);
				newVariable.setMessageType(messageType);
			} else {
				newVariable.setMessageType(FragmentDuplicator.copyMessage(origMsg));
			}
			
		} else {
			FragmentDuplicator.logger.warn("Variable without messageType is used! Variable should be always of messageType");
		}
		
		// xsd element
		XSDElementDeclaration origElement = variable.getXSDElement();
		if (origElement != null) {
			// TODO copy the full element
			newVariable.setXSDElement(origElement);
		}
		
		// from
		if (variable.getFrom() != null) {
			// TODO inline from-spec is for variable initialisation
			FragmentDuplicator.logger.warn("from in variable: " + variable.getName() + " is not copied.");
		}
		
		return newVariable;
	}
	
}
