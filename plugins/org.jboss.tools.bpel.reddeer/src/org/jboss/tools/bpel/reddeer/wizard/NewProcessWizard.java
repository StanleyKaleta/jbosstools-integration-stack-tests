package org.jboss.tools.bpel.reddeer.wizard;

import org.jboss.reddeer.jface.wizard.NewWizardDialog;
import org.jboss.reddeer.swt.impl.button.CheckBox;
import org.jboss.reddeer.swt.impl.combo.LabeledCombo;
import org.jboss.reddeer.swt.impl.text.LabeledText;
import org.jboss.reddeer.swt.impl.tree.DefaultTreeItem;

/**
 * 
 * @author apodhrad
 * 
 */
public class NewProcessWizard extends NewWizardDialog {

	public static final String TEMPLATE_SYNC = "Synchronous BPEL Process";
	public static final String TEMPLATE_ASYNC = "Asynchronous BPEL Process";
	public static final String TEMPLATE_EMPTY = "Empty BPEL Process";

	public static final String BINDING_SOAP = "SOAP";
	public static final String BINDING_HTTP = "HTTP";

	private String projectName;
	private String processName;
	private String serviceName;
	private String serviceAddress;
	private String portName;
	private String creationMode;
	private String bindingProtocol;
	private String namespace;
	private String template;
	private boolean isAbstract;

	public NewProcessWizard(String projectName, String processName) {
		super("BPEL 2.0", "BPEL Process File");
		this.projectName = projectName;
		this.processName = processName;
		this.namespace = "http://eclipse.org/bpel/sample";
		this.template = TEMPLATE_ASYNC;
		this.bindingProtocol = BINDING_SOAP;
		this.isAbstract = false;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void setServiceAddress(String serviceAddress) {
		this.serviceAddress = serviceAddress;
	}

	public void setPortName(String portName) {
		this.portName = portName;
	}

	public void setCreationMode(String creationMode) {
		this.creationMode = creationMode;
	}

	public void setBindingProtocol(String bindingProtocol) {
		this.bindingProtocol = bindingProtocol;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public NewProcessWizard setTemplate(String template) {
		this.template = template;
		return this;
	}

	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public NewProcessWizard setSyncTemplate() {
		return setTemplate(TEMPLATE_SYNC);
	}

	public NewProcessWizard setAsyncTemplate() {
		return setTemplate(TEMPLATE_ASYNC);
	}

	public NewProcessWizard setEmptyTemplate() {
		return setTemplate(TEMPLATE_EMPTY);
	}

	public void execute() {
		open();

		new LabeledText("Process Name:").setText(processName);
		new LabeledCombo("Namespace:").setText(namespace);
		new CheckBox("Abstract Process").toggle(isAbstract);

		next();

		new LabeledCombo("Template:").setSelection(template);

		if (!template.equals(TEMPLATE_EMPTY)) {
			new LabeledCombo("Binding Protocol:").setSelection(bindingProtocol);
		}

		next();

		new DefaultTreeItem(projectName, "bpelContent").select();

		finish();
	}
}
