package org.bpel4chor.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Catch;
import org.eclipse.bpel.model.CatchAll;
import org.eclipse.bpel.model.OnAlarm;
import org.eclipse.bpel.model.OnEvent;
import org.eclipse.bpel.model.Process;
import org.eclipse.emf.ecore.EObject;

/**
 * ExtendedActivityIterator recursively iterates through BPEL process as if the
 * process is a tree, and activity is node of the tree.
 * <p>
 * It iterates over possible FHs and EHs of the given process down to the
 * children, until it can not find any more child.
 * <p>
 * BPEL 'Link' will not be concerned, be cause we just want the activity, then
 * how the activities are connected via link, is not ActivityIterator's concern.
 * 
 * @since Oct 18, 2012
 * @author Peter Debicki
 */
public class ExtendedActivityIterator extends ActivityIterator {
	
	/** List of possible FaultHandlers */
	protected List<Catch> catchList = new ArrayList<Catch>();
	
	/** Possible CatchAll FaultHandler */
	protected CatchAll catchAll = null;
	
	/** List of possible EventHandler Alarms */
	protected List<OnAlarm> alarmList = new ArrayList<OnAlarm>();
	
	/** List of possible EventHandler Events */
	protected List<OnEvent> eventList = new ArrayList<OnEvent>();
	
	protected Activity lastActivity;
	
	protected EObject lastActContainer;
	
	
	public ExtendedActivityIterator(Process startPoint) {
		super(startPoint);
		
		Activity firstActivity = null;
		
		// Check if we have any FHs
		if ((this.processStartPoint.getFaultHandlers() != null)) {
			if (this.processStartPoint.getFaultHandlers().getCatch().size() > 0) {
				this.catchList.addAll(this.processStartPoint.getFaultHandlers().getCatch());
			} else if (this.processStartPoint.getFaultHandlers().getCatchAll() != null) {
				this.catchAll = this.processStartPoint.getFaultHandlers().getCatchAll();
			}
		}
		
		// Check if we have any EHs
		if ((this.processStartPoint.getEventHandlers() != null)) {
			if (this.processStartPoint.getEventHandlers().getAlarm().size() > 0) {
				this.alarmList.addAll(this.processStartPoint.getEventHandlers().getAlarm());
			}
			if (this.processStartPoint.getEventHandlers().getEvents().size() > 0) {
				this.eventList.addAll(this.processStartPoint.getEventHandlers().getEvents());
			}
		}
		
		// Now check if we have FHs or EHs and set the activity iterator to the
		// first one
		if (this.catchList.size() > 0) {
			firstActivity = this.catchList.get(0).getActivity();
		} else if (this.catchAll != null) {
			firstActivity = this.catchAll.getActivity();
		} else if (this.alarmList.size() > 0) {
			firstActivity = this.alarmList.get(0).getActivity();
		} else if (this.eventList.size() > 0) {
			firstActivity = this.eventList.get(0).getActivity();
		} else {
			// We set the iterator to the process activity
			firstActivity = this.processStartPoint.getActivity();
		}
		
		if (firstActivity != null) {
			this.lastActivity = firstActivity;
			this.lastActContainer = firstActivity.eContainer();
			this.activityList.add(0, firstActivity);
		}
	}
	
	@Override
	public boolean hasNext() {
		if (!this.activityList.isEmpty()) {
			return true;
		} else {
			// Check what type the container of the last activity was
			return false;
		}
	}
	
	private Activity checkForNextAct() {
		
		Activity newActivity = null;
		// if (this.lastActivity == this.processStartPoint.getActivity()) {
		// // We have already checked the last activity
		// return newActivity;
		// }
		// Check of what type the container of the last activity was
		if (this.lastActContainer instanceof Catch) {
			// If we have a catch, there couldn't be any catchAll
			int pos = this.catchList.indexOf((this.lastActContainer));
			if ((this.catchList.size() + 1) > pos) {
				newActivity = this.catchList.get(pos + 1).getActivity();
			} else {
				// Check if there are some EHs
				if (this.alarmList.size() > 0) {
					newActivity = this.alarmList.get(0).getActivity();
				} else if (this.eventList.size() > 0) {
					newActivity = this.eventList.get(0).getActivity();
				} else {
					newActivity = this.processStartPoint.getActivity();
				}
			}
		} else if (this.lastActContainer instanceof CatchAll) {
			// If we have a catchAll, there couldn't be any catch
			if (this.alarmList.size() > 0) {
				newActivity = this.alarmList.get(0).getActivity();
			} else if (this.eventList.size() > 0) {
				newActivity = this.eventList.get(0).getActivity();
			} else {
				newActivity = this.processStartPoint.getActivity();
			}
			
		} else if (this.lastActContainer instanceof OnAlarm) {
			int pos = this.alarmList.indexOf(this.lastActContainer);
			if ((this.alarmList.size() + 1) > pos) {
				newActivity = this.alarmList.get(pos + 1).getActivity();
			} else if (this.eventList.size() > 0) {
				newActivity = this.eventList.get(0).getActivity();
			} else {
				newActivity = this.processStartPoint.getActivity();
			}
		} else if (this.lastActContainer instanceof OnEvent) {
			int pos = this.eventList.indexOf(this.lastActContainer);
			if ((this.eventList.size() + 1) > pos) {
				newActivity = this.eventList.get(pos + 1).getActivity();
			} else {
				newActivity = this.processStartPoint.getActivity();
			}
		}
		return newActivity;
	}
	
	@Override
	public Activity next() {
		if (this.activityList.size() > 0) {
			
			// get the first element, remove it from the list and shift the rest
			// element left.
			Activity currentActivity = this.activityList.remove(0);
			
			// add children into list for next iteration
			this.addChildrenToList(currentActivity);
			
			return currentActivity;
		} else {
			Activity next = this.checkForNextAct();
			if (next != null) {
				this.lastActivity = next;
				this.lastActContainer = next.eContainer();
				this.activityList.add(0, next);
			}
			return next;
		}
	}
}
