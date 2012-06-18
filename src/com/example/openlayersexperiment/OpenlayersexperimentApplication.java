package com.example.openlayersexperiment;
import com.vaadin.Application;
import com.vaadin.ui.*;

public class OpenlayersexperimentApplication extends Application {
	private HorizontalLayout layout;
	
	@Override
	public void init() {
		Window mainWindow = new Window("Openlayersexperiment Application");

		Panel panel = new Panel();
		layout = new HorizontalLayout();
		layout.setSizeUndefined();

		panel.setContent(layout);
		panel.setScrollable(true);
		panel.setSizeFull();
		//panel.setHeight("100%");
		mainWindow.setContent(new VerticalLayout());
		mainWindow.addComponent(panel);
		SonarWidget sonarWidget = new SonarWidget();
		sonarWidget.setHeight("500px");
		sonarWidget.setWidth("100%");

		mainWindow.addComponent(sonarWidget);
		
		setMainWindow(mainWindow);	
	}
}
