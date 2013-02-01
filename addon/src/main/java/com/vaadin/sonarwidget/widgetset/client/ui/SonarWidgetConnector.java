package com.vaadin.sonarwidget.widgetset.client.ui;

import com.google.gwt.user.client.Timer;
import com.vaadin.client.communication.RpcProxy;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;
import com.vaadin.sonarwidget.SonarWidget;

@Connect(SonarWidget.class)
public class SonarWidgetConnector extends AbstractComponentConnector {

    private static final long serialVersionUID = 1L;
    SonarWidgetServerRpc rpc = RpcProxy.create(SonarWidgetServerRpc.class, this);
    
    public SonarWidgetConnector() {
        registerRpc(SonarWidgetClientRpc.class, new SonarWidgetClientRpc() {
            private static final long serialVersionUID = 1L;

            @Override
            public void frameData(int offset, String pic, String[] lowlimits, String[] depths, String[] temps) {
                String resourceUrl = getResourceUrl(pic);
                getWidget().getState().setOffset(offset, resourceUrl, lowlimits, depths, temps);
            }
        });
        
        getWidget().getState().setConnector(this);
        
        new Timer() {

            @Override
            public void run() {
                getWidget().getState().fetchInitialData();                
            }}.schedule(1000);
    }

    @Override
    public VSonarWidget getWidget() {
        return (VSonarWidget) super.getWidget();
    }
    
    @Override
    public SonarWidgetState getState() {
        return (SonarWidgetState) super.getState();
    }
    
    @Override
    public void onStateChanged(StateChangeEvent event) {
        super.onStateChanged(event);
        getWidget().getState().setPingCount(getState().pingCount);
        getWidget().getState().setColor(getState().color);
        getWidget().getState().setSideScan(getState().sidescan);
        getWidget().getState().setOverlay(getState().overlay);
    }
    
    public void getData(int height, int width, int index) {
        rpc.fetchSonarData(height, width, index);
    }
}
