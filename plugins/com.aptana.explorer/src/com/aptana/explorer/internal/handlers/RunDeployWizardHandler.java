package com.aptana.explorer.internal.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.aptana.deploy.wizard.DeployWizard;

public class RunDeployWizardHandler extends AbstractHandler
{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();

		// Instantiates and initializes the wizard
		DeployWizard wizard = new DeployWizard();
		wizard.init(part.getSite().getWorkbenchWindow().getWorkbench(), (IStructuredSelection) part.getSite()
				.getSelectionProvider().getSelection());
		wizard.setWindowTitle(Messages.DeployHandler_Wizard_Title);

		// Instantiates the wizard container with the wizard and opens it
		Shell shell = part.getSite().getShell();
		if (shell == null)
		{
			shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		}
		WizardDialog dialog = new WizardDialog(shell, wizard);
		dialog.setPageSize(350, 500);
		dialog.setHelpAvailable(false);
		dialog.create();
		dialog.open();

		return null;
	}
}