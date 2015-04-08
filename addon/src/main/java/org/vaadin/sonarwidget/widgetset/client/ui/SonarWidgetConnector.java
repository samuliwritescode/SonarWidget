package org.vaadin.sonarwidget.widgetset.client.ui;

import org.vaadin.sonarwidget.SonarWidget;

import com.vaadin.client.communication.RpcProxy;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.client.ui.layout.ElementResizeEvent;
import com.vaadin.client.ui.layout.ElementResizeListener;
import com.vaadin.shared.ui.Connect;

@Connect(SonarWidget.class)
public class SonarWidgetConnector extends AbstractComponentConnector implements
        ElementResizeListener, VSonarWidget.ClickListener {

    private static final long serialVersionUID = 1L;
    SonarWidgetServerRpc serverRpc = RpcProxy
            .create(SonarWidgetServerRpc.class, this);

    public SonarWidgetConnector() {
        registerRpc(SonarWidgetClientRpc.class, new SonarWidgetClientRpc() {
            private static final long serialVersionUID = 1L;

            @Override
            public void frameData(int offset, String pic, String[] lowlimits,
                    String[] depths, String[] temps) {
                String resourceUrl = getResourceUrl(pic);
                getWidget().setOffset(offset, resourceUrl, lowlimits, depths,
                        temps);
            }
        });

        getWidget().setConnector(this);
        getWidget().setState(getState());
    }

    @Override
    public VSonarWidget getWidget() {
        return (VSonarWidget) super.getWidget();
    }

    @Override
    public SonarWidgetState getState() {
        return (SonarWidgetState) super.getState();
    }

    @Override
    public void onStateChanged(StateChangeEvent event) {
        super.onStateChanged(event);
        getWidget().setState(getState());
        getWidget().initializeCanvases((int) getState().pingCount);
    }

    public void getData(int height, int width, int index) {
        serverRpc.fetchSonarData(height, width, index);
    }

    @Override
    public void onElementResize(ElementResizeEvent e) {
        getWidget().setDirty();
    }

    @Override
    public void onUnregister() {
        super.onUnregister();
        getLayoutManager().removeElementResizeListener(
                getWidget().getElement(), this);
        getWidget().setListener(null);
    }

    @Override
    protected void init() {
        super.init();
        getLayoutManager().addElementResizeListener(getWidget().getElement(),
                this);

        getWidget().setListener(this);
    }

    @Override
    public void onClick(int coordinate) {
        serverRpc.clicked(coordinate);
    }
}
