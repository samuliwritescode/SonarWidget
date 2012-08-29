package com.vaadin.sonarwidgetdemo.test;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

public class DemoServer {
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        WebAppContext ctx = new WebAppContext();

        ctx.setContextPath("/SonarWidget");
        ctx.setResourceBase("target/sonarwidget-demo-0.0.1/");
        ctx.setClassLoader(Thread.currentThread().getContextClassLoader());
        
        server.setHandler(ctx);
        server.start();
        server.join();
    }
}
