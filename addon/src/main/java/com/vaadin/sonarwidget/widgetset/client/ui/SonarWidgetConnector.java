package com.vaadin.sonarwidget.widgetset.client.ui;

import com.vaadin.client.communication.RpcProxy;
import com.vaadin.client.ui.LegacyConnector;
import com.vaadin.shared.ui.Connect;
import com.vaadin.sonarwidget.SonarWidget;

@Connect(SonarWidget.class)
public class SonarWidgetConnector extends LegacyConnector {

    SonarWidgetServerRpc rpc = RpcProxy.create(SonarWidgetServerRpc.class, this);
    
    public SonarWidgetConnector() {
        registerRpc(SonarWidgetClientRpc.class, new SonarWidgetClientRpc() {
            
            @Override
            public void frameData(int offset, long pingcount, String pic, String[] lowlimits, String[] depths, String[] temps) {
                String resourceUrl = getResourceUrl(pic);
                getWidget().getState().setPingCount(pingcount);
                getWidget().getState().setOffset(offset, resourceUrl, lowlimits, depths, temps);
            }
        });
        
        getWidget().getState().setConnector(this);
    }

    @Override
    public VSonarWidget getWidget() {
        return (VSonarWidget) super.getWidget();
    }
    
    public void getData(int height, int width, int index) {
        rpc.fetchSonarData(height, width, index);
    }
}
