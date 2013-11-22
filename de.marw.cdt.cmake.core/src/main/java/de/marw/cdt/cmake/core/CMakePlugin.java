// Copyright 2013 Martin Weber

package de.marw.cdt.cmake.core;

import java.text.MessageFormat;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * @author Martin Weber
 */
public class CMakePlugin extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "de.marw.cdt.cmake.core"; //$NON-NLS-1$
  /** extetion id of the cmake-generated makefile builder */
  public static final String BUILDER_ID = CMakePlugin.PLUGIN_ID
      + "." + "genmakebuilder"; //$NON-NLS-1$

  public static final String CMAKETOOLS_PROJECT_TYPE_ID = "de.marw.cdt.cmake.core.projectType"; //$NON-NLS-1$

  //The shared instance.
  private static CMakePlugin plugin;

  /**
   * The constructor.
   */
  public CMakePlugin() {
  }

  /**
   * This method is called upon plug-in activation
   */
  public void start(BundleContext context) throws Exception {
    if (!PLUGIN_ID.equals(this.getBundle().getSymbolicName()))
      throw new RuntimeException(
          "BUG: PLUGIN_ID does not match Bundle-SymbolicName");
    super.start(context);
    plugin = this;
  }

  /**
   * This method is called when the plug-in is stopped
   */
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    plugin = null;
  }

  /**
   * Returns the shared instance.
   */
  public static CMakePlugin getDefault() {
    return plugin;
  }

  /**
   * Returns the string from the plugin's resource bundle, or 'key' if not
   * found.
   *
   * @param key
   *        the message key
   * @return the resource bundle message
   */
  public static String getResourceString(String key) {
//		ResourceBundle bundle = CMakePlugin.getDefault().getResourceBundle();
//		try {
//			return bundle.getString(key);
//		} catch (MissingResourceException e) {
//			return key;
//		}
    return key;
  }

  /**
   * Returns the string from the plugin's resource bundle, or 'key' if not
   * found.
   *
   * @param key
   *        the message key
   * @param args
   *        an array of substituition strings
   * @return the resource bundle message
   */
  public static String getFormattedString(String key, String[] args) {
    return MessageFormat.format(getResourceString(key), (Object[]) args);
  }

}
