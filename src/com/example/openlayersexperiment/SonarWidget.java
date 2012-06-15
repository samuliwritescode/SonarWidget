package com.example.openlayersexperiment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;
import java.util.Map;

import javax.imageio.ImageIO;

import com.example.openlayersexperiment.LowranceSonar.Ping;
import com.vaadin.terminal.DownloadStream;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.terminal.Resource;
import com.vaadin.terminal.StreamResource;
import com.vaadin.terminal.StreamResource.StreamSource;
import com.vaadin.terminal.StreamVariable;
import com.vaadin.terminal.URIHandler;
import com.vaadin.terminal.StreamVariable.StreamingEndEvent;
import com.vaadin.terminal.StreamVariable.StreamingErrorEvent;
import com.vaadin.terminal.StreamVariable.StreamingProgressEvent;
import com.vaadin.terminal.StreamVariable.StreamingStartEvent;
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
			//String[] pixeldata = new String[length*100];
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
			
			/*for(int x=0; x < length; x++) {
				byte[] pings = pingRange[x].getSoundings();
				int resolution = pings.length;
				for(int y=0; y < 100; y++) {
					pixeldata[y*length+x] = String.valueOf(pings[(int)(y*resolution/100.0)]); 
				}
			}*/
			final int offset = this.offset;
			
			//target.addVariable(this, "pixeldata", pixeldata);
			target.addVariable(this, "totalwidth", sonar.getLength());
			target.addVariable(this, "row", values);
			target.addVariable(this, "offset", offset);
			
			
			StreamResource streamResource = new StreamResource(new StreamSource() {
				@Override
				public InputStream getStream() {
			        try {
			        	ByteArrayOutputStream imagebuffer = new ByteArrayOutputStream();
			            ImageIO.write(Main.createImage(sonar, offset, length, 210), "png", imagebuffer);	           
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
			//System.out.println(String.format("offset: %d", offset));
			requestRepaint();
		}
		  
	}
}
