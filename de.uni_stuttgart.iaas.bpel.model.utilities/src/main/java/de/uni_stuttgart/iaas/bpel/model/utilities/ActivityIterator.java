package org.bpel4chor.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Catch;
import org.eclipse.bpel.model.Else;
import org.eclipse.bpel.model.ElseIf;
import org.eclipse.bpel.model.FaultHandler;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.ForEach;
import org.eclipse.bpel.model.If;
import org.eclipse.bpel.model.OnAlarm;
import org.eclipse.bpel.model.OnMessage;
import org.eclipse.bpel.model.Pick;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.RepeatUntil;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.While;

/**
 * ActivityIterator recursively iterates through BPEL process as if the process
 * is a tree, and activity is node of the tree.
 * <p>
 * It iterates from the start activity of the given process down to the
 * children, until it can not find any more child.
 * <p>
 * BPEL 'Link' will not be concerned, be cause we just want the activity, then
 * how the activities are connected via link, is not ActivityIterator's concern.
 * 
 * @since Feb 11, 2012
 * @author Daojun Cui
 */
public class ActivityIterator implements Iterator<Activity> {

	protected Process processStartPoint = null;

	protected List<Activity> activityList = new ArrayList<Activity>();

	public ActivityIterator(Process startPoint) {
		if (startPoint == null)
			throw new NullPointerException();

		this.processStartPoint = startPoint;
		Activity firstAct = this.processStartPoint.getActivity();
		if (firstAct != null)
			activityList.add(0, firstAct);
	}

	@Override
	public boolean hasNext() {
		return activityList.isEmpty() ? false : true;
	}

	@Override
	public Activity next() {
		if (activityList.size() > 0) {
			// get the first element, remove it from the list and shift the rest
			// element left.
			Activity currentActivity = activityList.remove(0);

			// add children into list for next iteration
			addChildrenToList(currentActivity);

			return currentActivity;
		} else {
			return null;
		}
	}

	/**
	 * If you want to start over
	 */
	public void reset() {
		activityList = new ArrayList<Activity>();
		Activity firstAct = this.processStartPoint.getActivity();
		if (firstAct != null)
			activityList.add(0, firstAct);
	}

	@Override
	public void remove() {
	}

	/**
	 * Add children of the activity into the collection
	 * 
	 * @param act
	 *            The activity
	 */
	protected void addChildrenToList(Activity act) {
		if (act == null)
			throw new NullPointerException("argument is null.");

		if (act instanceof Flow) {
			addChildren((Flow) act);
		} else if (act instanceof If) {
			addChildren((If) act);
		} else if (act instanceof While) {
			addChildren((While) act);
		} else if (act instanceof Sequence) {
			addChildren((Sequence) act);
		} else if (act instanceof Pick) {
			addChildren((Pick) act);
		} else if (act instanceof Scope) {
			addChildren((Scope) act);
		} else if (act instanceof ForEach) {
			addChildren((ForEach) act);
		} else if (act instanceof RepeatUntil) {
			addChildren((RepeatUntil) act);
		}
	}

	protected void addChildren(Flow act) {
		List<Activity> children = act.getActivities();
		for (Activity child : children) {
			if (child != null)
				activityList.add(0, child);
		}

	}

	protected void addChildren(If act) {
		Activity actInIfBranch = act.getActivity();
		if (actInIfBranch != null)
			activityList.add(0, actInIfBranch);

		List<ElseIf> elseIfList = act.getElseIf();
		for (ElseIf elseIf : elseIfList) {
			Activity actInElseIfBranch = elseIf.getActivity();
			if (actInElseIfBranch != null)
				activityList.add(0, actInElseIfBranch);
		}

		Else elseBranch = act.getElse();
		if (elseBranch != null) {
			Activity actInElseBranch = elseBranch.getActivity();
			if (actInElseBranch != null)
				activityList.add(0, actInElseBranch);
		}
	}

	protected void addChildren(While act) {
		Activity child = act.getActivity();
		if (child != null) {
			activityList.add(0, child);
		}
	}

	protected void addChildren(Sequence act) {
		List<Activity> children = act.getActivities();
		for (Activity child : children) {
			if (child != null)
				activityList.add(0, child);
		}
	}

	protected void addChildren(Pick act) {
		List<OnMessage> onMsgs = act.getMessages();
		for (OnMessage onMsg : onMsgs) {
			Activity actInMsg = onMsg.getActivity();
			if (actInMsg != null)
				activityList.add(0, actInMsg);
		}

		List<OnAlarm> onAlarms = act.getAlarm();
		for (OnAlarm onAlarm : onAlarms) {
			Activity actInAlarm = onAlarm.getActivity();
			if (actInAlarm != null) {
				activityList.add(0, actInAlarm);
			}
		}
	}

	protected void addChildren(Scope act) {
		Activity actInScope = act.getActivity();
		if (actInScope != null)
			activityList.add(0, actInScope);
		FaultHandler fh = act.getFaultHandlers();
		if (fh != null) {
			for (Catch aCatch : fh.getCatch()) {
				activityList.add(0, aCatch.getActivity());
			}
		}
	}

	protected void addChildren(ForEach act) {
		Activity actInForEach = act.getActivity();
		if (actInForEach != null)
			activityList.add(0, actInForEach);
	}

	protected void addChildren(RepeatUntil act) {
		Activity actInReUntil = act.getActivity();
		if (actInReUntil != null)
			activityList.add(0, actInReUntil);
	}
}
