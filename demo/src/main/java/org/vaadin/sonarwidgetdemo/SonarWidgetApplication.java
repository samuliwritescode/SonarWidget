package org.vaadin.sonarwidgetdemo;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.vaadin.sonarwidget.SonarWidget;
import org.vaadin.sonarwidget.SonarWidget.SelectedPingEvent;
import org.vaadin.sonarwidget.SonarWidget.SelectedPingListener;
import org.vaadin.sonarwidget.data.Ping;
import org.vaadin.sonarwidget.data.Sonar.Type;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.tapio.googlemaps.GoogleMap;
import com.vaadin.tapio.googlemaps.client.LatLon;
import com.vaadin.tapio.googlemaps.client.overlays.GoogleMapMarker;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Slider;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class SonarWidgetApplication extends UI {
    private String selectedFile = "";
    private boolean overlay = false;
    private int colorbits = 0;
    private VerticalLayout sonarLayout;
    private float range;
    private GoogleMap mapsComponent;

    @Override
    protected void init(VaadinRequest request) {
        VerticalLayout layout = new VerticalLayout();
        HorizontalLayout controlLayout = new HorizontalLayout();
        sonarLayout = new VerticalLayout();
        sonarLayout.setSizeFull();
        ComboBox selector = new ComboBox("Select file");
        ComboBox rangeSelector = new ComboBox("Range by select");
        Slider rangeSlider = new Slider("Range by slider");
        CheckBox overlayCheck = new CheckBox("Overlay");
        OptionGroup colorsettings = new OptionGroup("Color settings");

        controlLayout.setSpacing(true);

        colorsettings.addItem(new Integer(0));

        colorsettings.addItem(SonarWidget.COLOR_BLUE | SonarWidget.COLOR_GREEN);

        colorsettings.addItem(SonarWidget.COLOR_BLUE | SonarWidget.COLOR_GREEN
                | SonarWidget.COLOR_RED | SonarWidget.COLOR_MAPCOLORS);

        colorsettings.addItem(SonarWidget.COLOR_BLUE | SonarWidget.COLOR_GREEN
                | SonarWidget.COLOR_RED | SonarWidget.COLOR_INVERSE);

        colorsettings.addItem(SonarWidget.COLOR_BLUE | SonarWidget.COLOR_GREEN
                | SonarWidget.COLOR_RED | SonarWidget.COLOR_MORECONTRAST);

        colorsettings.addItem(SonarWidget.COLOR_BLUE | SonarWidget.COLOR_GREEN
                | SonarWidget.COLOR_RED | SonarWidget.COLOR_MORECONTRAST
                | SonarWidget.COLOR_CONTRASTBOOST);

        colorsettings.addItem(SonarWidget.COLOR_BLUE | SonarWidget.COLOR_GREEN
                | SonarWidget.COLOR_RED | SonarWidget.COLOR_LESSCONTRAST);

        colorsettings.addItem(SonarWidget.COLOR_BLUE | SonarWidget.COLOR_GREEN
                | SonarWidget.COLOR_RED | SonarWidget.COLOR_LESSCONTRAST
                | SonarWidget.COLOR_CONTRASTBOOST);

        colorsettings.setItemCaption(0, "BW");
        colorsettings.setItemCaption(6, "Bluish");
        colorsettings.setItemCaption(23, "Mapped colors");
        colorsettings.setItemCaption(15, "Inverse BW");
        colorsettings.setItemCaption(39, "More contrast");
        colorsettings.setItemCaption(167, "Even more contrast");
        colorsettings.setItemCaption(71, "Less contrast");
        colorsettings.setItemCaption(199, "Even less contrast");
        colorsettings.setValue(0);
        selector.setImmediate(true);
        selector.setNullSelectionAllowed(false);
        selector.addItem("SideScan Sonar0001.sl2");
        selector.addItem("DownScan Sonar0001.sl2");
        selector.addItem("2D Sonar0011.slg");
        selector.addItem("SideScan R00001.DAT");
        selector.addItem("DownScan R00001.DAT");
        selector.addValueChangeListener(new Property.ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
                selectedFile = (String) event.getProperty().getValue();
                drawSonarWidget();
            }
        });

        rangeSlider.setWidth("150px");
        rangeSlider.setMin(1);
        rangeSlider.setMax(120);
        rangeSlider.setImmediate(true);
        rangeSlider.addValueChangeListener(new ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
                range = ((Double) event.getProperty().getValue()).floatValue();
                reDrawSonarWidget();
            }
        });
        rangeSelector.setImmediate(true);
        rangeSelector.setNullSelectionAllowed(false);
        rangeSelector.addItem(0f);
        rangeSelector.addItem(3.0f);
        rangeSelector.addItem(10.0f);
        rangeSelector.addItem(20.0f);
        rangeSelector.addItem(40.0f);
        rangeSelector.addItem(60.0f);
        rangeSelector.addItem(80.0f);
        rangeSelector.setValue(0f);
        rangeSelector.setItemCaption(0f, "Auto");
        rangeSelector.addValueChangeListener(new ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
                range = (Float) event.getProperty().getValue();
                reDrawSonarWidget();
            }
        });

        overlayCheck.setImmediate(true);
        overlayCheck.addValueChangeListener(new Property.ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
                overlay = (Boolean) event.getProperty().getValue();
                reDrawSonarWidget();
            }
        });

        colorsettings.setImmediate(true);
        colorsettings
                .addValueChangeListener(new Property.ValueChangeListener() {

                    @Override
                    public void valueChange(ValueChangeEvent event) {
                        colorbits = (Integer) event.getProperty().getValue();
                        reDrawSonarWidget();
                    }
                });

        selector.select("2D Sonar0011.slg");

        mapsComponent = new GoogleMap("apiKey", null, "english");
        mapsComponent.setWidth("100%");
        mapsComponent.setZoom(16);

        controlLayout.addComponent(selector);
        controlLayout.addComponent(overlayCheck);
        controlLayout.addComponent(colorsettings);
        controlLayout.addComponent(rangeSelector);
        controlLayout.addComponent(rangeSlider);
        controlLayout.addComponent(mapsComponent);
        controlLayout.setExpandRatio(mapsComponent, 1.0f);
        controlLayout.setWidth("100%");

        layout.addComponent(controlLayout);
        layout.addComponent(sonarLayout);
        layout.setSizeFull();
        layout.setExpandRatio(controlLayout, 0.0f);
        layout.setExpandRatio(sonarLayout, 1.0f);
        setContent(layout);
    }

    private void drawSonarWidget() {
        sonarLayout.removeAllComponents();
        Pattern pattern = Pattern.compile("\\S+");
        Matcher matcher = pattern.matcher(selectedFile);
        matcher.find();
        Type type = matcher.group().equalsIgnoreCase("SideScan") ? Type.eSideScan
                : Type.eDownScan;
        matcher.find();
        String filename = matcher.group();

        SonarWidget sonarWidget = new SonarWidget(new File(
                "/Users/cape/Code/sonar/" + filename), type);

        sonarWidget.addSelectedPingListener(new SelectedPingListener() {

            @Override
            public void selectPing(SelectedPingEvent event) {
                try {
                    Ping ping = event.getPing();
                    LatLon pos = new LatLon(ping.getLatitude(), ping
                            .getLongitude());
                    mapsComponent.setCenter(pos);
                    mapsComponent.addMarker(new GoogleMapMarker("Depth: "
                            + ping.getDepth(), pos,
                            false));
                } catch (IOException e) {
                    // Can't do nothing about it.
                    e.printStackTrace();
                }
            }
        });
        sonarWidget.setSizeFull();
        sonarLayout.addComponent(sonarWidget);
        reDrawSonarWidget();
    }

    private void reDrawSonarWidget() {
        if (sonarLayout.getComponentCount() > 0) {
            SonarWidget sonarWidget = (SonarWidget) sonarLayout.getComponent(0);
            sonarWidget.setColor(colorbits);
            sonarWidget.setOverlay(overlay);
            sonarWidget.setRange(range);
        }
    }
}
