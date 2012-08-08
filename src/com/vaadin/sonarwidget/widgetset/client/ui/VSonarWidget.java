package com.vaadin.sonarwidget.widgetset.client.ui;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.CanvasPixelArray;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.ImageData;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.Paintable;
import com.vaadin.terminal.gwt.client.UIDL;

public class VSonarWidget extends ScrollPanel implements Paintable, ScrollHandler  {

	private HorizontalPanel vert;
	private ApplicationConnection client;
	private String uid;
	private List<Canvas> canvases;
	private List<String> drawn;
	private DepthData model;

	private int tilewidth = 400;
	private Label depthlabel;
	private Label templabel;
	private Label cursorlabel;
	private boolean overlay;
	private boolean sidescan;
	private Canvas ruler;
	private VerticalPanel labels;
	private int colormask = 0;

	public VSonarWidget() {
		super();
		this.canvases = new ArrayList<Canvas>();
		this.drawn = new ArrayList<String>();
		this.depthlabel = new Label();
		this.templabel = new Label();
		this.cursorlabel = new Label();
		vert = new HorizontalPanel();
		vert.setHeight("100%");
		labels = new VerticalPanel();
		labels.getElement().getStyle().setPosition(Position.FIXED);
		labels.setStyleName("v-sonarwidget-labels");

		setWidget(vert);
		vert.add(labels);
		labels.add(depthlabel);
		labels.add(cursorlabel);
		labels.add(templabel);
		getElement().getStyle().setOverflowX(Overflow.AUTO);
		getElement().getStyle().setOverflowY(Overflow.HIDDEN);
		this.ruler = Canvas.createIfSupported();
		this.ruler.getElement().getStyle().setPosition(Position.FIXED);

		sinkEvents(Event.ONMOUSEMOVE);
		addScrollHandler(this);
	}
	
	private void initialize(int width) {		
		vert.clear();
		vert.setWidth(width+"px");
		vert.add(labels);
		
		model = new DepthData(width);
		
		vert.add(ruler);
		
		labels.setVisible(false);
		ruler.setVisible(false);
		for(int loop=0; loop < width; loop+=this.tilewidth) {
			Canvas canvas = Canvas.createIfSupported();
			this.canvases.add(canvas);
			vert.add(canvas);
			canvas.setCoordinateSpaceHeight(getElement().getClientHeight());
			canvas.setHeight(getElement().getClientHeight()+"px");

			int canvaswidth = Math.min(this.tilewidth, width-loop);
			canvas.setCoordinateSpaceWidth(canvaswidth);
			canvas.setWidth(canvaswidth+"px");
		}
	}

	
	private void fetchSonarData(int offset) {
		int normalizedoffset = offset-offset%tilewidth;
		
		for(int loop = normalizedoffset; loop < normalizedoffset+getOffsetWidth()+this.tilewidth; loop+= this.tilewidth) {
			if(this.drawn.contains(new Integer(loop).toString())) {
				continue;
			} else {
				this.drawn.add(new Integer(loop).toString());
			}
			client.updateVariable(uid, "windowheight", getElement().getClientHeight(), false);
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
				drawBitmap(offset, uidl.getStringAttribute("pic"), context, canvas);	
			}	
			
			drawOverlay(offset, context);
		}
	}

	/**
	 * Depth bitmap drawing is animated starting from 
	 * fully transparent. Image tag is hidden and it's
	 * content is drawn to HTML5 canvas.
	 * @param offset
	 * @param name
	 * @param context
	 */
	private void drawBitmap(final int offset, final String name, final Context2d context, final Canvas canvas) {
		final Image image = new Image(GWT.getHostPageBaseURL()+name.substring(5));
		RootPanel.get().add(image);
		image.setVisible(false);
		
		//When image loads start transition animation
		image.addLoadHandler(new LoadHandler() {
			
			@Override
			public void onLoad(LoadEvent event) {
				canvas.getElement().getStyle().setOpacity(0);
				context.drawImage(
					ImageElement.as(
						image.getElement()
					), 
					0, 
					0, 
					context.getCanvas().getOffsetWidth(), 
					getElement().getClientHeight()
				);
				
				colorizeImage(context);
				drawOverlay(offset, context);
				
				new Timer() {
					private int alpha = 0;
					@Override
					public void run() {
									
						canvas.getElement().getStyle().setOpacity(alpha*0.1);

						//when animation has reached
						//zero opacity stop animation timer.
						if(alpha >= 10) {
							this.cancel();
						}
						
						alpha++;
					}
				}.scheduleRepeating(24);
			}
		});
	}
	
	private void colorizeImage(Context2d context) {
		if(this.colormask == 0) {
			return;
		}
		
		ImageData data = context.getImageData(0, 0, context.getCanvas().getOffsetWidth(), getElement().getClientHeight());
		CanvasPixelArray array = data.getData();
		for(int x = 0; x < array.getLength(); x++) {
			int color = array.get(x);
			
			switch(x%4) {
			case 0: //red
				if((colormask&1) != 0) {
					array.set(x, color);
				} else {
					array.set(x, 0);
				}
				break;
			case 1: //green 
				if((colormask&2) != 0) {
					array.set(x, color);
				} else {
					array.set(x, 0);
				}
				break;
			case 2: //blue
				if((colormask&4) != 0) {
					array.set(x, color);
				} else {
					array.set(x, 0);
				}
				break;
			case 3: //alpha
				break;
			}
		}
		
		context.putImageData(data, 0, 0);
	}

	/**
	 * Overlay is drawn on top of depth image.
	 * Only red depth line is implemented so far.
	 * @param offset 
	 * @param context draw context
	 */
	private void drawOverlay(int offset, final Context2d context) {
		if(!this.overlay) {
			return;
		}

		int width = context.getCanvas().getOffsetWidth();
		double[] points = new double[width];
		double[] mirrorpoints = null;
		
		if(this.sidescan) {
			mirrorpoints = new double[width];
		}
		
		for(int loop=0; loop < width; loop++) {
			float depth = model.getDepth(loop+offset);
			float lowlimit = model.getLowlimit(loop+offset);
			
			double yPos = getElement().getClientHeight()*depth/lowlimit;
						
			if(this.sidescan) {
				yPos = (getElement().getClientHeight()/2)*depth/lowlimit + getElement().getClientHeight()/2;
				mirrorpoints[loop] = getElement().getClientHeight()/2 - (yPos-getElement().getClientHeight()/2);
			}
			
			points[loop] = yPos;
		}
		
		drawLine(context, points);
		if(mirrorpoints != null) {
			drawLine(context, mirrorpoints);
		}
	}
	
	private void drawLine(Context2d context, double[] points) {
		context.setStrokeStyle("red");
		context.beginPath();
		
		for(int loop=0; loop < points.length; loop++) {
			double yPos = points[loop];
			
			if(loop==0) {
				context.moveTo(loop, yPos);
			} else {
				context.lineTo(loop, yPos);
			}
		}
		
		context.stroke();
	}
	
	
	private void updateRuler(int coordinate) {
		this.ruler.setVisible(true);
		int height = getElement().getClientHeight();
		this.ruler.setCoordinateSpaceHeight(height);
		this.ruler.setCoordinateSpaceWidth(10);
		this.ruler.setWidth("10px");
		this.ruler.setHeight(height+"px");
		
		Context2d context2d = this.ruler.getContext2d();
		context2d.clearRect(0, 0, 10, height);
		context2d.setFillStyle("blue");
		context2d.fillRect(0, 0, 1, height);
		
		//Skip depth arrow in side scan mode
		if(!this.sidescan) {
			float depth = model.getDepth(coordinate);
			float lowlimit = model.getLowlimit(coordinate);
			int drawdepth = (int) (height*depth/lowlimit);
			context2d.beginPath();
			context2d.moveTo(10, drawdepth-10);
			context2d.lineTo(1, drawdepth);
			context2d.lineTo(10, drawdepth+10);
			context2d.stroke();
		}
		
		this.ruler.getElement().getStyle().setMarginLeft(coordinate-getHorizontalScrollPosition(), Unit.PX);
	}
	
	@Override
	public void onScroll(ScrollEvent event) {
		fetchSonarData(getHorizontalScrollPosition());		
	}
	
	private void onMouseHover(Point coordinate) {
		updateRuler(coordinate.getX());		
		updateTextLabels(coordinate);
	}

	private void updateTextLabels(Point coordinate) {
		this.labels.setVisible(true);
		this.labels.getElement().getStyle().setMarginLeft(coordinate.getX()-getHorizontalScrollPosition(), Unit.PX);

		this.depthlabel.setText("Depth: "+model.getDepth(coordinate.getX())+" m");			
		float lowlimit = model.getLowlimit(coordinate.getX());
		float cursor = lowlimit*(coordinate.getY()/(float)getElement().getClientHeight());
		
		if(this.sidescan) {
			float surfacepx = (float)getElement().getClientHeight()/2;
			float distancepx = Math.abs(surfacepx - coordinate.getY());
			cursor = distancepx * lowlimit/surfacepx;
		}
		this.cursorlabel.setText("Cursor: "+NumberFormat.getFormat("#.0 m").format(cursor));
		this.templabel.setText("Temp: "+model.getTemp(coordinate.getX())+" C");
	}
	
	@Override
	public void onBrowserEvent(Event event) {
		switch(DOM.eventGetType(event)) {
		case Event.ONMOUSEMOVE:
			onMouseHover(getMouseCursorPoint(event));
			break;
		default:
			super.onBrowserEvent(event);
			break;
		}
	}
	
	private Point getMouseCursorPoint(Event event) {
		Point pt = new Point(
			event.getClientX()-getAbsoluteLeft()+getHorizontalScrollPosition(),
			event.getClientY()-getAbsoluteTop()
		);
		return pt;
	}
	
	private static class Point {
		int x;
		int y;
		
		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}
		
		public int getX() {
			return this.x;
		}
		
		public int getY() {
			return this.y;
		}
	}
	
	private static class DepthData {
		private String[] depths;
		private String[] temps;
		private String[] lowlimits;
		
		public DepthData(int width) {
			depths = new String[width];
			temps = new String[width];
			lowlimits = new String[width];
		}
		
		private float getFloatValue(String[] table, int index) {
			if(table != null &&
				table.length > index && 
				table[index] != null) {
				return new Float(table[index]).floatValue();
			}
			
			return 0;
		}
		
		public float getDepth(int index) {
			return getFloatValue(this.depths, index);
		}
		
		public float getLowlimit(int index) {
			return getFloatValue(this.lowlimits, index);
		}
		
		public float getTemp(int index) {
			return getFloatValue(this.temps, index);
		}
		
		public void appendDepth(String[] data, int offset) {
			fillArray(data, this.depths, offset);
		}
		
		public void appendLowlimit(String[] data, int offset) {
			fillArray(data, this.lowlimits, offset);
		}
		
		public void appendTemp(String[] data, int offset) {
			fillArray(data, this.temps, offset);
		}
		
		private void fillArray(String[] slice, String[] array, int offset) {
			for(int loop=offset; loop < offset+slice.length && loop < array.length; loop++) {
				array[loop] = slice[loop-offset];
			}	
		}
	}
}
