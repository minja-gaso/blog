package org.sw.marketing.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.nodes.Element;
import org.sw.marketing.dao.blog.BlogDAO;
import org.sw.marketing.dao.blog.DAOFactory;
import org.sw.marketing.dao.blog.skin.BlogSkinDAO;
import org.sw.marketing.dao.blog.topic.BlogTopicDAO;
import org.sw.marketing.dao.blog.topic.BlogTopicTagDAO;
import org.sw.marketing.data.blog.Data;
import org.sw.marketing.data.blog.Data.Blog;
import org.sw.marketing.data.blog.Data.Blog.Topic;
import org.sw.marketing.data.blog.Data.Blog.Topic.Tag;
import org.sw.marketing.data.blog.Environment;
import org.sw.marketing.data.blog.Skin;
import org.sw.marketing.transformation.TransformerHelper;
import org.sw.marketing.util.ReadFile;
import org.sw.marketing.util.SkinReader;

@WebServlet("/detail/*")
public class BlogDetailServlet extends HttpServlet
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
		
		String paramTopicID = request.getParameter("TOPIC_ID");
		long topicID = 0;
		try
		{
			topicID = Long.parseLong(paramTopicID);
		}
		catch(NumberFormatException e)
		{
			response.getWriter().println("Topic ID unknown.  Please go back and try again.");
			return;
		}
		
		BlogDAO blogDAO = DAOFactory.getBlogDAO();
		BlogTopicDAO topicDAO = DAOFactory.getBlogTopicDAO();
		BlogTopicTagDAO topicTagDAO = DAOFactory.getBlogTopicTagDAO();
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
			Topic topic = topicDAO.getBlogTopic(topicID);
			if(topic != null)
			{
				java.util.List<Tag> tags = topicTagDAO.getTags(topicID);
				if(tags != null)
				{
					topic.getTag().addAll(tags);
				}
				blog.getTopic().add(topic);
			}
			
//			java.util.List<Category> categories = categoryDAO.getCategories(blog.getId());
//			if(categories !=  null)
//			{
//				blog.getCategory().addAll(categories);
//			}
			data.getBlog().add(blog);
		}
		
		Environment environment = new Environment();
		environment.setScreenName("DETAIL");		
		data.setEnvironment(environment);

		/*
		 * generate output
		 */
		TransformerHelper transformerHelper = new TransformerHelper();
		transformerHelper.setUrlResolverBaseUrl(getServletContext().getInitParameter("assetXslUrl"));
		
		String xmlStr = transformerHelper.getXmlStr("org.sw.marketing.data.blog", data);
		String xslScreen = getServletContext().getInitParameter("assetXslPath") + "detail.xsl";
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
		Skin skin = skinDAO.getSkin(skinID);
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
		response.getWriter().println(htmlStr);
//		response.getWriter().println(skinHtmlStr);
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
