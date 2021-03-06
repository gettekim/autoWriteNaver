package org.zerock.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.github.scribejava.core.model.OAuth2AccessToken;

/** * Handles requests for the application home page. */
@Controller
public class LoginController {

	private final Logger LOGGER = LoggerFactory.getLogger(LoginController.class.getName());

	/* NaverLoginBO */
	private NaverLoginBO naverLoginBO;
	private String apiResult = null;
	public String refreshToken = "";
	public String curruntRes = "";
	public OAuth2AccessToken oauthToken;

	@Autowired
	private void setNaverLoginBO(NaverLoginBO naverLoginBO) {
		this.naverLoginBO = naverLoginBO;
	}

	// 로그인 첫 화면 요청 메소드
	@RequestMapping(value = "/", method = { RequestMethod.GET, RequestMethod.POST })
	public String login(Model model, HttpSession session) {
		/* 네이버아이디로 인증 URL을 생성하기 위하여 naverLoginBO클래스의 getAuthorizationUrl메소드 호출 */
		String naverAuthUrl = naverLoginBO.getAuthorizationUrl(session);
		// https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=sE***************&
		// redirect_uri=http%3A%2F%2F211.63.89.90%3A8090%2Flogin_project%2Fcallback&state=e68c269c-5ba9-4c31-85da-54c16c658125
		System.out.println("네이버:" + naverAuthUrl);
		// 네이버
		model.addAttribute("url", naverAuthUrl);
		return "login";
	}

	// 네이버 로그인 성공시 callback호출 메소드
	@RequestMapping(value = "/callback", method = { RequestMethod.GET, RequestMethod.POST })
	public String callback(Model model, @RequestParam String code, @RequestParam String state, HttpSession session)
			throws IOException, ParseException {
		System.out.println("여기는 callback");
		System.out.println("코드=" + code + ", state= " + state);

		oauthToken = naverLoginBO.getAccessToken(session, code, state);
		refreshToken = oauthToken.getRefreshToken();

		session.setAttribute("token", oauthToken);

		// 세션 생성
		model.addAttribute("code", code);
		model.addAttribute("state", state);
		model.addAttribute("token", oauthToken);

		return "login";
	}

	// 로그아웃
	@RequestMapping(value = "/logout", method = { RequestMethod.GET, RequestMethod.POST })
	public String logout(HttpSession session) throws IOException {
		System.out.println("여기는 logout");
		session.invalidate();
		return "redirect:/";
	}

	// 수동
	@RequestMapping(value = "/post")
	@ResponseBody
	public int post() throws IOException, ParseException, InterruptedException {
		int result = 1;

		System.out.println("현재토크상황 =" + oauthToken.toString());

		refreshToken = oauthToken.getRefreshToken();
		// 해외증시
		result = naverLoginBO.crollingPost(oauthToken, 1);
		// 국내증시
		result = naverLoginBO.crollingPost(oauthToken, 2);

		return result;
	}

	// 자동
	@Scheduled(cron = "0 30 8 * * MON-FRI")
	public void autoPost() throws IOException, ParseException, InterruptedException {
		int result = 0;

		System.out.println("현재토크상황 =" + oauthToken.toString());

		refreshToken = oauthToken.getRefreshToken();
		// 해외증시
		result = naverLoginBO.crollingPost(oauthToken, 1);
		// 국내증시
		result = naverLoginBO.crollingPost(oauthToken, 2);

		return;
	}

	// 주기적으로 토큰갱신
	//@Scheduled(cron = "0 0/60 * * * ?")
	@RequestMapping(value = "/token")
	@ResponseBody
	public int tokenRefresh() throws IOException, ParseException {
		int result = 1;
		Date date = new Date();
		LOGGER.info(date + "@@@@토큰갱신");
		curruntRes = naverLoginBO.getRefreshToken(refreshToken);

		JSONParser jsonParser = new JSONParser();
		Object obj = jsonParser.parse(curruntRes);
		JSONObject jsonObj = (JSONObject) obj;
		// 갱신한 토큰값을 넣어줌
		oauthToken = new OAuth2AccessToken((String) jsonObj.get("access_token"), (String) jsonObj.get("token_type"),
				3600, (String) jsonObj.get("refresh_token"), null, curruntRes);

		// 갱신한 토큰은 리프레쉬토큰 새로 설정
		refreshToken = oauthToken.getRefreshToken();
		LOGGER.info("@@@@엑세스토큰 : " + oauthToken.getAccessToken());
		return result;

	}

	// 다트 크롤링
	@RequestMapping(value = "/dart")
	@Scheduled(cron = "0 0/60 10-19 * * MON-FRI")
	public void dartPost() throws IOException, ParseException, InterruptedException {
		int result = 0;
		
		tokenRefresh();
		System.out.println("현재토크상황 =" + oauthToken.toString());

		refreshToken = oauthToken.getRefreshToken();

		result = naverLoginBO.dartCrolling(oauthToken);

		return;
	}

	// 다트 내용가져오기

	@RequestMapping(value = "/screenshot")
	public void screenshot() throws FileNotFoundException, IOException, InterruptedException {

		// 크롬 드라이버의 경로를 설정
		System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "/chromedriver.exe");

		// 드라이버 실행
		ChromeOptions options = new ChromeOptions();
		options.addArguments("headless");
		WebDriver driver = new ChromeDriver(options);
		driver.get("http://dart.fss.or.kr/dsaf001/main.do?rcpNo=20210819800172");
		WebElement parent = driver.findElement(By.id("ifrm"));

		System.out.println("내용"+parent.getAttribute("src"));


		// 드라이버 종료
		driver.quit();

	}

	/*
	 * //스크린샷 전체
	 * 
	 * @RequestMapping(value = "/hole") public void holepage() throws IOException {
	 * WebDriver webDriver = new ChromeDriver();
	 * System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir")
	 * + "/chromedriver.exe");
	 * webDriver.get("http://dart.fss.or.kr/dsaf001/main.do?rcpNo=20210817901614");
	 * JavascriptExecutor js = ((JavascriptExecutor) webDriver); // Scroll right to
	 * the top js.executeScript("window.scrollTo(0,0)"); // Get the height of the
	 * screen int windowHeight = ((Number)
	 * js.executeScript("return window.innerHeight")).intValue(); // Get the total
	 * height of the page int pageHeight = ((Number)
	 * js.executeScript("return document.body.scrollHeight")).intValue(); //
	 * Calculate the number of full screen shots double fullFraction = pageHeight /
	 * windowHeight; int fullShots = (int) fullFraction; // this simply removes the
	 * decimals // Initialise ouput image int imageWidth =
	 * webDriver.manage().window().getSize().width; BufferedImage fullScreenshot =
	 * new BufferedImage(imageWidth, pageHeight, BufferedImage.TYPE_4BYTE_ABGR); //
	 * Get the graphics Graphics2D fullGraphics = fullScreenshot.createGraphics();
	 * // Calculate our scroll script String script = "window.scrollBy(0," +
	 * String.valueOf(windowHeight) + ")"; // Loop - for the required number of full
	 * screenshots for (int aShot = 0; aShot < fullShots; aShot ++) { // Sort out
	 * the screenshot and paste it in the correct place
	 * pasteScreenshot(fullGraphics, aShot * windowHeight,webDriver); // scroll
	 * js.executeScript(script); } // Final phase - scroll to the bottom
	 * js.executeScript(script); // we know this goes too far down, but should be
	 * OK. // Take final screenshot and paste at the bottom
	 * pasteScreenshot(fullGraphics, pageHeight - windowHeight,webDriver); // Save
	 * the whole thing to output file. ImageIO.write(fullScreenshot, "PNG", new
	 * File(System.getProperty("user.dir")+"/screent.png")); } //부분스크린샷 private void
	 * pasteScreenshot (Graphics2D outputGraphics, int yCoordinate, WebDriver
	 * webDriver ) throws IOException { // Take screenshot and hold it as an image
	 * File tmpFile = ((TakesScreenshot)webDriver).getScreenshotAs(OutputType.FILE);
	 * BufferedImage tmpImage = ImageIO.read(tmpFile); // Draw it on the graphics of
	 * the final output image outputGraphics.drawImage(tmpImage, null, 0,
	 * yCoordinate); }
	 */
}
