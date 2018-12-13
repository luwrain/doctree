/*
   Copyright 2012-2018 Michael Pozhidaev <michael.pozhidaev@gmail.com>
   Copyright 2015-2016 Roman Volovodov <gr.rPman@gmail.com>

   This file is part of LUWRAIN.

   LUWRAIN is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public
   License as published by the Free Software Foundation; either
   version 3 of the License, or (at your option) any later version.

   LUWRAIN is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
*/

package org.luwrain.controls.web;

import java.util.*;

import org.luwrain.core.*;
import org.luwrain.browser.*;
import org.luwrain.controls.web.WebArea.Callback.MessageType;

final class ViewBuilder
{
    static private final String LOG_COMPONENT = WebArea.LOG_COMPONENT;

    private final Container[] containers;

    ViewBuilder(Container[] containers)
    {
	NullCheck.notNullItems(containers, "containers");
	this.containers = containers;
    }

    View build(WebArea.Appearance appearance)
    {
	NullCheck.notNull(appearance, "appearance");
	calcGeom(100);
	final List<Container> viewContainers = new LinkedList();
	for(Container c: containers)
	{
	    final ContainerRowsBuilder b = new ContainerRowsBuilder();
	    for(ContentItem i: c.getContent())
		processContentItem(b, i);
	    b.commitRow();
	    c.setRows(b.rows.toArray(new ContainerRow[b.rows.size()]));
	    if (c.getRowCount() > 0)
		viewContainers.add(c); else
		Log.warning(LOG_COMPONENT, "the container <" + c.tagName + "> without rows (has " + c.getContent().length + " content items)" + System.lineSeparator() + c.treeItem.toString());
	}
	for(Container c: containers)
	    c.actualTextY = false;
		for(Container c: containers)
		    c.calcActualTextY();
		return new View(appearance, viewContainers.toArray(new Container[viewContainers.size()]));
    }

    private void processContentItem(ContainerRowsBuilder builder, ContentItem item)
    {
	NullCheck.notNull(builder, "builder");
	NullCheck.notNull(item, "item");
	if (builder.process(item))
	    return;
	for(ContentItem i: item.getChildren())
	    processContentItem(builder, i);
    }

    private void calcGeom(int width)
    {
	if (width < 10)
	    throw new IllegalArgumentException("width (" + width + ") may not be less than 10");
	calcTextXAndWidth(width);
	calcTextY();
	for(int i = 0;i < containers.length;++i)
	    for(int j = 0;j < containers.length;++j)
		if (i != j)
		    if (containers[i].intersectsText(containers[j]))
		{
		    Log.warning(LOG_COMPONENT, "intersecting containers with numbers " + i + " and " + j);
		    Log.warning(LOG_COMPONENT, "container #" + i + ":" + containers[i].toString());
		    		    Log.warning(LOG_COMPONENT, "container #" + j + ":" + containers[j].toString());
		}
		for(int i = 0;i < containers.length;++i)
	    for(int j = 0;j < containers.length;++j)
		if (i != j)
	    {
		final Container ci = containers[i];
		final Container cj = containers[j];
		if (cj.textY <= ci.textY)
		    continue;
		if (Container.intersects(ci.textX, ci.textWidth, cj.textX, cj.textWidth))
		    cj.vertDepOn.add(ci);
	    }
    }

    private void calcTextXAndWidth(int width)
    {
	int graphicalWidth = 0;
	for(Container c: containers)
	    graphicalWidth = Math.max(graphicalWidth, c.x + c.width);
	Log.debug(LOG_COMPONENT, "graphical width is " + graphicalWidth);
	final float ratio = (float)graphicalWidth / width;
	Log.debug(LOG_COMPONENT, "ratio is " + String.format("%.2f", ratio));
	for(Container c: containers)
	{
	    final float textX = (float)c.x / ratio;
	    c.textX = new Float(textX).intValue();
	    final float textWidth = (float)c.width / ratio;
	    c.textWidth = new Float(textWidth).intValue();
	}
    }

    private void calcTextY()
    {
	int topLevel = 0;
	int nextTextY = 0;
	while(true)
	{
	    int baseContIndex = -1;
	    for(int i = 0;i < containers.length;++i)
	    {
		final Container c = containers[i];
		//Checking if the container already has the text Y
		if (c.textY >= 0)
		    continue;
		if (c.y < topLevel)
		    continue;
		if (baseContIndex < 0)
		    baseContIndex = i;
		if (c.y < containers[baseContIndex].y)
		{
		    baseContIndex = i;
		    continue;
		}
	    }
	    //Checking if all containers were processed
	    if (baseContIndex < 0)
		return;
	    containers[baseContIndex].textY = nextTextY;
	    final Container chosenContainer = containers[baseContIndex];
	    Log.debug("building", "chosen for " + nextTextY + " is " + chosenContainer.toString());
	    nextTextY++;
	    //Checking if there are some more non-overlapping containers located vertically closely
	    final List<Container> closeContList = new LinkedList();
	    for(int k = 0;k < containers.length;++k)
	    {
		if (k == baseContIndex)
		    continue;
		final Container c2 = containers[k];
		if (c2.textY >= 0)//already has textY
		    continue;
		if (chosenContainer.intersectsGraphically(c2))
		    continue;
		final int diff = chosenContainer.y - c2.y;
		if (diff > -16 && diff < 16)
		    closeContList.add(c2);
	    }
	    if (closeContList.isEmpty())
		continue;
	    final Container[] closeCont = closeContList.toArray(new Container[closeContList.size()]);
	    Arrays.sort(closeCont, (o1, o2)->{
		    final Container c1 = (Container)o1;
		    		    final Container c2 = (Container)o2;
				    if (c1.y < c2.y)
					return -1;
				    if (c1.y < c2.y)
					return 1;
				    final int sq1 = c1.getGraphicalSquare();
				    				    final int sq2 = c2.getGraphicalSquare();
				    if (sq1 == 0 && sq2 != 0)
					return -1;
				    if (sq1 != 0 && sq2 == 0)
					return 1;
				    return 0;
		});
	    closeCont[0].textY = chosenContainer.textY;
	    /*
	    for(Container cc: closeCont)
		Log.debug("building", "near " + cc.toString());
	    */
	    for(int k = 1;k < closeCont.length;++k)
	    {
		if (closeCont[k].textY >= 0)
		    throw new RuntimeException("Considering the previously used container");
		int kk = 0;
		for(kk = 0;kk < k;++kk)
		{
		    //Log.debug("building", "" + closeCont[kk].textY);
		    if (closeCont[kk].textY >= 0 && closeCont[k].intersectsGraphically(closeCont[kk]))
			break;
		}
		//Log.debug("building", "k=" + k + ",kk=" + kk);
		if (kk < k)//We have an intersection with one of the previously use container
		    continue;
		closeCont[k].textY = chosenContainer.textY;
	    }
	} //for(containers)
    }
}
