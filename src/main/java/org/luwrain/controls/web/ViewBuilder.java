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

import org.luwrain.core.*;
import org.luwrain.browser.*;
import org.luwrain.controls.web.WebArea.Callback.MessageType;

final class ViewBuilder
{
    final Model model;

    ViewBuilder(Model model)
    {
	NullCheck.notNull(model, "model");
	this.model = model;
    }

    View build()
    {
	for(Container c: model.getContainers())
	{
	    final ContainerRowsBuilder b = new ContainerRowsBuilder();
	    for(ContentItem i: c.getContent())
		processContentItem(b, i);
	}
	return null;
    }

    private void processContentItem(ContainerRowsBuilder builder, ContentItem item)
    {
	NullCheck.notNull(builder, "builder");
	NullCheck.notNull(item, "item");
	if (item.isText())
	{
	    builder.process(item);
	    return;
	}
	for(ContentItem i: item.getChildren())
	    processContentItem(builder, i);
    }

    
}