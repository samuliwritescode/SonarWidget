package com.vaadin.sonarwidget.widgetset.client.ui;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.CanvasPixelArray;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.ImageData;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
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

public class VSonarWidget extends ScrollPanel implements ScrollHandler  {

	private WidgetState state;
	private DepthData model;
	private HorizontalPanel vert;
	private Label depthlabel;
	private Label templabel;
	private Label cursorlabel;
	private Canvas ruler;
	private VerticalPanel labels;
	
	public static int COLOR_RED = 1;
	public static int COLOR_GREEN = 2;
	public static int COLOR_BLUE = 4;
	public static int COLOR_INVERSE = 8;
	public static int COLOR_MAPCOLORS = 16;
	public static int COLOR_MORECONTRAST = 32;
	public static int COLOR_LESSCONTRAST = 64;
	public static int COLOR_CONTRASTBOOST = 128;

	public VSonarWidget() {
		super();
		
		this.state = new WidgetState(this);
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
		
		Scheduler.get().scheduleEntry(new RepeatingCommand() {
                    
                    @Override
                    public boolean execute() {
                        if (getElement().getClientWidth() <= 0) {
                            return true;
                        }
                        
                        getState().fetchInitialData();
                        return false;
                    }
                });
	}
	
	public void clearWidget(int totalwidth) {		
		vert.clear();
		vert.setWidth(totalwidth+"px");
		vert.add(labels);
		vert.add(ruler);		
		labels.setVisible(false);
		ruler.setVisible(false);
	}
	
	public Canvas addCanvas(int canvaswidth) {
		Canvas canvas = Canvas.createIfSupported();
		vert.add(canvas);
		
		canvas.setCoordinateSpaceHeight(getElement().getClientHeight());
		canvas.setHeight(getElement().getClientHeight()+"px");
		canvas.setCoordinateSpaceWidth(canvaswidth);
		canvas.setWidth(canvaswidth+"px");
		
		return canvas;
	}
	
	public void clearCanvas(Canvas canvas) {
		final Context2d context = canvas.getContext2d();
		context.clearRect(0, 0, canvas.getCoordinateSpaceWidth(), canvas.getCoordinateSpaceHeight());
	}
	
	public WidgetState getState() {
	    return state;
	}

	/**
	 * Depth bitmap drawing is animated starting from 
	 * fully transparent. Image tag is hidden and it's
	 * content is drawn to HTML5 canvas.
	 * @param offset
	 * @param name
	 * @param context
	 */
	public void drawBitmap(final int offset, final String name, final Context2d context, final Canvas canvas) {
		final Image image = new Image(name);
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
		int colormask = state.getColorMask();
		if(colormask == 0) {
			return;
		}
		
		int shift = 1;
		if((colormask&COLOR_CONTRASTBOOST) != 0) {
			shift = 2;
		}
		
		ImageData data = context.getImageData(0, 0, context.getCanvas().getOffsetWidth(), getElement().getClientHeight());
		CanvasPixelArray array = data.getData();
		for(int x = 0; x < array.getLength(); x+=4) {
			int red = array.get(x);
			int green = array.get(x+1);
			int blue = array.get(x+2);
			//int alpha = array.get(x+3);
			
			red = (colormask&COLOR_RED)!=0?red:0;
			green = (colormask&COLOR_GREEN)!=0?green:0;
			blue = (colormask&COLOR_BLUE)!=0?blue:0;
			
			if((colormask&COLOR_INVERSE) !=0) {
				red = 255-red;
				green = 255-green;
				blue = 255-blue;
			}

			if((colormask&COLOR_MORECONTRAST) != 0) {
				red <<= shift;
				green <<= shift;
				blue <<= shift;
				
				red = Math.min(255, red);
				green = Math.min(255, green);
				blue = Math.min(255, blue);
			}
			
			if((colormask&COLOR_LESSCONTRAST) != 0) {
				red >>= shift;
				green >>= shift;
				blue >>= shift;
			}
			
			if((colormask&COLOR_MAPCOLORS) != 0) {
				red &= 0xC0;
				green &= 0x60;
				blue &= 0x3F;
			}
			
			array.set(x, red&0xFF);
			array.set(x+1, green&0xFF);
			array.set(x+2, blue&0xFF);
			//array.set(x+3, alpha);
		}
		
		context.putImageData(data, 0, 0);
	}

	/**
	 * Overlay is drawn on top of depth image.
	 * Only red depth line is implemented so far.
	 * @param offset 
	 * @param context draw context
	 */
	public void drawOverlay(int offset, final Context2d context) {
		if(!state.isOverlay()) {
			return;
		}

		int width = context.getCanvas().getOffsetWidth();
		double[] points = new double[width];
		double[] mirrorpoints = null;
		
		if(state.isSideScan()) {
			mirrorpoints = new double[width];
		}
		
		for(int loop=0; loop < width; loop++) {
			float depth = model.getDepth(loop+offset);
			float lowlimit = model.getLowlimit(loop+offset);
			
			double yPos = getElement().getClientHeight()*depth/lowlimit;
						
			if(state.isSideScan()) {
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
		if(!state.isSideScan()) {
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
		state.scrollEvent(getHorizontalScrollPosition());
				
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
		
		if(state.isSideScan()) {
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

	public void setModel(DepthData model) {
		this.model = model;
		
	}
}
