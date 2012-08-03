package com.vaadin.sonarwidget;
import java.io.File;

import com.vaadin.Application;
import com.vaadin.ui.*;

public class SonarWidgetApplication extends Application {
	
	@Override
	public void init() {
		final Window mainWindow = new Window("Sonarwidget Application");

		final SonarWidget sonarWidget = new SonarWidget(new File("/Users/samuli/sonar/Sonar0001.sl2"));

		sonarWidget.setHeight("600px");
		sonarWidget.setWidth("100%");
		
		mainWindow.addComponent(sonarWidget);		
		setMainWindow(mainWindow);	
	}
}
