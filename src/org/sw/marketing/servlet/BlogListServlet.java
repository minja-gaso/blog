package org.sw.marketing.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.sw.marketing.dao.blog.BlogDAO;
import org.sw.marketing.dao.blog.DAOFactory;
import org.sw.marketing.dao.blog.skin.BlogSkinDAO;
import org.sw.marketing.dao.blog.topic.BlogTopicDAO;
import org.sw.marketing.data.blog.Data;
import org.sw.marketing.data.blog.Data.Blog;
import org.sw.marketing.data.blog.Data.Blog.Topic;
import org.sw.marketing.data.blog.Environment;
import org.sw.marketing.data.blog.Skin;
import org.sw.marketing.transformation.TransformerHelper;
import org.sw.marketing.util.ReadFile;

@WebServlet("/list/*")
public class BlogListServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	protected void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{ 
		boolean prettyUrl = false;
		long blogID = 0;
		String prettyUrlStr = null;
		try
		{
			blogID = Long.parseLong(request.getPathInfo().substring(1));
		}
		catch(NumberFormatException e)
		{
			prettyUrl = true;
			prettyUrlStr = request.getPathInfo().substring(1);
		}
		
		BlogDAO blogDAO = DAOFactory.getBlogDAO();
		BlogTopicDAO topicDAO = DAOFactory.getBlogTopicDAO();
//		CalendarCategoryDAO categoryDAO = DAOFactory.getCalendarCategoryDAO();
		
		Data data = new Data();
		Blog blog = null;
		
		if (prettyUrl)
		{
			blog = blogDAO.getBlogByPrettyUrl(prettyUrlStr);
		}
		else
		{
			blog = blogDAO.getBlog(blogID);
		}
		
		if(blog != null)
		{
			java.util.List<Topic> topics = topicDAO.getBlogTopics(blog.getId());
			if(topics != null)
			{
				blog.getTopic().addAll(topics);
			}			
//			java.util.List<Category> categories = categoryDAO.getCategories(blog.getId());
//			if(categories !=  null)
//			{
//				blog.getCategory().addAll(categories);
//			}
			
			data.getBlog().add(blog);
		}
		else
		{
			response.getWriter().println("The calendar you are looking for could not be found.");
			return;
		}
		
		Environment environment = new Environment();
		environment.setScreenName("LIST");		
		data.setEnvironment(environment);

		/*
		 * generate output
		 */
		TransformerHelper transformerHelper = new TransformerHelper();
		transformerHelper.setUrlResolverBaseUrl(getServletContext().getInitParameter("assetXslUrl"));
		String xmlStr = transformerHelper.getXmlStr("org.sw.marketing.data.blog", data);
		String xslScreen = getServletContext().getInitParameter("assetXslPath") + "list.xsl";
		String xslStr = ReadFile.getSkin(xslScreen);
		String htmlStr = transformerHelper.getHtmlStr(xmlStr, new ByteArrayInputStream(xslStr.getBytes()));
		
		String toolboxSkinPath = getServletContext().getInitParameter("assetPath") + "toolbox_1col.html";
		String skinHtmlStr = null;
		
		BlogSkinDAO skinDAO = DAOFactory.getBlogSkinDAO();
		String paramSkinID = request.getParameter("skinID");
		long skinID = blog.getFkSkinId();
		if(paramSkinID != null)
		{
			try
			{
				skinID = Long.parseLong(paramSkinID);
			}
			catch(NumberFormatException e)
			{
				//
			}
		}
		
		
		Skin skin = null;
		if(skinID > 0)
		{
			skin = skinDAO.getSkin(skinID);
		}
		
		if(skin != null)
		{
			skinHtmlStr = skin.getSkinHtml();
		}
		else
		{
			skinHtmlStr = ReadFile.getSkin(toolboxSkinPath);
		}
		skinHtmlStr = skinHtmlStr.replace("{TITLE}", blog.getTitle());
		skinHtmlStr = skinHtmlStr.replace("{CONTENT}", htmlStr);

//		String skinUrl = calendar.getSkinUrl();
//		String skinCssSelector = calendar.getSkinSelector();
//
//		if (skinUrl.length() > 0 && skinCssSelector.length() > 0)
//		{
//			skinHtmlStr = SkinReader.getSkinByUrl(calendar.getSkinUrl(), calendar.getSkinSelector());
//		}
//		else
//		{
//			skinHtmlStr = ReadFile.getSkin(toolboxSkinPath);
//		}
		
		if(skin != null)
		{
			Element styleElement = new Element(org.jsoup.parser.Tag.valueOf("style"), "");
			String skinCss = skin.getSkinCssOverrides() + skin.getCalendarCss();
			styleElement.text(skinCss);
			String styleElementStr = styleElement.toString();
			styleElementStr = styleElementStr.replaceAll("&gt;", ">").replaceAll("&lt;", "<");
			skinHtmlStr = skinHtmlStr.replace("{CSS}", styleElementStr);			
		}
		
		System.out.println(xmlStr);
		response.setCharacterEncoding("utf-8");
//		response.getWriter().println(skinHtmlStr);
		response.getWriter().println(htmlStr);
		
//		Gson gson = new GsonBuilder().setPrettyPrinting().create();
//		String json = gson.toJson(data);
//		System.out.println(json);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		process(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		process(request, response);
	}

}
