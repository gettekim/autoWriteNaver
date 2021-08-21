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
		
		// 크롬 드라이버의 경로를 설정
		System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "/chromedriver.exe");

		// 드라이버 실행
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
		
		//시간저장
		for (int i = 0; i < text.size(); i++) {
			if (text.get(i).length() == 5) {
				timeList.add(Integer.parseInt(text.get(i).replace(":", "")));
			}

		}
		
		for(int i = 0;  i < contentList.size(); i++) {
			
			tmp = contentList.get(i);
			if(tmp.contains("단일판매")) {
				
				cont = "["+typeList.get(i)+"] | "+nameList.get(i)+" |"+ tmp;
				
				System.out.println("결과" + cont);
				driver.get("https://dart.fss.or.kr"+urlList.get(i));
				parent = driver.findElement(By.id("ifrm"));
				
				doc = Jsoup.connect(parent.getAttribute("src")).get();
				contents = doc.getElementsByClass("xforms").html();
				System.out.println("주소"+contents);
				
				driver.close();
				
			}
		}
		
		max = Integer.parseInt(text.get(0).replace(":", ""));
		
		System.out.println("시간=" + timeList);
		System.out.println("회사명=" + nameList);
		System.out.println("내용=" + contentList);
		System.out.println("타입=" + typeList);
		System.out.println("타입=" + urlList);
		driver.quit();
		return "index";
	}

	@RequestMapping(value = "/cafe")
	public String navLogin(HttpServletRequest request, Model model) throws Exception {

		String access_token = request.getParameter("data");

		NaverPost naverPost = new NaverPost();
		if (naverPost.naverPost(access_token) == 1) {
			System.out.println("성공");
		} else {
			System.out.println("실패");
		}

		System.out.println("뭐냐고" + request.getParameter("data"));
		return "cafe";
	}

}
