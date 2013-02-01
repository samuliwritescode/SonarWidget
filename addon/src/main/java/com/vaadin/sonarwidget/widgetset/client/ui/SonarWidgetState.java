package com.vaadin.sonarwidget.widgetset.client.ui;

import com.vaadin.shared.AbstractComponentState;

public class SonarWidgetState extends AbstractComponentState {
    private static final long serialVersionUID = 1L;

    public int color;
    public boolean overlay;
    public boolean sidescan;
    public long pingCount;
}
