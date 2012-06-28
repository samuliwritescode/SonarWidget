package com.vaadin.sonarwidget;
import java.io.File;

import com.vaadin.Application;
import com.vaadin.ui.*;

public class SonarWidgetApplication extends Application {
	
	@Override
	public void init() {
		final Window mainWindow = new Window("Sonarwidget Application");

		final SonarWidget sonarWidget = new SonarWidget(new File("/Users/samuli/Documents/Sonar0011.slg"));

		sonarWidget.setHeight("400px");
		sonarWidget.setWidth("100%");
		
		mainWindow.addComponent(sonarWidget);		
		setMainWindow(mainWindow);	
	}
}
