package org.vaadin.sonarwidget.widgetset.client.ui;

import com.vaadin.shared.communication.ClientRpc;

public interface SonarWidgetClientRpc extends ClientRpc {
    public void frameData(int offset, String pic, String[] lowlimits, String[] depths, String[] temps);
}
