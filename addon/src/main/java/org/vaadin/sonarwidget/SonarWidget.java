package org.vaadin.sonarwidget;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Date;

import javax.imageio.ImageIO;

import org.vaadin.sonarwidget.data.HumminbirdSSI;
import org.vaadin.sonarwidget.data.LowranceSonar;
import org.vaadin.sonarwidget.data.LowranceStructureScan;
import org.vaadin.sonarwidget.data.Ping;
import org.vaadin.sonarwidget.data.Sonar;
import org.vaadin.sonarwidget.data.Sonar.Type;
import org.vaadin.sonarwidget.widgetset.client.ui.SonarWidgetClientRpc;
import org.vaadin.sonarwidget.widgetset.client.ui.SonarWidgetServerRpc;
import org.vaadin.sonarwidget.widgetset.client.ui.SonarWidgetState;

import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Component;
import com.vaadin.util.ReflectTools;

public class SonarWidget extends AbstractComponent {

    private static final long serialVersionUID = 1L;
    private Sonar sonar;
    public static int COLOR_RED = 1;
    public static int COLOR_GREEN = 2;
    public static int COLOR_BLUE = 4;
    public static int COLOR_INVERSE = 8;
    public static int COLOR_MAPCOLORS = 16;
    public static int COLOR_MORECONTRAST = 32;
    public static int COLOR_LESSCONTRAST = 64;
    public static int COLOR_CONTRASTBOOST = 128;

    @Override
    public SonarWidgetState getState() {
        return (SonarWidgetState) super.getState();
    }

    private SonarWidgetServerRpc rpc = new SonarWidgetServerRpc() {
        private static final long serialVersionUID = 1L;

        private int resolveWidth(int index, int width) {
            if (sonar.getLength() < (index + width)) {
                width = (int) (sonar.getLength() - index);
            }

            return width;
        }

        @Override
        public void fetchSonarData(final int height, final int width,
                final int index) {

            if (sonar.getLength() < index) {
                return;
            }

            final int resolvedWidth = resolveWidth(index, width);

            Ping[] pingRange = null;
            try {
                pingRange = sonar.getPingRange(index, resolvedWidth);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String filename = String.format("frame%d-%d.jpg", index,
                    new Date().getTime());
            StreamResource streamResource = new StreamResource(
                    new StreamSource() {
                        @Override
                        public InputStream getStream() {
                            try {
                                ByteArrayOutputStream imagebuffer = new ByteArrayOutputStream();
                                ImageIO.write(
                                        createImage(sonar, index,
                                                resolvedWidth, height), "jpg",
                                        imagebuffer);
                                return new ByteArrayInputStream(imagebuffer
                                        .toByteArray());
                            } catch (IOException e) {
                                e.printStackTrace();
                                return null;
                            }
                        }
                    }, filename);

            setResource(filename, streamResource);

            String[] lowlimits = new String[pingRange.length];
            String[] depths = new String[pingRange.length];
            String[] temps = new String[pingRange.length];

            for (int loop = 0; loop < pingRange.length; loop++) {
                lowlimits[loop] = String.format("%.1f",
                        pingRange[loop].getLowLimit());
                depths[loop] = String
                        .format("%.1f", pingRange[loop].getDepth());
                temps[loop] = String.format("%.1f", pingRange[loop].getTemp());
            }

            getRpcProxy(SonarWidgetClientRpc.class).frameData(index, filename,
                    lowlimits, depths, temps);
        }

        @Override
        public void clicked(int coordinate) {
            fireEvent(new SelectedPingEvent(SonarWidget.this, coordinate));
        }

    };

    public SonarWidget(File file, Type preferredChannel) {
        registerRpc(this.rpc);
        try {
            String filenameExtension = file.getName().substring(
                    file.getName().length() - 3);
            if (filenameExtension.equalsIgnoreCase("sl2")) {
                sonar = new LowranceStructureScan(file, preferredChannel);
            } else if (filenameExtension.equalsIgnoreCase("slg")) {
                sonar = new LowranceSonar(file);
            } else if (filenameExtension.equalsIgnoreCase("dat")) {
                sonar = new HumminbirdSSI(file, preferredChannel);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        getState().pingCount = sonar.getLength();
        getState().sidescan = sonar.getType() == Type.eSideScan;
        setColor(0);
        setOverlay(false);
    }

    public interface SelectedPingListener extends Serializable {
        public static final Method BUTTON_CLICK_METHOD = ReflectTools
                .findMethod(SelectedPingListener.class, "selectPing",
                        SelectedPingEvent.class);

        public void selectPing(SelectedPingEvent event);
    }

    public static class SelectedPingEvent extends Component.Event {

        private int coordinate;
        private SonarWidget widget;

        public SelectedPingEvent(SonarWidget sonarWidget, int coordinate) {
            super(sonarWidget);
            this.widget = sonarWidget;
            this.coordinate = coordinate;
        }

        public Ping getPing() throws IOException {
            int index = (int) Math
                    .min(coordinate, widget.sonar.getLength() - 1);
            if (index < 0) {
                index = 0;
            }

            Ping[] pingRange = widget.sonar.getPingRange(index, 1);

            return pingRange[0];
        }
    }

    public void addSelectedPingListener(
            SelectedPingListener listener) {
        addListener(SelectedPingEvent.class, listener,
                SelectedPingListener.BUTTON_CLICK_METHOD);
    }

    public void removeSelectedPingListener(
            SelectedPingListener listener) {
        removeListener(SelectedPingEvent.class, listener,
                SelectedPingListener.BUTTON_CLICK_METHOD);
    }

    public void setOverlay(boolean booleanValue) {
        getState().overlay = booleanValue;
    }

    public void setColor(int color) {
        getState().color = color;
    }

    public void setRange(float range) {
        getState().range = range;
    }

    private BufferedImage createImage(Sonar sonar, int offset, int width,
            int height) {
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);

        try {
            Ping[] pings = sonar.getPingRange(offset, width);

            for (int loop = 0; loop < width; loop++) {

                byte[] soundings = pings[loop].getSoundings();

                for (int i = 0; i < height; i++) {
                    int mapped = (int) ((i * soundings.length) / (double) height);
                    byte sounding = soundings[mapped];
                    int color = (0xFF & sounding) | (0xFF00 & (sounding << 8))
                            | (0xFF0000 & (sounding << 16));
                    image.setRGB(loop, i, color);
                }
            }
        } catch (IOException e) {
            return image;
        }

        return image;
    }
}
