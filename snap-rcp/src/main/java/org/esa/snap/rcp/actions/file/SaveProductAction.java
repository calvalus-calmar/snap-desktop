/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.rcp.actions.file;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.dataio.dimap.DimapProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.netbeans.api.progress.BaseProgressUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;

/**
 * Action which closes a selected product.
 *
 * @author Norman
 */
@ActionID(category = "File", id = "SaveProductAction")
@ActionRegistration(displayName = "#CTL_SaveProductActionName")
@ActionReferences({
        @ActionReference(path = "Menu/File", position = 40, separatorBefore = 38),
        @ActionReference(path = "Context/Product/Product", position = 90)
})
@NbBundle.Messages({"CTL_SaveProductActionName=Save Product"})
public final class SaveProductAction extends AbstractAction{


    private WeakReference<ProductNode> productNodeRef;

    public SaveProductAction(ProductNode productNode) {
        productNodeRef = new WeakReference<>(productNode);
    }

    static Boolean saveProduct(Product product) {
        Assert.notNull(product.getFileLocation());
        final File file = product.getFileLocation();
        if (file.isFile() && !file.canWrite()) {
            Dialogs.showWarning(Bundle.CTL_SaveProductActionName(),
                                MessageFormat.format("The product\n" +
                                                     "''{0}''\n" +
                                                     "exists and cannot be overwritten, because it is read only.\n" +
                                                     "Please choose another file or remove the write protection.",
                                                     file.getPath()),
                                null);
            return false;
        }

        SnapApp.getDefault().setStatusBarMessage(MessageFormat.format("Writing product ''{0}'' to {1}...", product.getDisplayName(), file));

        boolean incremental = true;
        WriteProductOperation operation = new WriteProductOperation(product, incremental);
        BaseProgressUtils.runOffEventThreadWithProgressDialog(operation,
                                                              Bundle.CTL_SaveProductActionName(),
                                                              operation.getProgressHandle(),
                                                              true,
                                                              50,
                                                              1000);

        SnapApp.getDefault().setStatusBarMessage("");

        return operation.getStatus();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        execute();
    }

    /**
     * Executes the action command.
     *
     * @return {@code Boolean.TRUE} on success, {@code Boolean.FALSE} on failure, or {@code null} on cancellation.
     */
    public Boolean execute() {
        ProductNode productNode = productNodeRef.get();
        if (productNode != null && productNode.getProduct() != null) {
            Product product = productNode.getProduct();
            if (product != null) {
                if (product.getFileLocation() != null && (product.getProductReader() == null || product.getProductReader() instanceof DimapProductReader)) {
                    return saveProduct(product);
                } else {
                    // if file location not set, delegate to save-as
                    return new SaveProductAsAction(product).execute();
                }
            } else {
                // reference was garbage collected, that's fine, no need to save.
                return true;
            }
        }
        return true;
    }


}
