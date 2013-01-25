package com.vaadin.sonarwidget.widgetset.client.ui;

import com.vaadin.client.ui.LegacyConnector;
import com.vaadin.shared.ui.Connect;
import com.vaadin.sonarwidget.SonarWidget;

@Connect(SonarWidget.class)
public class SonarWidgetConnector extends LegacyConnector {

    @Override
    public VSonarWidget getWidget() {
        return (VSonarWidget) super.getWidget();
    }
}
