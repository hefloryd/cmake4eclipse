/* ******************************************************************************
 * Copyright (c) 2021 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.marw.cmake4eclipse.mbs.nature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import de.marw.cmake4eclipse.mbs.internal.Activator;

/**
 * A project nature required to show a reduced MBS GUI to avoid end-user confusion on lots of things to configure which
 * have no effect when building with cmake.
 *
 * @author Martin Weber
 */
public class C4ENature implements IProjectNature {
  public static final String NATURE_ID = "de.marw.cmake4eclipse.mbs.cmake4eclipsenature"; // $NON-NLS-1$

  private IProject project;

  /**
   * Adds the natures to the specified project.
   *
   * @throws CoreException
   */
  public static void addNature(IProject project, IProgressMonitor monitor) throws CoreException {
    CProjectNature.addNature(project, NATURE_ID, monitor);
    CProjectNature.addCNature(project, monitor);
    CCProjectNature.addCCNature(project, monitor);
  }

  @Override
  public void configure() throws CoreException {
    IProgressMonitor monitor = new NullProgressMonitor();
    addBuilders(monitor);
  }

  private void addBuilders(IProgressMonitor monitor) throws CoreException {
    IProjectDescription description = project.getDescription();

    ICommand bldrCommand = description.newCommand();
    bldrCommand.setBuilderName(Activator.BUILDER_ID);
//    bldrCommand.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, false);

    List<ICommand> commands = new ArrayList<>(Arrays.asList(description.getBuildSpec()));
    commands.add(0, bldrCommand);

    ICommand[] cmds = commands.stream().distinct().toArray(ICommand[]::new);
    description.setBuildSpec(cmds);
    project.setDescription(description, monitor);
  }

  @Override
  public void deconfigure() throws CoreException {
    IProjectDescription description = project.getDescription();
    List<ICommand> commands = new ArrayList<>(Arrays.asList(description.getBuildSpec()));
    for (Iterator<ICommand> iter = commands.iterator(); iter.hasNext();) {
      ICommand iCommand = iter.next();
      if(Activator.BUILDER_ID.equals(iCommand.getBuilderName())) {
        iter.remove();
        break;
      }
    }
    ICommand[] cmds = commands.stream().distinct().toArray(ICommand[]::new);
    description.setBuildSpec(cmds);
    project.setDescription(description, null);
  }

  @Override
  public IProject getProject() {
    return project;
  }

  @Override
  public void setProject(IProject project) {
    this.project = project;
  }

}
