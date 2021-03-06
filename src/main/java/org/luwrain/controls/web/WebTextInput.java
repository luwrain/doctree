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

public final class WebTextInput extends WebObject
{
private String text = "";
    private final int width;

    WebTextInput(ContentItem contentItem, int width)
    {
	super(contentItem);
	this.text = contentItem.getText();
	this.width = width;
	if (width <= 0)
	    throw new IllegalArgumentException("width (" + width + ") must be greater than zero");
    }

        @Override public int getWidth()
    {
	return width;
    }

public String getText()
    {
	return text;
    }

    public void setText(String text)
    {
	NullCheck.notNull(text, "text");
	contentItem.it.setText(text);
	this.text = text;
    }
}
