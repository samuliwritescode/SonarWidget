package com.example.openlayersexperiment.widgetset.client.ui;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.Paintable;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.VConsole;

public class VSonarWidget extends ScrollPanel implements Paintable, ScrollHandler  {

	private HorizontalPanel vert;
	private ApplicationConnection client;
	private String uid;
	private List<Canvas> canvases;
	private List<String> drawn;
	private int width = 1000;

	public VSonarWidget() {
		super();
		this.canvases = new ArrayList<Canvas>();
		this.drawn = new ArrayList<String>();

		vert = new HorizontalPanel();
		add(vert);

		addScrollHandler(this);
		setAlwaysShowScrollBars(true);
		VConsole.log("width: "+getOffsetWidth());
		
	}
	
	private void updateSize(int width) {
		vert.clear();
		vert.setWidth(width+"px");
		vert.setHeight("100px");
		for(int loop=0; loop < width; loop+=this.width) {
			Canvas canvas = Canvas.createIfSupported();
			canvas.setCoordinateSpaceHeight(100);
			canvas.setCoordinateSpaceWidth(this.width);
			canvas.setWidth(this.width+"px");
			canvas.setHeight("100px");
			this.canvases.add(canvas);
			vert.add(canvas);
		}
	}
	
	private void getData(int offset) {
		int normalizedoffset = offset-offset%width;
		if(this.drawn.contains(new Integer(normalizedoffset).toString())) {
			return;
		}
		
		client.updateVariable(uid, "currentwindow", normalizedoffset, false);
		client.updateVariable(uid , "windowwidth", width, true);
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
		if(getOffsetWidth() != this.width) {
			this.width = getOffsetWidth();
			this.drawn.clear();
			this.canvases.clear();
		}
		
		
		if(!uidl.hasVariable("totalwidth")) {
			getData(0);
			return;
		}
		
		if(this.canvases.isEmpty() && uidl.hasVariable("totalwidth")) {
			updateSize(uidl.getIntVariable("totalwidth"));
		}
		
		if(uidl.hasVariable("offset")) {
			int offset = uidl.getIntVariable("offset");
			this.drawn.add(new Integer(offset).toString());
			Canvas canvas = this.canvases.get((int)(offset/width));
			final Context2d context = canvas.getContext2d();
			context.clearRect(0, 0, width, 100);
			
			if(uidl.hasAttribute("pic")) {
				String name = uidl.getStringAttribute("pic");
				VConsole.log("pic: "+name);	
				final Image image = new Image(GWT.getHostPageBaseURL()+name.substring(5));
				RootPanel.get().add(image);
				image.setVisible(false);
				image.addLoadHandler(new LoadHandler() {
					
					@Override
					public void onLoad(LoadEvent event) {
						context.drawImage(ImageElement.as(image.getElement()), 0, 0);
						
					}
				});
			}	
		
			if(uidl.hasVariable("row")) {
				String[] row = uidl.getStringArrayVariable("row");

				context.setStrokeStyle("red");
				context.beginPath();
				for(int loop=0; loop < row.length; loop++) {
					int value = new Integer(row[loop]).intValue();
					if(loop==0) {
						context.moveTo(loop, value);
					} else {
						context.lineTo(loop, value);
					}
					
				}
	
				context.stroke();
			}
		}
	}

	@Override
	public void onScroll(ScrollEvent event) {
		getData(getHorizontalScrollPosition());
	}
}
