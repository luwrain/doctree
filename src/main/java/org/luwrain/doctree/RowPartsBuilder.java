/*
   Copyright 2012-2015 Michael Pozhidaev <michael.pozhidaev@gmail.com>
   Copyright 2015 Roman Volovodov <gr.rPman@gmail.com>

   This file is part of the LUWRAIN.

   LUWRAIN is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public
   License as published by the Free Software Foundation; either
   version 3 of the License, or (at your option) any later version.

   LUWRAIN is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
*/

package org.luwrain.doctree;

import java.util.*;
import org.luwrain.core.NullCheck;
import org.luwrain.core.Log;

public class RowPartsBuilder
{
    private final LinkedList<RowPart> parts = new LinkedList<RowPart>();
    private final LinkedList<RowPart> currentParaParts = new LinkedList<RowPart>();
    private final LinkedList<ParagraphImpl> paragraphs = new LinkedList<ParagraphImpl>();

    /** The index of the next row to be added to the current paragraph*/
    private int index = 0;

    /** Number of characters in current incomplete row*/
    private int offset = 0;

    void onNode(NodeImpl node)
    {
	onNode(node, 0);
    }

    private void onNode(NodeImpl node, int width)
    {
	NullCheck.notNull(node, "node");
	if (node.type == Node.Type.PARAGRAPH && (node instanceof ParagraphImpl))
	{
	    offset = 0;
	    index = 0;
	    final ParagraphImpl para = (ParagraphImpl)node;
	    currentParaParts.clear();
	    if (para.runs != null)
		for(Run r: para.runs)
		    onRun(r, width > 0?width:para.width);
	    if (!currentParaParts.isEmpty())
	    {
		para.rowParts = currentParaParts.toArray(new RowPart[currentParaParts.size()]);
		paragraphs.add(para);
		for(RowPart p: currentParaParts)
		    parts.add(p);
	    }
	    return;
	}
	if (node.subnodes != null)
	    for(NodeImpl n: node.subnodes)
		onNode(n);
    }

    //Removes spaces only on row breaks and only if after the break there are non-spacing chars;
    private void onRun(Run run, int maxRowLen)
    {
	final String text = run.text;
	NullCheck.notNull(text, "text");
	if (text.isEmpty())
	    return;
	int posFrom = 0;
	while (posFrom < text.length())
	{
	    final int available = maxRowLen - offset;//Available space on current line
	    if (available <= 0)
	    {
		//Try again on the next line
		++index;
		offset = 0;
		continue;
	    }
	    final int remains = text.length() - posFrom;
	    //Both remains and available are greater than zero
	    if (remains <= available)
	    {
		//We have a chunk for the last row for this run
		currentParaParts.add(makeRunPart(run, posFrom, text.length()));
		offset += remains;
		posFrom = text.length();
		continue;
	    }
	    int posTo = posFrom;
	    int nextWordEnd = posTo;
	    while (nextWordEnd - posFrom <= available)
	    {
		posTo = nextWordEnd;//It is definitely before the row end
		while (nextWordEnd < text.length() && Character.isSpace(text.charAt(nextWordEnd)))//FIXME:nbsp
		    ++nextWordEnd;
		while (nextWordEnd < text.length() && !Character.isSpace(text.charAt(nextWordEnd)))//FIXME:nbsp
		    ++nextWordEnd;
	    }
	    if (posTo == posFrom)//No word ends before the end of the row
	    {
		if (offset > 0)
		{
		    //Trying to do the same once again from the beginning of the next line in hope a whole line is enough
		    offset = 0;
		    ++index;
		    continue;
		}
		//The only thing we can do is split the line in the middle of the word, no another way
		posTo = posFrom + available;
	    }
	    if (posFrom == posTo)
		Log.warning("doctree", "having posFrom equal to posTo (" + posFrom + ")");
	    if (posTo - posFrom > available)
		Log.warning("doctree", "getting the line with length greater than line length limit");
	    currentParaParts.add(makeRunPart(run, posFrom, posTo));
	    ++index;
	    offset = 0;
	    posFrom = posTo;
	    //Trying to find the beginning of the next word
	    final int rollBack = posFrom;
	    while (posFrom < text.length() && Character.isSpace(text.charAt(posFrom)))
		++posFrom;
	    if (posFrom >= text.length())
		posFrom = rollBack;
	}
    }

    private RowPart makeRunPart(Run run,
				int posFrom, int posTo)
    {
	final RowPart part = new RowPart();
	part.run = run;
	part.relRowNum = index;
	part.posFrom = posFrom;
	part.posTo = posTo;
	return part;
    }

    RowPart[] parts()
    {
	return parts.toArray(new RowPart[parts.size()]);
    }

    ParagraphImpl[] paragraphs()
    {
	return paragraphs.toArray(new ParagraphImpl[paragraphs.size()]);
    }

    static public String[] paraToLines(ParagraphImpl para, int width)
    {
	NullCheck.notNull(para, "para");
	final RowPartsBuilder builder = new RowPartsBuilder();
	builder.onNode(para, width);
	final RowPart[] parts = builder.parts();
	    for(RowPart r: parts)
		r.absRowNum = r.relRowNum;
	final RowImpl[] rows = RowImpl.buildRows(parts);
	final LinkedList<String> lines = new LinkedList<String>();
	for(RowImpl r: rows)
	    lines.add(r.text(parts));
	return lines.toArray(new String[lines.size()]);
    }
}
