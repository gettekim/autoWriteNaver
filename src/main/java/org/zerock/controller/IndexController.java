package org.zerock.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
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
		
		Document doc = Jsoup.connect("https://finance.naver.com/news/news_list.nhn?mode=LSS3D&section_id=101&section_id2=258&section_id3=403").get();
		Elements head = doc.select(".articleSubject a");
		Elements body = doc.select(".articleSummary");
		
		
		List<String> titleList = new ArrayList<String>();
		List<String> LinkList = new ArrayList<String>();
		List<String> text = new ArrayList<String>();
		titleList = head.eachAttr("title");
		LinkList = head.eachAttr("href");
		text = body.eachText();
		
		
		
		System.out.println("�Ӹ� = "+head);
		System.out.println("�� = "+body);
		//System.out.println("������ = "+titleList);
		//System.out.println("��ũ��� = "+LinkList);
		System.out.println("�ؽ�Ʈ="+text);
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
