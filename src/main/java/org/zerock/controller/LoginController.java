package org.zerock.controller;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Time;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
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

	// �α��� ù ȭ�� ��û �޼ҵ�
	@RequestMapping(value = "/login", method = { RequestMethod.GET, RequestMethod.POST })
	public String login(Model model, HttpSession session) {
		/* ���̹����̵�� ���� URL�� �����ϱ� ���Ͽ� naverLoginBOŬ������ getAuthorizationUrl�޼ҵ� ȣ�� */
		String naverAuthUrl = naverLoginBO.getAuthorizationUrl(session);
		// https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=sE***************&
		// redirect_uri=http%3A%2F%2F211.63.89.90%3A8090%2Flogin_project%2Fcallback&state=e68c269c-5ba9-4c31-85da-54c16c658125
		System.out.println("���̹�:" + naverAuthUrl);
		// ���̹�
		model.addAttribute("url", naverAuthUrl);
		return "login";
	}

	// ���̹� �α��� ������ callbackȣ�� �޼ҵ�
	@RequestMapping(value = "/callback", method = { RequestMethod.GET, RequestMethod.POST })
	public String callback(Model model, @RequestParam String code, @RequestParam String state, HttpSession session)
			throws IOException, ParseException {
		System.out.println("����� callback");
		System.out.println("�ڵ�="+code+", state= "+ state);
	
		oauthToken = naverLoginBO.getAccessToken(session, code, state);
		refreshToken = oauthToken.getRefreshToken();
		// 1. �α��� ����� ������ �о�´�.
		apiResult = naverLoginBO.getUserProfile(oauthToken);
		// String������ json������
		/** apiResult json ���� {"resultcode":"00", "message":"success", "response":{"id":"33666449","nickname":"shinn****","age":"20-29","gender":"M","email":"sh@naver.com","name":"\uc2e0\ubc94\ud638"}}
		**/
		// 2. String������ apiResult�� json���·� �ٲ�
		System.out.println("�����: "+apiResult);
		JSONParser parser = new JSONParser();
		Object obj = parser.parse(apiResult);
		JSONObject jsonObj = (JSONObject) obj;
		// 3. ������ �Ľ�
		// Top���� �ܰ� _response �Ľ�
		JSONObject response_obj = (JSONObject) jsonObj.get("response");
		// response�� nickname�� �Ľ�
		String nickname = (String) response_obj.get("id");
		// 4.�Ľ� �г��� �������� ����
		session.setAttribute("sessionId", nickname);
		session.setAttribute("token", oauthToken);
		
		// ���� ����
		model.addAttribute("result", apiResult);
		model.addAttribute("code", code);
		model.addAttribute("state", state);
		model.addAttribute("token", oauthToken);
		
		return "login";
	}

	// �α׾ƿ�
	@RequestMapping(value = "/logout", method = { RequestMethod.GET, RequestMethod.POST })
	public String logout(HttpSession session) throws IOException {
		System.out.println("����� logout");
		session.invalidate();
		return "redirect:/";
	}
	
	
	//�ؿ�����
	public void broadPost()
			throws IOException, ParseException, InterruptedException {
		int result = 0;
		
		System.out.println("������ũ��Ȳ ="+oauthToken.toString());
	
		refreshToken = oauthToken.getRefreshToken();
		
		result = naverLoginBO.crollingPost(oauthToken,1);
	
		
		return ;
	}
	
	// ��������
	@RequestMapping(value = "/post")
	public void koreaPost()
			throws IOException, ParseException, InterruptedException {
		int result = 0;
		
		System.out.println("������ũ��Ȳ ="+oauthToken.toString());
	
		refreshToken = oauthToken.getRefreshToken();
		 
		result = naverLoginBO.crollingPost(oauthToken,2);
		result = naverLoginBO.crollingPost(oauthToken,1);
	
		return;
	}
	
	//�ֱ������� ��ū����
	@Scheduled(cron = "0 0/55 * * * ?")
	public void tokenRefresh() throws IOException, ParseException {
		
	    Date date = new Date(); 
		LOGGER.info(date + "@@@@��ū����");
		curruntRes = naverLoginBO.getRefreshToken(refreshToken);
		
		JSONParser jsonParser = new JSONParser();
		 Object obj = jsonParser.parse(curruntRes);
		 JSONObject jsonObj = (JSONObject) obj;
		//������ ��ū���� �־���
	    oauthToken = new OAuth2AccessToken((String)jsonObj.get("access_token"), (String)jsonObj.get("token_type"), 3600, (String)jsonObj.get("refresh_token"), null, curruntRes);
		
	    //������ ��ū�� ����������ū ���� ����
	    refreshToken = oauthToken.getRefreshToken();
		LOGGER.info("@@@@��������ū : "+oauthToken.getAccessToken());
		return;
		
	}
	
	//��Ʈ ũ�Ѹ�
	@RequestMapping(value = "/dart")
	public void dartPost()
			throws IOException, ParseException, InterruptedException {
		int result = 0;
		
		System.out.println("������ũ��Ȳ ="+oauthToken.toString());
	
		refreshToken = oauthToken.getRefreshToken();
		 
		result = naverLoginBO.crollingPost(oauthToken,3);
	
		return;
	}
	
	
	
	//��Ʈ ���밡������
	
	@RequestMapping(value = "/screenshot")
	public void screenshot() throws FileNotFoundException, IOException, InterruptedException {
		
		
		// ũ�� ����̹��� ��θ� ����
				System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "/chromedriver.exe");
		    
				// ����̹� ����
				WebDriver driver = new ChromeDriver();
				driver.navigate().to("http://dart.fss.or.kr/dsaf001/main.do?rcpNo=20210819800172");
			//	driver.get("http://dart.fss.or.kr/dsaf001/main.do?rcpNo=20210819800172");
			
				WebDriverWait wait = new WebDriverWait(driver, 10);
				//WebElement parent = driver.findElement(By.id("LIB_LC000"));
				TimeUnit.SECONDS.sleep(5);
				
		        System.out.println(driver.getPageSource());
		        
			
//				TakesScreenshot screenshot = (TakesScreenshot)driver;
//				byte[] imageByte = screenshot.getScreenshotAs(OutputType.BYTES);
//				try (FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir") + "/screenshot.png")) {
//				   fos.write(imageByte);
//				   fos.close();
//				}
				
				// ����̹� ����
				driver.quit();
		
	}

	/*
	//��ũ���� ��ü
	@RequestMapping(value = "/hole")
	public void holepage() throws IOException {
		WebDriver webDriver = new ChromeDriver();
		System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "/chromedriver.exe");
		webDriver.get("http://dart.fss.or.kr/dsaf001/main.do?rcpNo=20210817901614");
		   JavascriptExecutor js = ((JavascriptExecutor) webDriver);
	        // Scroll right to the top
	        js.executeScript("window.scrollTo(0,0)");
	        // Get the height of the screen
	        int windowHeight = ((Number) js.executeScript("return window.innerHeight")).intValue();
	        // Get the total height of the page
	        int pageHeight = ((Number) js.executeScript("return document.body.scrollHeight")).intValue();
	        // Calculate the number of full screen shots
	        double fullFraction = pageHeight / windowHeight;
	        int fullShots = (int) fullFraction; // this simply removes the decimals
	        // Initialise ouput image
	        int imageWidth = webDriver.manage().window().getSize().width;
	        BufferedImage fullScreenshot = new BufferedImage(imageWidth, pageHeight, BufferedImage.TYPE_4BYTE_ABGR);
	        // Get the graphics
	        Graphics2D fullGraphics = fullScreenshot.createGraphics();
	        // Calculate our scroll script
	        String script = "window.scrollBy(0," + String.valueOf(windowHeight) + ")";
	        // Loop - for the required number of full screenshots
	        for (int aShot = 0; aShot < fullShots; aShot ++) {
	            // Sort out the screenshot and paste it in the correct place
	            pasteScreenshot(fullGraphics, aShot * windowHeight,webDriver);
	            // scroll
	            js.executeScript(script);
	        }
	        // Final phase - scroll to the bottom
	        js.executeScript(script); // we know this goes too far down, but should be OK.
	        // Take final screenshot and paste at the bottom
	        pasteScreenshot(fullGraphics, pageHeight - windowHeight,webDriver);
	        // Save the whole thing to output file.
	        ImageIO.write(fullScreenshot, "PNG", new File(System.getProperty("user.dir")+"/screent.png"));
	}
	//�κн�ũ����
	private void pasteScreenshot (Graphics2D outputGraphics, int yCoordinate, WebDriver webDriver ) throws IOException {
        // Take screenshot and hold it as an image
        File tmpFile = ((TakesScreenshot)webDriver).getScreenshotAs(OutputType.FILE);
        BufferedImage tmpImage = ImageIO.read(tmpFile);
        // Draw it on the graphics of the final output image
        outputGraphics.drawImage(tmpImage, null, 0, yCoordinate);
    }
    */
}
