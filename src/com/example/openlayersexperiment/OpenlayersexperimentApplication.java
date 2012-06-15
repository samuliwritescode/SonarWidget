package com.example.openlayersexperiment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.vaadin.Application;
import com.vaadin.terminal.StreamResource;
import com.vaadin.ui.*;

public class OpenlayersexperimentApplication extends Application {
	private HorizontalLayout layout;
	private LowranceSonar sonar;
	
	
	@Override
	public void init() {
		Window mainWindow = new Window("Openlayersexperiment Application");

		Panel panel = new Panel();
		layout = new HorizontalLayout();
		layout.setSizeUndefined();
		
		try {
			sonar = new LowranceSonar(new File("/Users/samuli/Documents/Sonar0011.slg"));
		} catch (IOException e) {
			mainWindow.showNotification(e.toString());
			e.printStackTrace();
		}
		
		/*Slider slider = new Slider(0, (int) sonar.getLength());
		slider.setWidth("100%");
		mainWindow.addComponent(slider);*/
		panel.setContent(layout);
		panel.setScrollable(true);
		panel.setSizeFull();
		panel.setHeight("400px");
		mainWindow.setContent(new VerticalLayout());
		mainWindow.addComponent(panel);
		SonarWidget sonarWidget = new SonarWidget();
		sonarWidget.setHeight("200px");
		sonarWidget.setWidth("100%");
		mainWindow.addComponent(sonarWidget);
		
		setMainWindow(mainWindow);
		/*
		redraw(0);
		slider.addListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				Double index = (Double)event.getProperty().getValue();
				redraw(index.intValue());
			}
		});
		slider.setImmediate(true);*/

		/*for(int index = 0; index < this.sonar.getLength(); index+=400) {
			appendImage(index);
		}*/
	}
	
	/*private void appendImage(final int index) {
		layout.addComponent(
			new LazyLoadWrapper(
				new Embedded(
						"",
						new StreamResource(
							new StreamResource.StreamSource() {

								@Override
								public InputStream getStream() {
									
							        try {
							        	ByteArrayOutputStream imagebuffer = new ByteArrayOutputStream();
							            ImageIO.write(Main.createImage(sonar, index, 600, 200), "png", imagebuffer);	           
							            return  new ByteArrayInputStream(imagebuffer.toByteArray());
							        } catch (IOException e) {
							        	return null;
							        }
								}
						    },
							Double.toString(Math.random())+"image.png",
							this
						)
				),
				"600px",
				"200px"
			)
		);
	}*/
	
//	private void redraw(int index) {
//		layout.removeAllComponents();
//		image = Main.createImage(this.sonar, index);
//		
//		LowranceSonar.Ping firstping;
//		try {
//			firstping = this.sonar.getPingRange(index, 1)[0];
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//		
//		layout.addComponent(
//			new Link(
//				"depth: "+firstping.getDepth()+
//				", temp: "+firstping.getTemp()+
//				", speed: "+firstping.getSpeed()+
//				", time: "+firstping.getTimeStamp()/1000/60, 
//				new ExternalResource(
//					String.format(
//						"http://maps.googleapis.com/maps/api/staticmap?center=%f,%f&zoom=14&size=800x800&sensor=false",
//						firstping.getLatitude(),
//						firstping.getLongitude()
//					)
//				)
//			)
//		);
//				
//		layout.addComponent(
//			new Embedded(
//					"",
//					new StreamResource(streamresource,
//							Double.toString(Math.random())+"image.png",
//							this
//					)
//			)
//		);
//	}
	
//	private StreamResource.StreamSource streamresource = new StreamResource.StreamSource() {
//
//		@Override
//		public InputStream getStream() {
//			
//	        try {
//	        	ByteArrayOutputStream imagebuffer = new ByteArrayOutputStream();
//	            ImageIO.write(image, "png", imagebuffer);	           
//	            return  new ByteArrayInputStream(imagebuffer.toByteArray());
//	        } catch (IOException e) {
//	        	return null;
//	        }
//		}
//    };


}
