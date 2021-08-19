package org.zerock.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.zerock.service.NaverPost;

/**
 * Handles requests for the application home page.
 */
@Controller
@Component
public class IndexController {

	private static final Logger logger = LoggerFactory.getLogger(IndexController.class);
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home(HttpServletRequest request, Model model) {
		
		
		return "index";
	}
	
	@RequestMapping(value = "/croll")
	public String naverlogin(HttpServletRequest request, Model model) throws IOException {
		String cont = "";
		Document doc = Jsoup.connect("https://finance.naver.com/news/news_read.nhn?article_id=0004593676&office_id=015").get();
		Elements head = doc.select(".articleCont");
		//Elements body = doc.select(".articleSummary");
		head.select(".link_news").remove();
		head.select(".end_photo_org").remove();
		List<String> titleList = new ArrayList<String>();
		List<String> LinkList = new ArrayList<String>();
		List<String> text = new ArrayList<String>();
		titleList = head.eachAttr("br");
		LinkList = head.eachAttr("href");
		//text = body.eachText();
		cont = head.html();
		
		
		System.out.println("�Ӹ� = "+head);
		//System.out.println("�� = "+body);
		System.out.println("������ = "+titleList);
		//System.out.println("��ũ��� = "+LinkList);
		System.out.println("�ؽ�Ʈ="+text);
		System.out.println("�׽�Ʈ= "+ cont);
		return "index";
	}
	
	@RequestMapping(value = "/cafe")
	public String navLogin(HttpServletRequest request, Model model) throws Exception {
		
		String access_token = request.getParameter("data");
		
		NaverPost naverPost = new NaverPost();
		if(naverPost.naverPost(access_token) == 1) {
		System.out.println("����");
		}
		else {
			System.out.println("����");
		}
		
		System.out.println("���İ�"+request.getParameter("data"));
		return "cafe";
	}	
	
	
	//@Scheduled(cron = "0 0/1 * * * *")
	public void autoUpdate(){
		System.out.println("�����췯 �����۵�");
	}
	
	

}
