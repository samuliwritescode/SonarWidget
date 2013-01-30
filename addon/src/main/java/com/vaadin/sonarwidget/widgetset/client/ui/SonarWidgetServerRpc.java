package com.vaadin.sonarwidget.widgetset.client.ui;

import com.vaadin.shared.communication.ServerRpc;

public interface SonarWidgetServerRpc extends ServerRpc {
    public void fetchSonarData(int height, int width, int index);
}
