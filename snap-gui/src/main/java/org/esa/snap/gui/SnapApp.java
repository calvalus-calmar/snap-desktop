package org.esa.snap.gui;

import com.bc.ceres.jai.operator.ReinterpretDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.PropertyMap;
import org.esa.snap.gui.util.CompatiblePropertyMap;
import org.esa.snap.tango.TangoIcons;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.NotificationDisplayer;
import org.openide.awt.StatusDisplayer;
import org.openide.modules.OnStart;
import org.openide.modules.OnStop;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OnShowing;
import org.openide.windows.OutputWriter;
import org.openide.windows.WindowManager;

import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static org.openide.util.NbBundle.Messages;

/**
 * The central SNAP application class (dummy).
 *
 * @author Norman Fomferra
 */
@SuppressWarnings("UnusedDeclaration")
public class SnapApp {

    private final static Logger LOG = Logger.getLogger(SnapApp.class.getName());

    private static SnapApp instance;

    protected SnapApp() {
    }

    public static SnapApp getInstance() {
        return instance;
    }

    protected static void setInstance(SnapApp instance) {
        SnapApp.instance = instance;
    }

    public Frame getMainFrame() {
        return WindowManager.getDefault().getMainWindow();
    }

    public ProductNode getSelectedProductNode() {
        return Utilities.actionsGlobalContext().lookup(ProductNode.class);
    }

    public void setStatusBarMessage(String message) {
        StatusDisplayer.getDefault().setStatusText(message);
    }

    /**
     * @return The (display) name of this application.
     * @deprecated use {@link #getInstanceName()}
     */
    @Deprecated
    public String getAppName() {
        return getInstanceName();
    }

    public String getInstanceName() {
        return NbBundle.getBundle("org.netbeans.core.ui.Bundle").getString("LBL_ProductInformation");
    }

    public void showOutOfMemoryErrorDialog(String message) {
        showErrorDialog("Out of Memory", message);
    }

    public void showErrorDialog(String title, String message) {
        NotifyDescriptor nd = new NotifyDescriptor(message,
                                                   title,
                                                   JOptionPane.OK_OPTION,
                                                   NotifyDescriptor.ERROR_MESSAGE,
                                                   null,
                                                   null);
        DialogDisplayer.getDefault().notify(nd);

        ImageIcon icon = TangoIcons.status_dialog_error(TangoIcons.Res.R16);
        JLabel balloonDetails = new JLabel(message);
        JButton popupDetails = new JButton("Call ESA");
        NotificationDisplayer.getDefault().notify(title,
                                                  icon,
                                                  balloonDetails,
                                                  popupDetails,
                                                  NotificationDisplayer.Priority.HIGH,
                                                  NotificationDisplayer.Category.ERROR);
    }

    /**
     * @return The user's application preferences.
     */
    public Preferences getPreferences() {
        return NbPreferences.forModule(getClass());
    }

    /**
     * @return The user's application preferences.
     * @deprecated this is for compatibility only, use #getPreferences()
     */
    @Deprecated
    public PropertyMap getCompatiblePreferences() {
        return new CompatiblePropertyMap(getPreferences());
    }

    public Logger getLogger() {
        return LOG;
    }

    /**
     * @deprecated Should be superfluous now. Kept for compatibility reasons only. Remove ASAP and latest before 2.0 release.
     */
    @Deprecated
    public void updateState() {
    }

    public void handleError(String message, Throwable t) {
        if (t != null) {
            t.printStackTrace();
        }
        showErrorDialog(getInstanceName() + " - Error", message);
        getLogger().log(Level.SEVERE, message, t);
    }

    public Product getSelectedProduct() {
        ProductSceneView productSceneView = getSelectedProductSceneView();
        if (productSceneView != null) {
            return productSceneView.getProduct();
        }
        ProductNode productNode = Utilities.actionsGlobalContext().lookup(ProductNode.class);
        if (productNode != null) {
            return productNode.getProduct();
        }
        return null;
    }

    public ProductSceneView getSelectedProductSceneView() {
        return Utilities.actionsGlobalContext().lookup(ProductSceneView.class);
    }

    @Messages("LBL_Question=Question")
    public int showQuestionDialog(String message, String preferencesKey) {
        return showQuestionDialog("Question", message, preferencesKey); /*I18N*/
    }

    public int showQuestionDialog(String title, String message, String preferencesKey) {
        return showQuestionDialog(title, message, false, preferencesKey);
    }

    @Messages("LBL_QuestionRemember=Remember my decision and don't ask again.")
    public int showQuestionDialog(String title, String message, boolean allowCancel, String preferencesKey) {
        Object result;
        boolean storeResult;
        if (preferencesKey != null) {
            String decision = getPreferences().get(preferencesKey + ".confirmed", "");
            if (decision.equals("yes")) {
                return JOptionPane.YES_OPTION;
            } else if (decision.equals("no")) {
                return JOptionPane.NO_OPTION;
            }
            JPanel panel = new JPanel(new BorderLayout(4, 4));
            panel.add(new JLabel(message), BorderLayout.CENTER);
            JCheckBox decisionCheckBox = new JCheckBox("Remember my decision and don't ask again.", false);
            panel.add(decisionCheckBox, BorderLayout.SOUTH);
            NotifyDescriptor d = new NotifyDescriptor.Confirmation(panel, getInstanceName() + " - " + title, allowCancel ? NotifyDescriptor.YES_NO_CANCEL_OPTION : NotifyDescriptor.YES_NO_OPTION);
            result = DialogDisplayer.getDefault().notify(d);
            storeResult = decisionCheckBox.isSelected();
        } else {
            NotifyDescriptor d = new NotifyDescriptor.Confirmation(message, getInstanceName() + " - " + title, allowCancel ? NotifyDescriptor.YES_NO_CANCEL_OPTION : NotifyDescriptor.YES_NO_OPTION);
            result = DialogDisplayer.getDefault().notify(d);
            storeResult = false;
        }
        if (NotifyDescriptor.YES_OPTION.equals(result)) {
            if (storeResult) {
                getPreferences().put(preferencesKey + ".confirmed", "yes");
            }
            return JOptionPane.YES_OPTION;
        } else if (NotifyDescriptor.NO_OPTION.equals(result)) {
            if (storeResult) {
                getPreferences().put(preferencesKey + ".confirmed", "no");
            }
            return JOptionPane.NO_OPTION;
        } else {
            return JOptionPane.CANCEL_OPTION;
        }
    }

    @Messages("LBL_Information=Information")
    public final void showInfoDialog(String message, String preferencesKey) {
        showInfoDialog("Information", message, preferencesKey);
    }

    public final void showInfoDialog(String title, String message, String preferencesKey) {
        showMessageDialog(title, message, JOptionPane.INFORMATION_MESSAGE, preferencesKey);
    }

    public final void showMessageDialog(String title, String message, int messageType, String preferencesKey) {
        if (preferencesKey != null) {
            String decision = getPreferences().get(preferencesKey + ".dontShow", "");
            if (decision.equals("true")) {
                return;
            }
            JPanel panel = new JPanel(new BorderLayout(4, 4));
            panel.add(new JLabel(message), BorderLayout.CENTER);
            JCheckBox dontShowCheckBox = new JCheckBox("Don't show this message anymore.", false);
            panel.add(dontShowCheckBox, BorderLayout.SOUTH);
            NotifyDescriptor d = new NotifyDescriptor(panel, getInstanceName() + " - " + title, NotifyDescriptor.DEFAULT_OPTION, messageType, null, null);
            DialogDisplayer.getDefault().notify(d);
            boolean storeResult = dontShowCheckBox.isSelected();
            if (storeResult) {
                getPreferences().put(preferencesKey + ".dontShow", "true");
            }
        } else {
            NotifyDescriptor d = new NotifyDescriptor(message, getInstanceName() + " - " + title, NotifyDescriptor.DEFAULT_OPTION, messageType, null, null);
            DialogDisplayer.getDefault().notify(d);
        }
    }

    private static GlobalSelectionLogger globalSelectionLogger;

    /**
     * {@code @OnStart}: {@code Runnable}s defined by various modules are invoked in parallel and as soon
     * as possible. It is guaranteed that execution of all {@code runnable}s is finished
     * before the startup sequence is claimed over.
     */
    @OnStart
    public static class StartOp implements Runnable {

        @Override
        public void run() {
            LOG.info(">>> " + getClass() + " called");
            setInstance(new SnapApp());
            initJAI();

            globalSelectionLogger = new GlobalSelectionLogger();
        }
    }

    /**
     * {@code @OnShowing}: Annotation to place on a {@code Runnable} with default constructor which should be invoked as soon as the window
     * system is shown. The {@code Runnable}s are invoked in AWT event dispatch thread one by one
     */
    @OnShowing
    public static class ShowingOp implements Runnable {

        @Override
        public void run() {
            LOG.info(getClass() + " called");
        }
    }

    /**
     * {@code @OnStop}: Annotation that can be applied to {@code Runnable} or {@code Callable<Boolean>}
     * subclasses with default constructor which will be invoked during shutdown sequence or when the
     * module is being shutdown.
     * <p>
     * First of all call {@code Callable}s are consulted to allow or deny proceeding with the shutdown.
     * <p>
     * If the shutdown is approved, all {@code Runnable}s registered are acknowledged and can perform the shutdown
     * cleanup. The {@code Runnable}s are invoked in parallel. It is guaranteed their execution is finished before
     * the shutdown sequence is over.
     */
    @OnStop
    public static class MaybeStopOp implements Callable {

        @Override
        public Boolean call() {
            Frame mainWindow = getInstance().getMainFrame();
            if (mainWindow == null || !mainWindow.isShowing()) {
                return true;
            }
            LOG.info(">>> " + getClass() + " called");
            ActionListener actionListener = (ActionEvent e) -> LOG.info(">>> " + getClass() + " action called");
            JLabel label = new JLabel("<html>SNAP found some cached <b>bazoo files</b> in your <b>gnarz folder</b>.<br>" +
                                      "Should they be rectified now?");
            JPanel panel = new JPanel();
            panel.setBorder(new EmptyBorder(10, 10, 10, 10));
            panel.add(label);
            DialogDescriptor dialogDescriptor = new DialogDescriptor(
                    panel,
                    "Confirm",
                    true,
                    DialogDescriptor.YES_NO_CANCEL_OPTION,
                    null,
                    actionListener);
            Dialog dialog = DialogDisplayer.getDefault().createDialog(dialogDescriptor, mainWindow);
            dialog.setVisible(true);
            Object value = dialogDescriptor.getValue();
            return !new Integer(2).equals(value);
        }
    }

    @OnStop
    public static class StopOp implements Runnable {

        @Override
        public void run() {
            LOG.info(">>> " + getClass() + " called");
            // do some cleanup
            setInstance(null);

            globalSelectionLogger = null;
        }
    }

    private static void initJAI() {
        // Disable native libraries for JAI:
        // This suppresses ugly (and harmless) JAI error messages saying that a JAI is going to
        // continue in pure Java mode.
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");

        // Set JAI tile scheduler parallelism
        int processorCount = Runtime.getRuntime().availableProcessors();
        int parallelism = Integer.getInteger("snap.jai.parallelism", processorCount);
        JAI.getDefaultInstance().getTileScheduler().setParallelism(parallelism);
        LOG.info(MessageFormat.format(">>> JAI tile scheduler parallelism set to {0}", parallelism));

        // Load JAI registry files.
        // For some reason registry file loading must be done in this order: first our own, then JAI's descriptors (nf)
        loadJaiRegistryFile(ReinterpretDescriptor.class, "/META-INF/registryFile.jai");
        loadJaiRegistryFile(JAI.class, "/META-INF/javax.media.jai.registryFile.jai");
    }

    private static void loadJaiRegistryFile(Class<?> cls, String jaiRegistryPath) {
        LOG.info("Reading JAI registry file from " + jaiRegistryPath);
        // Must use a new operation registry in order to register JAI operators defined in Ceres and BEAM
        OperationRegistry operationRegistry = OperationRegistry.getThreadSafeOperationRegistry();
        InputStream is = cls.getResourceAsStream(jaiRegistryPath);
        if (is != null) {
            final PrintStream oldErr = System.err;
            try {
                // Suppress annoying and harmless JAI error messages saying that a descriptor is already registered.
                System.setErr(new PrintStream(new ByteArrayOutputStream()));
                operationRegistry.updateFromStream(is);
                operationRegistry.registerServices(cls.getClassLoader());
                JAI.getDefaultInstance().setOperationRegistry(operationRegistry);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, MessageFormat.format("Error loading {0}: {1}", jaiRegistryPath, e.getMessage()), e);
            } finally {
                System.setErr(oldErr);
            }
        } else {
            LOG.warning(MessageFormat.format("{0} not found", jaiRegistryPath));
        }
    }


    private static class GlobalSelectionLogger implements LookupListener {
        private final DateFormat FORMAT = new SimpleDateFormat("HH:mm:ss");
        private final Lookup.Result<Object> selectionResult;
        private final OutputWriter writer;

        private GlobalSelectionLogger() {
            InputOutput io = IOProvider.getDefault().getIO("Global Selection", true);
            writer = io.getOut();
            selectionResult = Utilities.actionsGlobalContext().lookupResult(Object.class);
            selectionResult.addLookupListener(WeakListeners.create(LookupListener.class, this, selectionResult));
        }

        @Override
        public void resultChanged(LookupEvent lookupEvent) {
            String timeStamp = FORMAT.format(System.currentTimeMillis());
            writer.println(String.format("%s: selection changed:", timeStamp));
            for (Object selection : selectionResult.allInstances()) {
                writer.println(String.format("  type %s: %s", selection.getClass().getName(), selection));
            }
        }
    }
}
