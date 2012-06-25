package com.example.openlayersexperiment.widgetset.client.ui;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.Paintable;
import com.vaadin.terminal.gwt.client.UIDL;

public class VSonarWidget extends ScrollPanel implements Paintable, ScrollHandler  {

	private HorizontalPanel vert;
	private ApplicationConnection client;
	private String uid;
	private List<Canvas> canvases;
	private List<String> drawn;
	private String[] depths;
	private String[] temps;
	private int tilewidth = 400;
	private int height = 400;
	private Label depthlabel;
	private Label templabel;

	public VSonarWidget() {
		super();
		this.canvases = new ArrayList<Canvas>();
		this.drawn = new ArrayList<String>();
		this.depthlabel = new Label();
		this.depthlabel.getElement().getStyle().setPosition(Position.FIXED);
		this.depthlabel.setText("Depth: ");
		
		this.templabel = new Label();
		this.templabel.getElement().getStyle().setPosition(Position.FIXED);
		this.templabel.getElement().getStyle().setMarginTop(20, Unit.PX);
		this.templabel.setText("Temp: ");

		vert = new HorizontalPanel();
		vert.setHeight("100%");
		//setHeight("100%");
		
		setWidget(vert);
		vert.add(depthlabel);
		vert.add(templabel);
		getElement().getStyle().setOverflowX(Overflow.AUTO);
		getElement().getStyle().setOverflowY(Overflow.HIDDEN);

		addScrollHandler(this);
	}
	
	private void initialize(int width) {		
		vert.clear();
		vert.setWidth(width+"px");
		//vert.setHeight(this.height+"px");
		vert.add(depthlabel);
		vert.add(templabel);
		depths = new String[width];
		temps = new String[width];
		for(int loop=0; loop < width; loop+=this.tilewidth) {
			Canvas canvas = Canvas.createIfSupported();
			canvas.setCoordinateSpaceHeight(this.height);
			canvas.setCoordinateSpaceWidth(this.tilewidth);
			canvas.setWidth(this.tilewidth+"px");
			//canvas.setHeight("100%");
			canvas.setHeight(this.height+"px");
			this.canvases.add(canvas);
			vert.add(canvas);
		}
	}
	
	private void getData(int offset) {
		int normalizedoffset = offset-offset%tilewidth;
		
		for(int loop = normalizedoffset; loop < normalizedoffset+getOffsetWidth()+this.tilewidth; loop+= this.tilewidth) {
			if(this.drawn.contains(new Integer(loop).toString())) {
				continue;
			} else {
				this.drawn.add(new Integer(loop).toString());
			}
			client.updateVariable(uid, "windowheight", height, false);
			client.updateVariable(uid, "windowwidth", tilewidth, false);
			client.updateVariable(uid, "currentwindow", loop, true);
		}
	}

	@Override
	public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
		if (client.updateComponent(this, uidl, true)) {
		    // If client.updateComponent returns true there has been no changes and we
		    // do not need to update anything.
			return;
		}
		this.client = client;
		this.uid = uidl.getId();
		
		this.height = getOffsetHeight();
		
		if(this.drawn.isEmpty()) {
			getData(0);
			return;
		}		

		if(this.canvases.isEmpty() && uidl.hasAttribute("pingcount")) {
			initialize(uidl.getIntAttribute("pingcount"));
		}

		if(uidl.hasAttribute("offset")) {
			int offset = uidl.getIntAttribute("offset");

			Canvas canvas = this.canvases.get((int)(offset/tilewidth));
			final Context2d context = canvas.getContext2d();
			context.clearRect(0, 0, tilewidth, height);
			
			if(uidl.hasAttribute("pic")) {
				drawBitmap(uidl, context);	
			}
		
			if(uidl.hasAttribute("lowlimits")) {
				drawOverlay(uidl, context);
			}
			
			if(uidl.hasAttribute("depths")) {
				fillArray(uidl.getStringArrayAttribute("depths"), this.depths, offset);			
			}
			
			if(uidl.hasAttribute("temps")) {
				fillArray(uidl.getStringArrayAttribute("temps"), this.temps, offset);				
			}
		}
	}
	
	private void fillArray(String[] slice, String[] array, int offset) {
		for(int loop=offset; loop < offset+slice.length; loop++) {
			array[loop] = slice[loop-offset];
		}	
	}

	private void drawBitmap(final UIDL uidl, final Context2d context) {
		String name = uidl.getStringAttribute("pic");
		final Image image = new Image(GWT.getHostPageBaseURL()+name.substring(5));
		RootPanel.get().add(image);
		image.setVisible(false);
		image.addLoadHandler(new LoadHandler() {
			
			@Override
			public void onLoad(LoadEvent event) {
				context.setGlobalAlpha(0);
				new Timer() {
					private int alpha = 0;
					@Override
					public void run() {
						
						if(alpha >= 10) {
							this.cancel();
						}
						
						context.setGlobalAlpha(alpha*0.1);
						context.drawImage(ImageElement.as(image.getElement()), 0, 0);
						drawOverlay(uidl, context);
						alpha++;
					}
				}.scheduleRepeating(24);
			}
		});
	}

	private void drawOverlay(final UIDL uidl, final Context2d context) {
		String[] lowlimits = uidl.getStringArrayAttribute("lowlimits");
		String[] depths = uidl.getStringArrayAttribute("depths");

		context.setStrokeStyle("red");
		context.beginPath();
		for(int loop=0; loop < depths.length; loop++) {
			float depth = new Float(depths[loop]).floatValue();
			float lowlimit = new Float(lowlimits[loop]).floatValue();
			if(loop==0) {
				context.moveTo(loop, this.height*depth/lowlimit);
			} else {
				context.lineTo(loop, this.height*depth/lowlimit);
			}
		}
		context.stroke();
	}
	
	@Override
	public void onScroll(ScrollEvent event) {
		if(this.depths != null && this.depths.length > getHorizontalScrollPosition()) {
			this.depthlabel.setText("Depth: "+this.depths[getHorizontalScrollPosition()]);
		}
		
		if(this.temps != null && this.temps.length > getHorizontalScrollPosition()) {
			this.templabel.setText("Temp: "+this.temps[getHorizontalScrollPosition()]);
		}
		
		getData(getHorizontalScrollPosition());		
	}
}
