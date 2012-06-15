package com.example.openlayersexperiment.widgetset.client.ui;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
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
	private int width = 1000;
	private Label legend;

	public VSonarWidget() {
		super();
		this.canvases = new ArrayList<Canvas>();
		this.drawn = new ArrayList<String>();
		this.legend = new Label();
		this.legend.getElement().getStyle().setPosition(Position.FIXED);
		this.legend.setText("Depth: ");

		vert = new HorizontalPanel();
		add(vert);
		vert.add(legend);

		addScrollHandler(this);
		setAlwaysShowScrollBars(true);
	}
	
	private void updateSize(int width) {
		vert.clear();
		vert.setWidth(width+"px");
		vert.setHeight("100px");
		vert.add(legend);
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
		
		if(!uidl.hasAttribute("totalwidth")) {
			getData(0);
			return;
		}
		
		if(this.canvases.isEmpty() && uidl.hasAttribute("totalwidth")) {
			updateSize(uidl.getIntAttribute("totalwidth"));
		}
		
		if(uidl.hasAttribute("offset")) {
			int offset = uidl.getIntAttribute("offset");
			this.drawn.add(new Integer(offset).toString());
			Canvas canvas = this.canvases.get((int)(offset/width));
			final Context2d context = canvas.getContext2d();
			context.clearRect(0, 0, width, 100);
			
			if(uidl.hasAttribute("pic")) {
				drawBitmap(uidl, context);	
			}
		
			if(uidl.hasAttribute("row")) {
				drawOverlay(uidl, context);
			}
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
				context.drawImage(ImageElement.as(image.getElement()), 0, 0);
				drawOverlay(uidl, context);
			}
		});
	}

	private void drawOverlay(final UIDL uidl, final Context2d context) {
		String[] row = uidl.getStringArrayAttribute("row");

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

	@Override
	public void onScroll(ScrollEvent event) {
		getData(getHorizontalScrollPosition());
	}
}
