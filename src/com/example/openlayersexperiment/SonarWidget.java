package com.example.openlayersexperiment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import javax.imageio.ImageIO;

import com.example.openlayersexperiment.LowranceSonar.Ping;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.terminal.StreamResource;
import com.vaadin.terminal.StreamResource.StreamSource;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.ClientWidget;

@ClientWidget(com.example.openlayersexperiment.widgetset.client.ui.VSonarWidget.class)
public class SonarWidget extends AbstractComponent{

	private int offset = 0;
	private int length = 0;
	private LowranceSonar sonar;
	
	public SonarWidget() {
		try {
			sonar = new LowranceSonar(new File("/Users/samuli/Documents/Sonar0011.slg"));
			System.out.println(String.format("len: %d samples", sonar.getLength()));
		} catch (IOException e) {			
			throw new RuntimeException(e);
		}
		

	}
	
	@Override
	public void paintContent(PaintTarget target) throws PaintException {
		super.paintContent(target);
		if(length != 0) {
			String[] values = new String[length];
			Ping[] pingRange = null;
			try {
				if(sonar.getLength() > (offset+length)) {
					pingRange = sonar.getPingRange(offset, length);
				} else {
					pingRange = sonar.getPingRange(offset, (int) (sonar.getLength()-offset));
				}
				
			} catch (IOException e) {		
				e.printStackTrace();
			}
			
			for(int loop=0; loop < length && loop < pingRange.length; loop++) {
				values[loop] = Integer.toString((int)(100.0*pingRange[loop].getDepth()/pingRange[loop].getLowLimit()));			
			}
			
			final int offset = this.offset;

			target.addAttribute("totalwidth", sonar.getLength());
			target.addAttribute("row", values);
			target.addAttribute("offset", offset);
						
			StreamResource streamResource = new StreamResource(new StreamSource() {
				@Override
				public InputStream getStream() {
			        try {
			        	ByteArrayOutputStream imagebuffer = new ByteArrayOutputStream();
			            ImageIO.write(Main.createImage(sonar, offset, length, 100), "png", imagebuffer);	           
			            return new ByteArrayInputStream(imagebuffer.toByteArray());
			        } catch (IOException e) {
			        	e.printStackTrace();
			        	return null;
			        }
				}
			}, 
			String.format("frame%d-%d.png", offset, new Date().getTime()), 
			getApplication()
			);
			
			target.addAttribute("pic", streamResource);
			
		}
	}
	
	@Override
	public void changeVariables(Object source, Map<String, Object> variables) {
		if(variables.containsKey("currentwindow")) {
			Integer currentwindow = (Integer)variables.get("currentwindow");
			offset = currentwindow.intValue();			
			length = (Integer)variables.get("windowwidth");
			requestRepaint();
		}
		  
	}
}
