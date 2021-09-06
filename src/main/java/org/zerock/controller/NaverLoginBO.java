package org.zerock.controller;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;

import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

public class NaverLoginBO {

	private final Logger LOGGER = LoggerFactory.getLogger(NaverLoginBO.class.getName());
	
	int max = 0;
	
	// client_id: 애플리케이션 등록 후 발급받은 클라이언트 아이디
	// response_type: 인증 과정에 대한 구분값. code로 값이 고정돼 있습니다.
	// redirect_uri: 네이버 로그인 인증의 결과를 전달받을 콜백 URL(URL 인코딩). 애플리케이션을 등록할 때 Callback
	// URL에 설정한 정보입니다.
	// state: 애플리케이션이 생성한 상태 토큰

	/* 인증 요청문을 구성하는 파라미터 */
	private final static String CLIENT_ID = "ZrbXzVhUS3TdCamIaJiP";
	private final static String CLIENT_SECRET = "2scKIuOCzy";
	private final static String REDIRECT_URI = "http://localhost:8080/callback";
	private final static String SESSION_STATE = "oauth_state";
	/* 프로필 조회 API URL */
	private final static String PROFILE_API_URL = "https://openapi.naver.com/v1/nid/me";
	
	/* 네이버 아이디로 인증 URL 생성 Method */
	public String getAuthorizationUrl(HttpSession session) {
		/* 세션 유효성 검증을 위하여 난수를 생성 */
		String state = generateRandomString();
		/* 생성한 난수 값을 session에 저장 */
		setSession(session, state);
		/* Scribe에서 제공하는 인증 URL 생성 기능을 이용하여 네아로 인증 URL 생성 */
		OAuth20Service oauthService = new ServiceBuilder().apiKey(CLIENT_ID).apiSecret(CLIENT_SECRET)
				.callback(REDIRECT_URI).state(state)
				// 앞서 생성한 난수값을 인증 URL생성시 사용함
				.build(NaverLoginApi.instance());
		return oauthService.getAuthorizationUrl();
	}

	/* 네이버아이디로 Callback 처리 및 AccessToken 획득 Method */
	public OAuth2AccessToken getAccessToken(HttpSession session, String code, String state) throws IOException {
		/* Callback으로 전달받은 세선검증용 난수값과 세션에 저장되어있는 값이 일치하는지 확인 */
		String sessionState = getSession(session);
		System.out.println("세션: " + sessionState);
		if (StringUtils.pathEquals(sessionState, state)) {
			OAuth20Service oauthService = new ServiceBuilder().apiKey(CLIENT_ID).apiSecret(CLIENT_SECRET)
					.callback(REDIRECT_URI).state(state).build(NaverLoginApi.instance());
			/* Scribe에서 제공하는 AccessToken 획득 기능으로 네아로 Access Token을 획득 */
			OAuth2AccessToken accessToken = oauthService.getAccessToken(code);
			System.out.println("엑세스토큰 : " + accessToken);
			return accessToken;
		}
		return null;
	}

	/* 네이버아이디로 Callback 처리 및 RefreshToken 획득 Method */
	public String getRefreshToken(String refreshToken) throws IOException, ParseException {
		String apiURL;
		apiURL = "https://nid.naver.com/oauth2.0/token?grant_type=refresh_token&";
		apiURL += "client_id=" + CLIENT_ID;
		apiURL += "&client_secret=" + CLIENT_SECRET;
		apiURL += "&refresh_token=" + refreshToken;
		System.out.println("apiURL=" + apiURL);
		String res = requestToServer(apiURL, "");

		return res;
	}

	/* 세션 유효성 검증을 위한 난수 생성기 */
	private String generateRandomString() {
		return UUID.randomUUID().toString();
	}

	/* http session에 데이터 저장 */
	private void setSession(HttpSession session, String state) {
		session.setAttribute(SESSION_STATE, state);
	}

	/* http session에서 데이터 가져오기 */
	private String getSession(HttpSession session) {
		return (String) session.getAttribute(SESSION_STATE);
	}

	/* Access Token을 이용하여 네이버 사용자 프로필 API를 호출 */
	public String getUserProfile(OAuth2AccessToken oauthToken) throws IOException {
		OAuth20Service oauthService = new ServiceBuilder().apiKey(CLIENT_ID).apiSecret(CLIENT_SECRET)
				.callback(REDIRECT_URI).build(NaverLoginApi.instance());
		OAuthRequest request = new OAuthRequest(Verb.GET, PROFILE_API_URL, oauthService);
		oauthService.signRequest(oauthToken, request);
		Response response = request.send();
		return response.getBody();
	}

	// 카페글쓰기
	@SuppressWarnings("unchecked")
	public int crollingPost(OAuth2AccessToken oauthToken, int flag) throws IOException, InterruptedException {

		LOGGER.info("크롤링포스트 시작");
		Document doc = null;
		Elements body = null;
		// 초기화
		List resultList = null; // 크롤링한 리스트
		List<String> titleList = null; // 제목 리스트
		List<String> LinkList = null; // 링크 리스트
		List<String> text = null; // 본문내용 리스트

		String title = "";
		String contents = "";
		if (flag == 1 || flag == 2) {
			// 해외 증시 뉴스 가져오기
			if (flag == 1) {
				resultList = newCrolling(1);
			}
			// 국내증시
			else if (flag == 2) {
				resultList = newCrolling(2);
			}
			titleList = (List<String>) resultList.get(0);
			LinkList = (List<String>) resultList.get(1);
			text = (List<String>) resultList.get(2);

			for (int i = 0; i < 10; i++) {

				Date date = new Date();
				LOGGER.info(date + "글등록");

				doc = Jsoup.connect("https://finance.naver.com" + LinkList.get(i)).get();
				body = doc.select(".articleCont");
				body.select(".link_news").remove();
				body.select(".end_photo_org").remove();

				title = titleList.get(i);
				contents = body.html();
				// 따옴표 변환작업
				contents = contents.replaceAll("\\\"", "\\\\\"");
				contents += "<br> [출처] https://finance.naver.com" + LinkList.get(i);
				if (flag == 2) {
					post(oauthToken, title, contents, 2);
				} else if (flag == 1) {
					post(oauthToken, title, contents, 1);
				}
				System.out.println("개시시간 : " + date);
				TimeUnit.SECONDS.sleep(65);

			}
		}
		// 다트 일때
		else {

			String cont = "";
			doc = Jsoup.connect(
					"http://dart.fss.or.kr/report/viewer.do?rcpNo=20210819800172&dcmNo=8185915&eleId=0&offset=0&length=0&dtd=HTML")
					.get();
			doc.getElementsByTag("a").removeAttr("onclick");
			body = doc.getElementsByClass("xforms");
			cont = body.html();
			cont = cont.replaceAll("\\\"", "\\\\\"");
			title = "테스트";

			post(oauthToken, title, cont, 3);
		}
		LOGGER.info("크롤링포스트 종료");
		return 1;
	}

	// 네이버 뉴스 크롤링
	public List newCrolling(int flag) throws IOException {
		Document doc = null;
		Elements head = null;
		Elements body = null;
		// 해외증시
		if (flag == 1) {
			doc = Jsoup.connect(
					"https://finance.naver.com/news/news_list.nhn?mode=LSS3D&section_id=101&section_id2=258&section_id3=403")
					.get();
		}
		// 국내증시
		else if (flag == 2) {
			doc = Jsoup.connect(
					"https://finance.naver.com/news/news_list.nhn?mode=LSS3D&section_id=101&section_id2=258&section_id3=401")
					.get();
		}

		if (flag == 1 || flag == 2) {

			head = doc.select(".articleSubject a");
			body = doc.select(".articleSummary");

		}
		List result = new ArrayList<>();
		List<String> titleList = new ArrayList<String>();
		List<String> linkList = new ArrayList<String>();
		List<String> text = new ArrayList<String>();
		titleList = head.eachAttr("title");
		linkList = head.eachAttr("href");
		text = body.eachText();

		result.add(titleList);
		result.add(linkList);
		result.add(text);

		return result;

	}

	/* Access Token을 이용하여 네이버 카페글쓰기 API를 호출 */
	public int post(OAuth2AccessToken oauthToken, String title, String contents, int flag) throws IOException {

		Date date = new Date();
		LOGGER.info(date + ": 글쓰기 작성 시작");

		String subject = "";
		String content = "";
		String menuid = "";
		try {

			String token = oauthToken.getAccessToken();// 네이버 로그인 접근 토큰;
			String header = "Bearer " + token; // Bearer 다음에 공백 추가
			String clubid = "30513537";// 카페의 고유 ID값
			if (flag == 1) {
				menuid = "1"; // 카페 게시판 id (상품게시판은 입력 불가)
			}
			if (flag == 2) {
				menuid = "2"; // 카페 게시판 id (상품게시판은 입력 불가)
			}
			if (flag == 3) {
				menuid = "2";
			}

			String apiURL = "https://openapi.naver.com/v1/cafe/" + clubid + "/menu/" + menuid + "/articles";

			URL url = new URL(apiURL);

			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Authorization", header);

			// post request
			// 해당 string은 UTF-8로 encode 후 MS949로 재 encode를 수행한 값
			subject = URLEncoder.encode(URLEncoder.encode(title, "UTF-8"), "MS949");
			;
			content = URLEncoder.encode(URLEncoder.encode(contents, "UTF-8"), "MS949");
			String postParams = "subject=" + subject + "&content=" + content;
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(postParams);
			wr.flush();
			wr.close();
			int responseCode = con.getResponseCode();
			BufferedReader br;
			if (responseCode == 200) { // 정상 호출
				br = new BufferedReader(new InputStreamReader(con.getInputStream()));
			} else { // 에러 발생
				br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
			}
			String inputLine;
			StringBuffer response = new StringBuffer();
			while ((inputLine = br.readLine()) != null) {
				response.append(inputLine);
			}
			br.close();
			System.out.println(response.toString());
		} catch (Exception e) {
			System.out.println(e);
		}
		return 1;

	}

	// 다트 크롤링
	public int dartCrolling(OAuth2AccessToken oauthToken) throws IOException, InterruptedException {

		// 크롬 드라이버의 경로를 설정
		System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "/chromedriver.exe");

		// 드라이버 실행
		ChromeOptions options = new ChromeOptions();
		options.addArguments("headless");
		
		WebElement parent = null;
		String title = "";
		Document doc = Jsoup.connect("https://dart.fss.or.kr/dsac001/mainAll.do").get();
		//Document doc = Jsoup.connect("https://dart.fss.or.kr/dsac001/mainAll.do?selectDate=&sort=&series=&mdayCnt=3").get();
		Elements time = null;
		Elements name = null;
		Elements content = null;
		Elements type = null;
		String contents = "";
		String tmp = "";

		List<String> text = new ArrayList<String>();
		List<Integer> timeList = new ArrayList<Integer>();
		List<String> nameList = new ArrayList<String>();
		List<String> contentList = new ArrayList<String>();
		List<String> typeList = new ArrayList<String>();
		List<String> urlList = new ArrayList<String>();

		time = doc.select("tr > td:first-of-type");
		type = doc.select(".innerWrap span");
		name = doc.select(".innerWrap a");
		content = doc.select("tr > td:nth-child(3) > a");
		nameList = name.eachText();
		text = time.eachText();
		contentList = content.eachText();
		typeList = type.eachAttr("title");
		urlList = content.eachAttr("href");

		// 시간저장
		for (int i = 0; i < text.size(); i++) {
			if (text.get(i).length() == 5) {
				timeList.add(Integer.parseInt(text.get(i).replace(":", "")));
			}

		}

		for (int i = 0; i < contentList.size(); i++) {

			if (timeList.get(i) > max) {
				tmp = contentList.get(i);
				if (tmp.contains("단일판매")) {

					title = "[" + typeList.get(i) + "] | " + nameList.get(i) + " | " + tmp;

					System.out.println("결과" + title);
					WebDriver driver = new ChromeDriver(options);
					driver.get("https://dart.fss.or.kr" + urlList.get(i));
					parent = driver.findElement(By.id("ifrm"));

					doc = Jsoup.connect(parent.getAttribute("src")).get();
					contents = doc.getElementsByClass("xforms").html();
					contents = contents.replaceAll("\\\"", "\\\\\"");
					post(oauthToken, title, contents, 3);
					driver.quit();
					TimeUnit.SECONDS.sleep(65);
					
				}
			}
		}

		max = Integer.parseInt(text.get(0).replace(":", ""));
	
		LOGGER.info("다트글쓰기 완료");
		return 1;
	}

	/**
	 * 서버 통신 메소드
	 * 
	 * @param apiURL
	 * @param headerStr
	 * @return
	 * @throws IOException
	 */
	private String requestToServer(String apiURL, String headerStr) throws IOException {
		URL url = new URL(apiURL);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		System.out.println("header Str: " + headerStr);
		if (headerStr != null && !headerStr.equals("")) {
			con.setRequestProperty("Authorization", headerStr);
		}
		int responseCode = con.getResponseCode();
		BufferedReader br;
		System.out.println("responseCode=" + responseCode);
		if (responseCode == 200) { // 정상 호출
			br = new BufferedReader(new InputStreamReader(con.getInputStream()));
		} else { // 에러 발생
			br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
		}
		String inputLine;
		StringBuffer res = new StringBuffer();
		while ((inputLine = br.readLine()) != null) {
			res.append(inputLine);
		}
		br.close();
		if (responseCode == 200) {
			return res.toString();
		} else {
			return null;
		}
	}

}
