/*
   Copyright 2012-2015 Michael Pozhidaev <michael.pozhidaev@gmail.com>

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

package org.luwrain.util;

import java.util.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.luwrain.core.NullCheck;

public class Opds
{
	final static private int BUFFER_SIZE=32*1024;
	
    static public class Entry 
    {
	private String id;
	private String title;
	private String link;

	Entry(String id, String title, String link)
	{
	    this.id = id;
	    this.title = title;
	    this.link = link;
	    NullCheck.notNull(id, "id");
	    NullCheck.notNull(title, "title");
	    NullCheck.notNull(link, "link");
	}

	@Override public String toString()
	{
	    return title;
	}

	public String id()
	{
	    return id;
	}

	public String link()
	{
	    return link;
	}

	public String title()
	{
	    return title;
	}
    }

    static public class Directory
    {
	private Entry[] entries;

	Directory(Entry[] entries)
	{
	    this.entries = entries;
	    NullCheck.notNullItems(entries, "entries");
	}

	public Entry[] entries()
	{
	    return entries;
	}
    }

    static public class Result
    {
	public enum Errors {FETCH, PARSE, NOERROR, NEEDPAY};

	private Directory dir;
	private Errors error;
	// file link if result is not a directory entry and mime type of it
	private String filename;
	private String mime;

	Result(Errors error)
	{
	    this.error = error;
	    this.dir = null;
	    this.filename=null;
	    this.mime=null;
	}

	Result(Directory dir)
	{
	    NullCheck.notNull(dir, "dir");
	    this.error = Errors.NOERROR;
	    this.dir = dir;
	}
	Result(String filename, String mime)
	{
	    this.error=Errors.NOERROR;
	    this.dir=null;
	    this.filename=filename;
	    this.mime=mime;
	}

	public String getFileName()
	{
		return filename;
	}

	public String getMimeType()
	{
		return mime;
	}

	public Directory directory()
	{
	    return dir;
	}

	public Errors error()
	{
	    return error;
	}

	public boolean isDirectory()
	{
		return (error==Errors.NOERROR&&dir!=null);
	}
	public boolean isBook()
	{
		return (error==Errors.NOERROR&&filename!=null);
	}
	
    }

    static public Result fetch(URL url)
    {
	NullCheck.notNull(url, "url");
	final LinkedList<Entry> res = new LinkedList<Entry>();
	org.jsoup.nodes.Document doc = null;
	try {

		//final URLConnection con = url.openConnection();
		//con.setRequestProperty("User-Agent", "Mozilla/4.0");
		
		final Connection con=Jsoup.connect(url.toString());
		con.userAgent("Mozilla/4.0");
		con.timeout(60000);
		doc = con.get();
	}
	catch(UnsupportedMimeTypeException e)
	{
		String mime=e.getMimeType();
		String path=e.getUrl().trim();
		if(path.isEmpty()) path=url.toString();
		//System.out.println("* "+path);
		String p="";
		while(p.isEmpty()&&path.length()>0)
		{
			p=path.substring(path.lastIndexOf('/')+1,path.length());
			if(p.isEmpty())
				path=path.substring(0,path.length()-1);
			if(p.equals("download"))
			{
				p="";
				path=path.substring(0,path.length()-9);
			}
		}
		if(p.isEmpty())
		{
			
		} else
		if(p.indexOf('.')==-1)
		{ // add file name extension by mime type
			path=p;
			if(mime.equals("application/zip")) path+=".zip"; 
			if(mime.equals("application/rar")) path+=".rar"; 
			if(mime.equals("application/epub+zip")) path+=".epub";
			if(mime.equals("application/rtf")) path+=".rtf";
			// FIXME: remake app-reader to accept mime type
		}
		System.out.println("* "+path);
		String filename=path.replaceAll("[^a-zA-Z0-9\\.\\-\\\\\\/]","_");
		if(!(new File(filename).exists()))
		{ // not downloaded later
			try
			{
				final URLConnection con = url.openConnection();
				con.setRequestProperty("User-Agent", "Mozilla/4.0");
				System.out.println("* store to file: '"+filename+"' from '"+path+"'");
				FileWriter fw=new FileWriter(filename);
				FileOutputStream os = new FileOutputStream(filename);
				InputStream is=con.getInputStream();
				int bytesRead = -1;
				byte[] buffer = new byte[BUFFER_SIZE];
				while ((bytesRead = is.read(buffer)) != -1)
					os.write(buffer, 0, bytesRead);
				os.close();
				is.close();
				// open document
			} catch(IOException e1)
			{
			    e1.printStackTrace();
			    return new Result(Result.Errors.FETCH);
			}
		}
		return new Result(filename,mime);
	}
	catch (HttpStatusException e)
	{
		if(e.getStatusCode()==402)
			return new Result(Result.Errors.NEEDPAY);
	    e.printStackTrace();
	    return new Result(Result.Errors.FETCH);
	}
	catch (Exception e)
	{
	    e.printStackTrace();
	    return new Result(Result.Errors.FETCH);
	}
	try {

		for(org.jsoup.nodes.Element node:doc.getElementsByTag("entry"))
		{
			final Entry entry = parseEntry(node);
			if (entry != null)
			    res.add(entry);
		}
	}
	catch (Exception e)
	{
	    e.printStackTrace();
	    return new Result(Result.Errors.PARSE);
	}
	return new Result(new Directory(res.toArray(new Entry[res.size()])));
    }

    static private Entry parseEntry(Element el)
    {
	NullCheck.notNull(el, "el");
	try {
	    String id = "";
	    String title = "";
	    String link = "";

	    //Title
	    for(Element node:el.getElementsByTag("title"))
	    	title = node.text();

	    //ID
	    for(Element node:el.getElementsByTag("id"))
	    	id = node.text();

	    //Link
	    for(Element node:el.getElementsByTag("link"))
	    	link = node.attributes().get("href");

	    if (id != null && title != null && link != null)
		return new Entry(id, title, link);
	    return null;
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    return null;
	}
    }
}