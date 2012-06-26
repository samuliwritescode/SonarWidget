package com.example.openlayersexperiment;
import com.vaadin.Application;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.ui.*;

public class OpenlayersexperimentApplication extends Application {
	private HorizontalLayout layout;
	
	@Override
	public void init() {
		final Window mainWindow = new Window("Openlayersexperiment Application");

		layout = new HorizontalLayout();
		layout.setSizeFull();

		final SonarWidget sonarWidget = new SonarWidget();
		final CheckBox button = new CheckBox("click");
		button.setImmediate(true);
		button.setValue(Boolean.TRUE);
		button.addListener(new Property.ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {				
				Boolean value = (Boolean)event.getProperty().getValue();
				sonarWidget.setOverlay(value.booleanValue());
			}
		});
		
		layout.addComponent(button);
		layout.addComponent(sonarWidget);

		sonarWidget.setHeight("200px");
		sonarWidget.setWidth("100%");

		
		mainWindow.addComponent(layout);
		
		setMainWindow(mainWindow);	
	}
}
