package org.esa.snap.rcp.statistics;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Marco Peters
 */
@SuppressWarnings({"unused", "FieldCanBeLocal"})
class MetadataPlotSettings {

    static final String FIELD_NAME_NONE = "None";
    static final String FIELD_NAME_RECORD_INDEX = "Record Index";
    static final String FIELD_NAME_ARRAY_FIELD_INDEX = "Array Field Index [n]";

    static final String PROP_NAME_METADATA_ELEMENT = "metadataElement";
    static final String PROP_NAME_RECORD_START_INDEX = "recordStartIndex";
    static final String PROP_NAME_RECORDS_PER_PLOT = "recordsPerPlot";
    static final String PROP_NAME_FIELD_X = "fieldX";
    static final String PROP_NAME_FIELD_Y1 = "fieldY1";
    static final String PROP_NAME_FIELD_Y2 = "fieldY2";

    private MetadataElement metadataElement;
    private double recordStartIndex = 1.0;
    private int recordsPerPlot = 1;
    private String fieldX;
    private String fieldY1;
    private String fieldY2;

    private BindingContext context;
    private AtomicBoolean isSynchronising = new AtomicBoolean(false);

    public MetadataPlotSettings() {
        context = new BindingContext(PropertyContainer.createObjectBacked(this, new ParameterDescriptorFactory()));
        Property propertyRecordStart = context.getPropertySet().getProperty(PROP_NAME_RECORD_START_INDEX);
        propertyRecordStart.getDescriptor().setAttribute("stepSize", 1);
        Property propertyMetaElement = context.getPropertySet().getProperty(PROP_NAME_METADATA_ELEMENT);
        propertyMetaElement.addPropertyChangeListener(evt -> {
            try {
                if (!isSynchronising.getAndSet(true)) {

                    PropertySet propertySet = context.getPropertySet();
                    // clear current settings
                    propertySet.setValue(PROP_NAME_RECORD_START_INDEX, 1.0);
                    propertySet.setValue(PROP_NAME_RECORDS_PER_PLOT, 1);
                    propertySet.setValue(PROP_NAME_FIELD_X, null);
                    propertySet.setValue(PROP_NAME_FIELD_Y1, null);
                    propertySet.setValue(PROP_NAME_FIELD_Y2, null);

                    List<String> usableFieldNames = retrieveUsableFieldNames(metadataElement);
                    ArrayList<String> usableYFieldNames = new ArrayList<>(usableFieldNames);
                    usableYFieldNames.add(0, FIELD_NAME_NONE);
                    PropertyDescriptor propertyFieldY1 = propertySet.getProperty(PROP_NAME_FIELD_Y1).getDescriptor();
                    propertyFieldY1.setValueSet(new ValueSet(usableYFieldNames.toArray(new String[0])));
                    PropertyDescriptor propertyFieldY2 = propertySet.getProperty(PROP_NAME_FIELD_Y2).getDescriptor();
                    propertyFieldY2.setValueSet(new ValueSet(usableYFieldNames.toArray(new String[0])));

                    PropertyDescriptor propertyFieldX = propertySet.getProperty(PROP_NAME_FIELD_X).getDescriptor();
                    ArrayList<String> usableXFieldNames = new ArrayList<>(usableFieldNames);
                    usableXFieldNames.add(0, FIELD_NAME_RECORD_INDEX);
                    usableXFieldNames.add(1, FIELD_NAME_ARRAY_FIELD_INDEX);
                    propertyFieldX.setValueSet(new ValueSet(usableXFieldNames.toArray(new String[0])));
                }
            } finally {
                isSynchronising.set(false);
            }

        });

    }

    /**
     * Retrieves the binding context, to be used to bind the UI elements to.
     */
    BindingContext getContext() {
        return context;
    }

    /**
     * Returns the currently selected metadata element
     */
    MetadataElement getMetadataElement() {
        return metadataElement;
    }

    /**
     * Name of the field to be used for the domain(X) axis.
     */
    String getNameX() {
        return fieldX;
    }

    /**
     * Name of the field to be used for the first range(Y) axis.
     */
    public String getNameY1() {
        return fieldY1;
    }

    /**
     * Name of the field to be used for the second range(Y) axis.
     */
    public String getFieldY2() {
        return fieldY2;
    }

    void setMetadataElements(MetadataElement[] elements) {
        if (elements == null) {
            context.getPropertySet().setDefaultValues();
            return;
        }
        Property property = context.getPropertySet().getProperty(PROP_NAME_METADATA_ELEMENT);
        property.getDescriptor().setValueSet(new ValueSet(filterElements(elements)));
        try {
            property.setValue(elements[0]);
        } catch (ValidationException e) {
            e.printStackTrace();
        }
    }

    int getNumRecords() {
        return getNumRecords(metadataElement);
    }

    int getRecordStartIndex() {
        return (int) recordStartIndex;
    }

    public int getRecordsPerPlot() {
        return recordsPerPlot;
    }

    static List<String> retrieveUsableFieldNames(MetadataElement element) {

        int numRecords = getNumRecords(element);
        if (numRecords > 1) {
            return retrieveUsableFieldNames(element.getElements()[0]);
        } else {
            List<String> list = new ArrayList<>();
            String[] attributeNames = element.getAttributeNames();
            for (String fullAttribName : attributeNames) {
                String fieldName = getFieldName(fullAttribName);
                if (list.contains(fieldName)) { // skip over split array attributes if already added
                    continue;
                }
                MetadataAttribute attribute = element.getAttribute(fullAttribName);
                if (isNumericType(attribute)) {
                    list.add(fieldName);
                }

            }
            return list;
        }


    }

    private static String getFieldName(String fullAttribName) {
        String fieldName;
        Pattern p = Pattern.compile("(.*)\\.(\\d+)");
        final Matcher m = p.matcher(fullAttribName);
        if (m.matches()) {
            fieldName = m.group(1);
        } else {
            fieldName = fullAttribName;
        }
        return fieldName;
    }

    private static boolean isNumericType(MetadataAttribute attribute) {
        return ProductData.isIntType(attribute.getDataType()) || ProductData.isFloatingPointType(attribute.getDataType());
    }

    private MetadataElement[] filterElements(MetadataElement[] elements) {
        return elements;
    }

    private static int getNumRecords(MetadataElement metadataElement) {
        if (metadataElement == null) {
            return 1;
        }
        int numSubElements = metadataElement.getNumElements();
        if (numSubElements > 0) {
            MetadataElement[] subElements = metadataElement.getElements();
            int count = 0;
            for (MetadataElement subElement : subElements) {
                if (subElement.getName().matches(metadataElement.getName() + "\\.\\d+")) {   // subelements should only have a number suffix
                    count++;
                }
            }
            if (count == numSubElements) {
                return count;
            }
        }
        return 1;
    }

}
