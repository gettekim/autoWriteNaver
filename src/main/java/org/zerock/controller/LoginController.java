package org.zerock.controller;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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

	// 로그인 첫 화면 요청 메소드
	@RequestMapping(value = "/login", method = { RequestMethod.GET, RequestMethod.POST })
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
		System.out.println("코드="+code+", state= "+ state);
	
		oauthToken = naverLoginBO.getAccessToken(session, code, state);
		refreshToken = oauthToken.getRefreshToken();
		// 1. 로그인 사용자 정보를 읽어온다.
		apiResult = naverLoginBO.getUserProfile(oauthToken);
		// String형식의 json데이터
		/** apiResult json 구조 {"resultcode":"00", "message":"success", "response":{"id":"33666449","nickname":"shinn****","age":"20-29","gender":"M","email":"sh@naver.com","name":"\uc2e0\ubc94\ud638"}}
		**/
		// 2. String형식인 apiResult를 json형태로 바꿈
		System.out.println("결과값: "+apiResult);
		JSONParser parser = new JSONParser();
		Object obj = parser.parse(apiResult);
		JSONObject jsonObj = (JSONObject) obj;
		// 3. 데이터 파싱
		// Top레벨 단계 _response 파싱
		JSONObject response_obj = (JSONObject) jsonObj.get("response");
		// response의 nickname값 파싱
		String nickname = (String) response_obj.get("id");
		// 4.파싱 닉네임 세션으로 저장
		session.setAttribute("sessionId", nickname);
		session.setAttribute("token", oauthToken);
		
		// 세션 생성
		model.addAttribute("result", apiResult);
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
	
	
	//해외증시
	public void broadPost()
			throws IOException, ParseException, InterruptedException {
		int result = 0;
		
		System.out.println("현재토크상황 ="+oauthToken.toString());
	
		refreshToken = oauthToken.getRefreshToken();
		
		result = naverLoginBO.crollingPost(oauthToken,1);
	
		
		return ;
	}
	
	// 국내증시
	@RequestMapping(value = "/post")
	public void koreaPost()
			throws IOException, ParseException, InterruptedException {
		int result = 0;
		
		System.out.println("현재토크상황 ="+oauthToken.toString());
	
		refreshToken = oauthToken.getRefreshToken();
		 
		result = naverLoginBO.crollingPost(oauthToken,2);
	
		return;
	}
	
	//주기적으로 토큰갱신
	@Scheduled(cron = "0 0/55 * * * ?")
	public void tokenRefresh() throws IOException, ParseException {
		
	    Date date = new Date(); 
		LOGGER.info(date + "@@@@토큰갱신");
		curruntRes = naverLoginBO.getRefreshToken(refreshToken);
		
		JSONParser jsonParser = new JSONParser();
		 Object obj = jsonParser.parse(curruntRes);
		 JSONObject jsonObj = (JSONObject) obj;
		//갱신한 토큰값을 넣어줌
	    oauthToken = new OAuth2AccessToken((String)jsonObj.get("access_token"), (String)jsonObj.get("token_type"), 3600, (String)jsonObj.get("refresh_token"), null, curruntRes);
		
	    //갱신한 토큰은 리프레쉬토큰 새로 설정
	    refreshToken = oauthToken.getRefreshToken();
		LOGGER.info("@@@@엑세스토큰 : "+oauthToken.getAccessToken());
		return;
		
	}
}
