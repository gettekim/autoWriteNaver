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
	
	// client_id: ���ø����̼� ��� �� �߱޹��� Ŭ���̾�Ʈ ���̵�
	// response_type: ���� ������ ���� ���а�. code�� ���� ������ �ֽ��ϴ�.
	// redirect_uri: ���̹� �α��� ������ ����� ���޹��� �ݹ� URL(URL ���ڵ�). ���ø����̼��� ����� �� Callback
	// URL�� ������ �����Դϴ�.
	// state: ���ø����̼��� ������ ���� ��ū

	/* ���� ��û���� �����ϴ� �Ķ���� */
	private final static String CLIENT_ID = "ZrbXzVhUS3TdCamIaJiP";
	private final static String CLIENT_SECRET = "2scKIuOCzy";
	private final static String REDIRECT_URI = "http://localhost:8080/callback";
	private final static String SESSION_STATE = "oauth_state";
	/* ������ ��ȸ API URL */
	private final static String PROFILE_API_URL = "https://openapi.naver.com/v1/nid/me";
	
	/* ���̹� ���̵�� ���� URL ���� Method */
	public String getAuthorizationUrl(HttpSession session) {
		/* ���� ��ȿ�� ������ ���Ͽ� ������ ���� */
		String state = generateRandomString();
		/* ������ ���� ���� session�� ���� */
		setSession(session, state);
		/* Scribe���� �����ϴ� ���� URL ���� ����� �̿��Ͽ� �׾Ʒ� ���� URL ���� */
		OAuth20Service oauthService = new ServiceBuilder().apiKey(CLIENT_ID).apiSecret(CLIENT_SECRET)
				.callback(REDIRECT_URI).state(state)
				// �ռ� ������ �������� ���� URL������ �����
				.build(NaverLoginApi.instance());
		return oauthService.getAuthorizationUrl();
	}

	/* ���̹����̵�� Callback ó�� �� AccessToken ȹ�� Method */
	public OAuth2AccessToken getAccessToken(HttpSession session, String code, String state) throws IOException {
		/* Callback���� ���޹��� ���������� �������� ���ǿ� ����Ǿ��ִ� ���� ��ġ�ϴ��� Ȯ�� */
		String sessionState = getSession(session);
		System.out.println("����: " + sessionState);
		if (StringUtils.pathEquals(sessionState, state)) {
			OAuth20Service oauthService = new ServiceBuilder().apiKey(CLIENT_ID).apiSecret(CLIENT_SECRET)
					.callback(REDIRECT_URI).state(state).build(NaverLoginApi.instance());
			/* Scribe���� �����ϴ� AccessToken ȹ�� ������� �׾Ʒ� Access Token�� ȹ�� */
			OAuth2AccessToken accessToken = oauthService.getAccessToken(code);
			System.out.println("��������ū : " + accessToken);
			return accessToken;
		}
		return null;
	}

	/* ���̹����̵�� Callback ó�� �� RefreshToken ȹ�� Method */
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

	/* ���� ��ȿ�� ������ ���� ���� ������ */
	private String generateRandomString() {
		return UUID.randomUUID().toString();
	}

	/* http session�� ������ ���� */
	private void setSession(HttpSession session, String state) {
		session.setAttribute(SESSION_STATE, state);
	}

	/* http session���� ������ �������� */
	private String getSession(HttpSession session) {
		return (String) session.getAttribute(SESSION_STATE);
	}

	/* Access Token�� �̿��Ͽ� ���̹� ����� ������ API�� ȣ�� */
	public String getUserProfile(OAuth2AccessToken oauthToken) throws IOException {
		OAuth20Service oauthService = new ServiceBuilder().apiKey(CLIENT_ID).apiSecret(CLIENT_SECRET)
				.callback(REDIRECT_URI).build(NaverLoginApi.instance());
		OAuthRequest request = new OAuthRequest(Verb.GET, PROFILE_API_URL, oauthService);
		oauthService.signRequest(oauthToken, request);
		Response response = request.send();
		return response.getBody();
	}

	// ī��۾���
	@SuppressWarnings("unchecked")
	public int crollingPost(OAuth2AccessToken oauthToken, int flag) throws IOException, InterruptedException {

		LOGGER.info("ũ�Ѹ�����Ʈ ����");
		Document doc = null;
		Elements body = null;
		// �ʱ�ȭ
		List resultList = null; // ũ�Ѹ��� ����Ʈ
		List<String> titleList = null; // ���� ����Ʈ
		List<String> LinkList = null; // ��ũ ����Ʈ
		List<String> text = null; // �������� ����Ʈ

		String title = "";
		String contents = "";
		if (flag == 1 || flag == 2) {
			// �ؿ� ���� ���� ��������
			if (flag == 1) {
				resultList = newCrolling(1);
			}
			// ��������
			else if (flag == 2) {
				resultList = newCrolling(2);
			}
			titleList = (List<String>) resultList.get(0);
			LinkList = (List<String>) resultList.get(1);
			text = (List<String>) resultList.get(2);

			for (int i = 0; i < 10; i++) {

				Date date = new Date();
				LOGGER.info(date + "�۵��");

				doc = Jsoup.connect("https://finance.naver.com" + LinkList.get(i)).get();
				body = doc.select(".articleCont");
				body.select(".link_news").remove();
				body.select(".end_photo_org").remove();

				title = titleList.get(i);
				contents = body.html();
				// ����ǥ ��ȯ�۾�
				contents = contents.replaceAll("\\\"", "\\\\\"");
				contents += "<br> [��ó] https://finance.naver.com" + LinkList.get(i);
				if (flag == 2) {
					post(oauthToken, title, contents, 2);
				} else if (flag == 1) {
					post(oauthToken, title, contents, 1);
				}
				System.out.println("���ýð� : " + date);
				TimeUnit.SECONDS.sleep(65);

			}
		}
		// ��Ʈ �϶�
		else {

			String cont = "";
			doc = Jsoup.connect(
					"http://dart.fss.or.kr/report/viewer.do?rcpNo=20210819800172&dcmNo=8185915&eleId=0&offset=0&length=0&dtd=HTML")
					.get();
			doc.getElementsByTag("a").removeAttr("onclick");
			body = doc.getElementsByClass("xforms");
			cont = body.html();
			cont = cont.replaceAll("\\\"", "\\\\\"");
			title = "�׽�Ʈ";

			post(oauthToken, title, cont, 3);
		}
		LOGGER.info("ũ�Ѹ�����Ʈ ����");
		return 1;
	}

	// ���̹� ���� ũ�Ѹ�
	public List newCrolling(int flag) throws IOException {
		Document doc = null;
		Elements head = null;
		Elements body = null;
		// �ؿ�����
		if (flag == 1) {
			doc = Jsoup.connect(
					"https://finance.naver.com/news/news_list.nhn?mode=LSS3D&section_id=101&section_id2=258&section_id3=403")
					.get();
		}
		// ��������
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

	/* Access Token�� �̿��Ͽ� ���̹� ī��۾��� API�� ȣ�� */
	public int post(OAuth2AccessToken oauthToken, String title, String contents, int flag) throws IOException {

		Date date = new Date();
		LOGGER.info(date + ": �۾��� �ۼ� ����");

		String subject = "";
		String content = "";
		String menuid = "";
		try {

			String token = oauthToken.getAccessToken();// ���̹� �α��� ���� ��ū;
			String header = "Bearer " + token; // Bearer ������ ���� �߰�
			String clubid = "30513537";// ī���� ���� ID��
			if (flag == 1) {
				menuid = "1"; // ī�� �Խ��� id (��ǰ�Խ����� �Է� �Ұ�)
			}
			if (flag == 2) {
				menuid = "2"; // ī�� �Խ��� id (��ǰ�Խ����� �Է� �Ұ�)
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
			// �ش� string�� UTF-8�� encode �� MS949�� �� encode�� ������ ��
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
			if (responseCode == 200) { // ���� ȣ��
				br = new BufferedReader(new InputStreamReader(con.getInputStream()));
			} else { // ���� �߻�
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

	// ��Ʈ ũ�Ѹ�
	public int dartCrolling(OAuth2AccessToken oauthToken) throws IOException, InterruptedException {

		// ũ�� ����̹��� ��θ� ����
		System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "/chromedriver.exe");

		// ����̹� ����
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

		// �ð�����
		for (int i = 0; i < text.size(); i++) {
			if (text.get(i).length() == 5) {
				timeList.add(Integer.parseInt(text.get(i).replace(":", "")));
			}

		}

		for (int i = 0; i < contentList.size(); i++) {

			if (timeList.get(i) > max) {
				tmp = contentList.get(i);
				if (tmp.contains("�����Ǹ�")) {

					title = "[" + typeList.get(i) + "] | " + nameList.get(i) + " | " + tmp;

					System.out.println("���" + title);
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
	
		LOGGER.info("��Ʈ�۾��� �Ϸ�");
		return 1;
	}

	/**
	 * ���� ��� �޼ҵ�
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
		if (responseCode == 200) { // ���� ȣ��
			br = new BufferedReader(new InputStreamReader(con.getInputStream()));
		} else { // ���� �߻�
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
