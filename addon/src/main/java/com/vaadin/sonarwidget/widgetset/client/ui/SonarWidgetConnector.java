package com.vaadin.sonarwidget.widgetset.client.ui;

import com.vaadin.client.communication.RpcProxy;
import com.vaadin.client.ui.LegacyConnector;
import com.vaadin.shared.ui.Connect;
import com.vaadin.sonarwidget.SonarWidget;

@Connect(SonarWidget.class)
public class SonarWidgetConnector extends LegacyConnector {

    SonarWidgetRpc rpc = RpcProxy.create(SonarWidgetRpc.class, this);

    @Override
    public VSonarWidget getWidget() {
        return (VSonarWidget) super.getWidget();
    }
    
    public void getData(int height, int width, int index) {
        rpc.fetchSonarData(height, width, index);
    }
}
