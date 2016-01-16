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

import java.util.HashMap;

import org.luwrain.core.NullCheck;
import org.luwrain.core.Log;

public class Document 
{
    private NodeImpl root;
    private String title;
    private Layout layout = new Layout(this);

    private ParagraphImpl[] paragraphs; //Only paragraphs which appear in document, no paragraphs without row parts
    public RowPart[] rowParts;
    private RowImpl[] rows;

    public HashMap<String,NodeImpl> idx=new HashMap<String,NodeImpl>();

    public Document(NodeImpl root)
    {
	this.root = root;
	NullCheck.notNull(root, "root");
	title = "";
    }

    public Document(String title, NodeImpl root)
    {
	this.root = root;
	this.title = title;
	NullCheck.notNull(root, "root");
	NullCheck.notNull(title, "title");
    }

    public void buildView(int width)
    {
	try {
	    Log.debug("doctree", "building view for width=" + width);
	    root.commit();
	    root.setEmptyMark();
	    root.removeEmpty();
	    root.calcWidth(width);
	    final RowPartsBuilder rowPartsBuilder = new RowPartsBuilder();
	    rowPartsBuilder.onNode(root);
	    rowParts = rowPartsBuilder.parts();
	    if (rowParts == null)
		rowParts = new RowPart[0];
	    Log.debug("doctree", "" + rowParts.length + " row parts prepared");
	    if (rowParts.length <= 0)
		return;
	    paragraphs = rowPartsBuilder.paragraphs();
	    Log.debug("doctree", "" + paragraphs.length + " paragraphs prepared");
	    root.calcHeight();
	    calcAbsRowNums();
	    root.calcPosition();
	    rows = RowImpl.buildRows(rowParts);
	    Log.debug("doctree", "" + rows.length + " rows prepared");
	    layout.calc();
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	}
    }

    public int getLineCount()
    {
	return layout.getLineCount();
    }

    public String getLine(int index)
    {
	return layout.getLine(index);
    }

    public boolean checkConsistency(boolean stopImmediately)
    {
	boolean ok = true;
	//All paragraphs must have valid parent node;
	for(ParagraphImpl p: paragraphs)
	    if (p.parentNode == null)
	{
	    System.out.println("warning::doctree:have a paragraph with an empty parent node");
	    if (stopImmediately)
		return false;
	    ok = false;
	}
	return ok;
    }

    public Iterator getIterator()
    {
	return new Iterator(this);
    }

    public String getTitle()
    {
	return title != null?title:"";
    }

    public NodeImpl getRoot()
    {
	return root;
    }

    public ParagraphImpl[] getParagraphs()
    {
	return paragraphs;
    }

    public RowImpl[] getRows()
    {
	return rows;
    }

    public RowPart[] getRowParts()
    {
	return rowParts;
    }

    private void calcAbsRowNums()
    {
	int currentParaTop = 0;
	for(ParagraphImpl p: paragraphs)
	{
	    p.topRowIndex = currentParaTop;
	    for(RowPart r: p.rowParts)
		r.absRowNum = r.relRowNum + currentParaTop;
	    //currentParaTop += (p.height + (p.shouldHaveExtraLine()?1:0));
	    currentParaTop += p.height;
	}
    }
    
    // recreate hash map index for all node's ids
    public void makeIndex()
    {
    	idx=new HashMap<String,NodeImpl>();
    	makeIndex(root);
    }
    public void makeIndex(NodeImpl node)
    {
    	// TODO: duplicate id in document, it would but can not be supported, make decision, what to do with this
    	if(node.id!=null) idx.put(node.id,node);
    	for(NodeImpl n:node.subnodes)
    		makeIndex(n);
    }
}
