Sonar widget for Vaadin
=======================

You have a fishfinder with ability to save your depth data? 
Need to share your depth logs visually? If you say yes
then this is the tool for you. Sonar widget for Vaadin
shows saved sonar logs in web browser. Upload raw sonar log
files to server and you're ready to start using Sonar widget.
API is simple and clean. Data is uploaded to web browser in
compressed format and only in demand.

## System requirements
* Vaadin 7.x
* HTML5 canvas compliant browser. Tested with Firefox 9, Chrome 19, Safari 5, IE 9

API documentation
-----------------

## Basic usage
	@Override
	public void init(VaadinRequest request) {
		VerticalLayout layout = new VerticalLayout();
		SonarWidget sonar = new SonarWidget(new File("/path/to/sonar/file.slg"), Type.eTraditional);
		sonar.setWidth("100%");
		sonar.setHeight("400px");
		layout.addComponent(sonar);
		setContent(layout);
	}
	

Planned features
---------------

Known issues
---------------
* Depth and temperature data will not display with Humminbird files
* Widget depth cursor is misaligned when browser window is vertically scrolled

Version history
---------------
## 0.0.5 (2015-04-08)
* Uniform stiching of different depths
* Automatic or manual depth range selection
* Demo app shows location in a map

## 0.0.4
* renamed com.vaadin.sonarwidget package to be org.vaadin.sonarwidget

## 0.0.3 (2013-02-01)
* Support for Humminbird DAT/SON format
* Colors
* Vaadin 7
 
## 0.0.2 (2012-08-06)
* Support for Lowrance SL2 format
* API changes to select channel if log has multiple of them

## 0.0.1 (2012-06-29)
* First release
* Supports Lowrance SLG format

Licensing
---------

Copyright 2012 Samuli Penttilä

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

* http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
