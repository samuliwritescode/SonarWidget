package com.example.openlayersexperiment;
import com.vaadin.Application;
import com.vaadin.ui.*;

public class OpenlayersexperimentApplication extends Application {
	private HorizontalLayout layout;
	
	@Override
	public void init() {
		Window mainWindow = new Window("Openlayersexperiment Application");

		layout = new HorizontalLayout();
		layout.setSizeFull();

		SonarWidget sonarWidget = new SonarWidget();
		layout.addComponent(sonarWidget);

		sonarWidget.setHeight("200px");
		sonarWidget.setWidth("100%");

		mainWindow.addComponent(layout);
		
		setMainWindow(mainWindow);	
	}
}
