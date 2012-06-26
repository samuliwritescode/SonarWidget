package com.example.openlayersexperiment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

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

	private int windowlength = 0;
	private int windowheight = 0;
	private LowranceSonar sonar;
	private Queue<Integer> offsets;
	private boolean overlay = true;
	
	public SonarWidget() {
		offsets = new LinkedList<Integer>();
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
		
		target.addAttribute("overlay", overlay);
		if(windowlength != 0 && windowheight != 0) {
			final Integer offset = this.offsets.poll();
			
			if(offset == null) {
				return;
			}
			
			String[] lowlimits = new String[windowlength];
			String[] depths = new String[windowlength];
			String[] temps = new String[windowlength];
			Ping[] pingRange = null;
			try {
				if(sonar.getLength() > (offset+windowlength)) {
					pingRange = sonar.getPingRange(offset, windowlength);
				} else {
					pingRange = sonar.getPingRange(offset, (int) (sonar.getLength()-offset));
				}
				
			} catch (IOException e) {		
				e.printStackTrace();
			}
			
			for(int loop=0; loop < windowlength && loop < pingRange.length; loop++) {
				lowlimits[loop] = String.format("%.1f", pingRange[loop].getLowLimit());	
				depths[loop] = String.format("%.1f", pingRange[loop].getDepth());
				temps[loop] = String.format("%.1f", pingRange[loop].getTemp());
			}

			target.addAttribute("pingcount", sonar.getLength());
			target.addAttribute("lowlimits", lowlimits);
			target.addAttribute("depths", depths);
			target.addAttribute("temps", temps);

			
			target.addAttribute("offset", offset);
						
			StreamResource streamResource = new StreamResource(new StreamSource() {
				@Override
				public InputStream getStream() {
			        try {
			        	ByteArrayOutputStream imagebuffer = new ByteArrayOutputStream();
			            ImageIO.write(Main.createImage(sonar, offset, windowlength, windowheight), "png", imagebuffer);	           
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
		if(variables.containsKey("windowwidth")) {
			windowlength = (Integer)variables.get("windowwidth");			
		}
		
		if(variables.containsKey("windowheight")) {
			windowheight = (Integer)variables.get("windowheight");
		}
		
		if(variables.containsKey("currentwindow")) {			
			this.offsets.add((Integer)variables.get("currentwindow"));
		}
		
		requestRepaint();
		  
	}

	public void setOverlay(boolean booleanValue) {
		this.overlay = booleanValue;
		requestRepaint();
	}
}
