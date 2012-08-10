package com.vaadin.sonarwidget.widgetset.client.ui;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.UIDL;

class WidgetState {
	private VSonarWidget ui;
	private DepthData model;
	private ApplicationConnection client;
	private String uid;
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
	
	public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
		this.client = client;
		this.uid = uidl.getId();
		
		if(this.drawn.isEmpty()) {
			fetchSonarData(0);
			return;
		}		

		if(this.canvases.isEmpty() && uidl.hasAttribute("pingcount")) {
			initialize(uidl.getIntAttribute("pingcount"));
		}
		
		if(uidl.hasAttribute("overlay")) {
			this.overlay = uidl.getBooleanAttribute("overlay");
		}
		
		if(uidl.hasAttribute("color")) {
			this.colormask = uidl.getIntAttribute("color");
		}
		
		if(uidl.hasAttribute("sidesonar")) {
			this.sidescan = uidl.getBooleanAttribute("sidesonar");
		}

		if(uidl.hasAttribute("offset")) {
			int offset = uidl.getIntAttribute("offset");

			Canvas canvas = this.canvases.get((int)(offset/tilewidth));
			final Context2d context = canvas.getContext2d();
			context.clearRect(0, 0, tilewidth, canvas.getCoordinateSpaceHeight());
			
			if(uidl.hasAttribute("lowlimits")) {
				model.appendLowlimit(uidl.getStringArrayAttribute("lowlimits"), offset);			
			}
			
			if(uidl.hasAttribute("depths")) {
				model.appendDepth(uidl.getStringArrayAttribute("depths"), offset);			
			}
			
			if(uidl.hasAttribute("temps")) {
				model.appendTemp(uidl.getStringArrayAttribute("temps"), offset);				
			}
			
			if(uidl.hasAttribute("pic")) {
				ui.drawBitmap(offset, uidl.getStringAttribute("pic"), context, canvas);	
			}	
			
			ui.drawOverlay(offset, context);
		}
	}
	
	private void initialize(int pingcount) {
		model = new DepthData(pingcount);
		ui.initialize(pingcount);
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
			client.updateVariable(uid, "windowheight", ui.getElement().getClientHeight(), false);
			client.updateVariable(uid, "windowwidth", tilewidth, false);
			client.updateVariable(uid, "currentwindow", loop, true);
		}
	}

	public void addCanvas(Canvas canvas) {
		this.canvases.add(canvas);
	}

	public void scrollEvent(int horizontalScrollPosition) {
		fetchSonarData(horizontalScrollPosition);
	}
}
