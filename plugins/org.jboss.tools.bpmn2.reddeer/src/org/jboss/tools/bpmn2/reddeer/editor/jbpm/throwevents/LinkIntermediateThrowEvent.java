package org.jboss.tools.bpmn2.reddeer.editor.jbpm.throwevents;

import org.jboss.tools.bpmn2.reddeer.editor.Element;
import org.jboss.tools.bpmn2.reddeer.properties.shell.LinkEventTargetSetUpCTab;

public class LinkIntermediateThrowEvent extends IntermediateThrowEvent {

	public LinkIntermediateThrowEvent(String name) {
		super(name);
	}
	
	public LinkIntermediateThrowEvent(Element element) {
		super(element);
	}

	public void setTarget(String targetEventName, int targetEventIndexInCombo) {
		propertiesHandler.setUp(new LinkEventTargetSetUpCTab(targetEventName, targetEventIndexInCombo));
	}
}