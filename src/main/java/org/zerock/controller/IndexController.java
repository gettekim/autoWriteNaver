package org.zerock.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
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
	@RequestMapping(value = "/index", method = RequestMethod.GET)
	public String home(HttpServletRequest request, Model model) {

		return "index";
	}

	@RequestMapping(value = "/croll")
	public String naverlogin(HttpServletRequest request, Model model) throws IOException {
		
		// ũ�� ����̹��� ��θ� ����
		System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "/chromedriver.exe");

		// ����̹� ����
		ChromeOptions options = new ChromeOptions();
		options.addArguments("headless");
		WebDriver driver = new ChromeDriver(options);
		WebElement parent = null;
		String cont = "";
		Document doc = Jsoup.connect("https://dart.fss.or.kr/dsac001/mainAll.do").get();
		Elements time = null;
		Elements name = null;
		Elements content = null;
		Elements type = null;
		String contents = "";
		int max = 0;
		String tmp = "";
		
		List<String> text = new ArrayList<String>();
		List<Integer> timeList = new ArrayList<Integer>();
		List<String> nameList = new ArrayList<String>();
		List<String> contentList = new ArrayList<String>();
		List<String> typeList = new ArrayList<String>();
		List<String> urlList = new ArrayList<String>();
		
		time = doc.getElementsByClass("cen_txt");
		name = doc.getElementsByClass("nobr");
		type = doc.select(".nobr1 img");
		doc.select("span").remove();
		content = doc.select("td a");
		nameList = name.eachText();
		text = time.eachText();
		contentList = content.eachText();
		typeList = type.eachAttr("title");
		urlList = content.eachAttr("href");
		
		//�ð�����
		for (int i = 0; i < text.size(); i++) {
			if (text.get(i).length() == 5) {
				timeList.add(Integer.parseInt(text.get(i).replace(":", "")));
			}

		}
		
		for(int i = 0;  i < contentList.size(); i++) {
			
			tmp = contentList.get(i);
			if(tmp.contains("�����Ǹ�")) {
				
				cont = "["+typeList.get(i)+"] | "+nameList.get(i)+" |"+ tmp;
				
				System.out.println("���" + cont);
				driver.get("https://dart.fss.or.kr"+urlList.get(i));
				parent = driver.findElement(By.id("ifrm"));
				
				doc = Jsoup.connect(parent.getAttribute("src")).get();
				contents = doc.getElementsByClass("xforms").html();
				System.out.println("�ּ�"+contents);
				
				driver.close();
				
			}
		}
		
		max = Integer.parseInt(text.get(0).replace(":", ""));
		
		System.out.println("�ð�=" + timeList);
		System.out.println("ȸ���=" + nameList);
		System.out.println("����=" + contentList);
		System.out.println("Ÿ��=" + typeList);
		System.out.println("Ÿ��=" + urlList);
		driver.quit();
		return "index";
	}

	@RequestMapping(value = "/cafe")
	public String navLogin(HttpServletRequest request, Model model) throws Exception {

		String access_token = request.getParameter("data");

		NaverPost naverPost = new NaverPost();
		if (naverPost.naverPost(access_token) == 1) {
			System.out.println("����");
		} else {
			System.out.println("����");
		}

		System.out.println("���İ�" + request.getParameter("data"));
		return "cafe";
	}

}
