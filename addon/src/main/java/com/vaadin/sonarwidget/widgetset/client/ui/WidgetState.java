package com.vaadin.sonarwidget.widgetset.client.ui;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;

class WidgetState {
	private VSonarWidget ui;
	private DepthData model;
	private SonarWidgetConnector connector;
	private List<String> drawn;
	private boolean overlay;
	private boolean sidescan;
	private int colormask = 0;
	public static final int tilewidth = 400;
	private List<Canvas> canvases;
	
	public WidgetState(VSonarWidget ui) {
		this.drawn = new ArrayList<String>();
		this.ui = ui;		
		this.canvases = new ArrayList<Canvas>();
	}
	
	public int getColorMask() {
		return this.colormask;
	}
	
	public boolean isSideScan() {
		return this.sidescan;
	}
	
	public boolean isOverlay() {
		return this.overlay;
	}
	
	public void setConnector(SonarWidgetConnector connector) {
	    this.connector = connector;
	}
	
	public void fetchInitialData() {
	    if(this.drawn.isEmpty()) {
                fetchSonarData(0);                
            }       
	}
	
	public void setOverlay(boolean isOverlay) {
	    this.overlay = isOverlay;
	}
	
	public void setColor(int color) {
	    this.colormask = color;
	}
	
	public void setSideScan(boolean isSideScan) {
	    this.sidescan = isSideScan;
	}
	
	public void setPingCount(long pingcount) {
	    if(this.canvases.isEmpty()) {
                initialize((int)pingcount);
            }
	}
	
	public void setOffset(int offset, String pic, String[] lowlimits, String[] depths, String[] temps) {
            Canvas canvas = this.canvases.get((int)(offset/tilewidth));
            ui.clearCanvas(canvas);
            final Context2d context = canvas.getContext2d();

            model.appendLowlimit(lowlimits, offset);                        
            model.appendDepth(depths, offset);                      
            model.appendTemp(temps, offset);                                
            ui.drawBitmap(offset, pic, context, canvas); 
            ui.drawOverlay(offset, context);
	}
	
	private void initialize(int pingcount) {
		model = new DepthData(pingcount);
		ui.clearWidget(pingcount);
		ui.setModel(model);
		
		for(int loop=0; loop < pingcount; loop+=WidgetState.tilewidth) {
			int width = Math.min(WidgetState.tilewidth, pingcount-loop);
			Canvas canvas = ui.addCanvas(width);
			this.canvases.add(canvas);
		}
	}

	private void fetchSonarData(int offset) {
		int normalizedoffset = offset-offset%tilewidth;
		
		for(int loop = normalizedoffset; loop < normalizedoffset+ui.getOffsetWidth()+tilewidth; loop+= tilewidth) {
			if(this.drawn.contains(new Integer(loop).toString())) {
				continue;
			} else {
				this.drawn.add(new Integer(loop).toString());
			}
			connector.getData(ui.getElement().getClientHeight(), tilewidth, loop);
		}
	}

	public void scrollEvent(int horizontalScrollPosition) {
		fetchSonarData(horizontalScrollPosition);
	}
}
