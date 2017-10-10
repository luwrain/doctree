/*
   Copyright 2012-2017 Michael Pozhidaev <michael.pozhidaev@gmail.com>
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

package org.luwrain.browser.docbuilder;

import java.awt.Rectangle;
import java.net.*;
import java.util.*;

import org.luwrain.core.*;
import org.luwrain.doctree.*;
import org.luwrain.browser.*;
import org.luwrain.browser.selectors.*;

public class DocumentBuilder
{
    static final String LOG_COMPONENT = "browser-builder";

    private final Browser browser;
    private final URL baseUrl;
    private final Prenode prenodesRoot = new Prenode();

    LinkedList<Integer> watch = new LinkedList<Integer>();

    public DocumentBuilder(Browser browser)
    {
	NullCheck.notNull(browser, "browser");
	this.browser = browser;
	this.baseUrl = prepareBaseUrl(browser);
	new PrenodeTreeBuilder(browser, prenodesRoot).build();
    }

    public Document build()
    {
	//	watch.clear();
	while(Cleaning.clean(prenodesRoot) != 0){}
	return makeDocument();
    }

    private Document makeDocument()
    {
	final Node root=NodeFactory.newNode(Node.Type.ROOT);
	final Node[] subnodes = makeNodes(prenodesRoot);
	root.setSubnodes(subnodes);
final org.luwrain.doctree.Document res = new Document(root);
return res;
    }

    private Node[] makeNodes(Prenode prenode)
    {
	NullCheck.notNull(prenode, "prenode");
	Log.debug(LOG_COMPONENT, "makeNodes(" + prenode.tagName + ")");
	final List<Node> res = new LinkedList<Node>();
	final List<Run> runs = new LinkedList<Run>();
	Rectangle rect = null;
	for(ItemWrapper w: makeWrappers(prenode))
	{
	    if(w.isNode())
	    {
		res.add(createPara(runs));
		res.add(w.node);
		continue;
	    }
	    if(rect == null)
		rect = w.nodeInfo.browserIt.getRect();
	    final Rectangle curRect = w.nodeInfo.browserIt.getRect();
	    if(!((curRect.y>=rect.y&&curRect.y<rect.y+rect.height)
		 ||(rect.y>=curRect.y&&rect.y<curRect.y+curRect.height)))
		res.add(createPara(runs));
	    runs.add(w.run);
	    rect = curRect;
	} //for(wrappers)
	res.add(createPara(runs));
	return res.toArray(new Node[res.size()]);
    }

    private Paragraph createPara(List<Run> runs)
    {
	NullCheck.notNull(runs, "run");
	final Paragraph para = NodeFactory.newPara();
	para.runs = runs.toArray(new Run[runs.size()]);
	runs.clear();
	return para;
    }

    private ItemWrapper[] makeWrappers(Prenode node)
    {
	NullCheck.notNull(node, "node");
		Log.debug(LOG_COMPONENT, "makeWrappers(" + node.tagName + ")");
		NullCheck.notNull(node.children, "node.children");
	if(node.children.isEmpty())
	    return new ItemWrapper[]{makeLeafWrapper(node)};
	final List<ItemWrapper> res = new LinkedList<ItemWrapper>();
	final BrowserIterator it = node.browserIt;
	final String tagName = node.tagName;
	switch(tagName)
	{
	case "ol":
	case "ul":
	    {
	    final Node listNode = NodeFactory.newNode(tagName.equals("ol")?Node.Type.ORDERED_LIST:Node.Type.UNORDERED_LIST);
	final List<Node> listItems = new LinkedList<Node>();
	for(Prenode child: node.children)
	{
	    final Node item = NodeFactory.newNode(Node.Type.LIST_ITEM);
	    item.setSubnodes(makeNodes(child));
	    listItems.add(item);
	}
	listNode.setSubnodes(listItems.toArray(new Node[listItems.size()]));
	res.add(new ItemWrapper(listNode,node));
	break;
	}

			case "h1":
		    		case "h2":
				    		case "h3":
				    		case "h4":
				    		case "h5":
				    		case "h6":
				    		case "h7":
				    		case "h8":
				    		case "h9":
				    {
					Log.debug(LOG_COMPONENT, "header " + tagName);

					final Node sect = NodeFactory.newSection(1);//FIXME:proper section level
					final List<Node> subnodes = new LinkedList<Node>();
					for(Prenode p: node.children)
					    for(Node n: makeNodes(p))
						subnodes.add(n);
					sect.setSubnodes(subnodes.toArray(new Node[subnodes.size()]));
					res.add(new ItemWrapper(sect, node));
					break;
				    }


	


	case "table": // table can be mixed with any other element, for example parent form
	case "tbody": // but if tbody not exist, table would exist as single, because tr/td/th can't be mixed
	    res.add(createRunInfoForTable(node));
	break;

	case "div":
	    {
			    for(Prenode child: node.children)
		for (ItemWrapper childToAdd: makeWrappers(child))
		    res.add(childToAdd);

			    break;			    
	    }
	    
	default:
	    Log.warning(LOG_COMPONENT, "unknown block tag:" + tagName);
	    for(Prenode child: node.children)
		for (ItemWrapper childToAdd: makeWrappers(child))
		    res.add(childToAdd);
	}
	return res.toArray(new ItemWrapper[res.size()]);
    }

    private ItemWrapper createRunInfoForTable(Prenode tableNodeInfo)
    {
	NullCheck.notNull(tableNodeInfo, "tableNodeInfo");
	final LinkedList<LinkedList<Node>> table = new LinkedList<LinkedList<Node>>();
	//All children are rows, no additional checking is required 
	for(Prenode rowNodeInfo: tableNodeInfo.children)
	{ // each rows contains a table cell or header cell, but also we can see tbody, tfoor, thead, we must enter into
	    final LinkedList<Node> row = new LinkedList<Node>();
	    // detect thead, tbody, tfoot
Prenode child_ = rowNodeInfo;
	    final String tagName = rowNodeInfo.browserIt.getHtmlTagName().toLowerCase();
	    switch(tagName)
	    {
	    case "thead":
	    case "tfoot":
	    case "tbody":
		// check child exist, if not, skip this row at all
		if(!rowNodeInfo.children.isEmpty())
		    child_ = rowNodeInfo.children.firstElement();
	    // we must go out here but we can pass this alone child next without errors 
	    break;
	    }
	    //Cells
	    for(Prenode cellChild: child_.children)
	    {
		//collspan detection
		final String tagName2 = cellChild.browserIt.getHtmlTagName().toLowerCase();
		String collSpanStr = null;
		switch(tagName2)
		{
		case "td":
		case "th":
		    collSpanStr = cellChild.browserIt.getAttribute("colspan");
		//break; // no we can skip this check becouse we don't known what to do if we detect errors here
		default:
		    Integer collSpan=null;
		    if(collSpanStr != null) 
			try {
					      collSpan=Integer.parseInt(collSpanStr);
					  } 
catch(NumberFormatException e)
			{} // we can skip this error
		    // add node
		    final Node cellNode=NodeFactory.newNode(Node.Type.TABLE_CELL);
		    //Make a recursive call of makeNodes()
		    final Node[] cellNodes = makeNodes(cellChild);
		    for(Node nn: cellNodes)
		    row.add(nn);
		    // emulate collspan FIXME: make Document table element usable with it
		    if(collSpan != null)
		    { // we have colspan, add empty colls to table row
			for(;collSpan>0;collSpan--)
			{
			    Node emptyCellNode=NodeFactory.newNode(Node.Type.TABLE_CELL);
			    Paragraph emptyPar=NodeFactory.newPara("");
			    emptyCellNode.setSubnodes(new Node[]{emptyPar});
			    row.add(emptyCellNode);
			}
		    }
		    break;	
		}
	    }
	    table.add(row);
	}
	// add empty cells to make table balanced by equal numbers of colls each row
	// move multiple colls to section inside of it
	// [col1,col2,col3,col4,col5,col6] -> [{col1,col2},{col3,col4},{col5,col6}]
	// call setSubnodes each one row
	Node tableNode = NodeFactory.newNode(Node.Type.TABLE);
	final LinkedList<Node> tableRowNodes = new LinkedList<Node>();
	for(LinkedList<Node> rows: table)
	{
	    final Node tableRowNode = NodeFactory.newNode(Node.Type.TABLE_ROW);
	    tableRowNode.setSubnodes(rows.toArray(new Node[rows.size()]));
	    tableRowNodes.add(tableRowNode);
	}
	tableNode.setSubnodes(tableRowNodes.toArray(new Node[tableRowNodes.size()]));
	return new ItemWrapper(tableNode, tableNodeInfo);
    }

    private ItemWrapper makeLeafWrapper(Prenode prenode)
    {
	NullCheck.notNull(prenode, "prenode");
	final BrowserIterator it = prenode.browserIt;
	final String text = it.getText() != null?it.getText():"";
	final String tagName = prenode.tagName;
	switch(tagName)
	{
	    //	case "img":
	    //	case "video":
	case "input":
	case "select":
	case "button":
return onFormItem(prenode, tagName);
	case "#text":
	    //FIXME:href
	    	    return new ItemWrapper(new TextRun(text), prenode);
	default:
	    return new ItemWrapper(new TextRun("FIXME:UNKNOWN TAG:" + tagName + ":" + text), prenode);
	}
    }

    	    /*
	if(!nodeInfo.mixed.isEmpty())
	{ // check for A tag inside mixed
	    for(BrowserIterator e: nodeInfo.getMixedinfo())
	    {
		final String etag = e.getHtmlTagName().toLowerCase();
		if(etag.equals("video"))
		{
		    Log.debug(LOG_COMPONENT, "video found");
		}
		if(etag.equals("a"))
		{
		    final String url;
		    final String urlSrc = e.getAttribute("href");
		    if (urlSrc != null && !urlSrc.trim().isEmpty())
			url = constructUrl(urlSrc); else
			url = null;
		    if (url != null)
			Log.debug("href", url);
		    //		    txt = txt;
		    webInfo = new WebInfo(WebInfo.ActionType.CLICK, nodeInfo.browserIt);
		    break;
		} else
		    if(etag.equals("button"))
		    {
			txt = "Button: "+txt;
			webInfo = new WebInfo(WebInfo.ActionType.CLICK, nodeInfo.browserIt);
			break;
		    }
	    }
	    */


    private ItemWrapper onFormItem(Prenode prenode, String tagName)
{
    NullCheck.notNull(prenode, "prenode");
    NullCheck.notEmpty(tagName, "tagName");
    final BrowserIterator it = prenode.browserIt;
    NullCheck.notNull(it, "it");
    if (tagName.equals("input"))
    {
	    final String type = it.getAttribute("type");
	    if (type == null || type.trim().isEmpty())
						return new ItemWrapper(new EditRun(it), prenode);
	    switch(type)
	    {
		/*
	    case "image":
	    case "button":
	    case "radio":
	    case "checkbox":
		*/
	    case "submit":
						return new ItemWrapper(new ButtonRun(it), prenode);
	    case "text":
				return new ItemWrapper(new EditRun(it), prenode);
	    default:
		Log.warning(LOG_COMPONENT, "unknown input type:" + type);
		return new ItemWrapper(new TextRun("FIXME:UNKNOWN INPUT:" + type + ":" + it.getText()), prenode);
	    }
    }
    switch(tagName)
    {
	case "button":
	    //	    txt = "Button: " + it.getText();
	    //	    webInfo = new WebInfo(WebInfo.ActionType.CLICK, nodeInfo.browserIt);
	    break;
	case "select":
	    //	    txt = "Select: " + it.getText();
	    //	    webInfo = new WebInfo(WebInfo.ActionType.SELECT, nodeInfo.browserIt);
	    break;
	default:
	    //	    txt = it.getText();
	    //	    break;
	    		return new ItemWrapper(new TextRun("FIXME:UNKNOWN INPUT TAG:" + tagName + ":" + it.getText()), prenode);
	}
    	    		return new ItemWrapper(new TextRun("FIXME:UNKNOWN INPUT TAG:" + tagName + ":" + it.getText()), prenode);
}

    private String constructUrl(String url)
    {
	NullCheck.notNull(url, "url");
	if (baseUrl == null)
	    return url;
	try {
	    return new URL(baseUrl, url).toString();
	}
	catch(MalformedURLException e)
	{
	    return url;
	}
    }

    static private URL prepareBaseUrl(Browser browser)
    {
	NullCheck.notNull(browser, "browser");
	final String openedUrl = browser.getUrl();
	if (openedUrl == null || openedUrl.trim().isEmpty())
	    return null;
	try {
	    return new URL(openedUrl);
	    }
	catch(MalformedURLException e)
	{
	    return null;
	}
    }

    static private String cleanupText(String txt)
    {
	return txt.replace("/\\s+/g"," ").trim();
    }
}
