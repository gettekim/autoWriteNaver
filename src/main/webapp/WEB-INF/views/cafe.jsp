<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.net.URL" %>
<%@ page import="java.net.HttpURLConnection" %>
<%@ page import="java.io.BufferedReader" %>
<%@ page import="java.io.InputStreamReader" %>
<%@ page import="java.io.DataOutputStream" %>
<%@ page import="javax.servlet.http.HttpSession" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%
String daa = (String)session.getAttribute("dd");
String header = "Bearer " + daa; // Bearer 다음에 공백 추가
try {
	String clubid = "30508469";// 카페의 고유 ID값
	String menuid = "1"; // 카페 게시판 id (상품게시판은 입력 불가)
	String apiURL = "https://openapi.naver.com/v1/cafe/" + clubid + "/menu/" + menuid + "/articles";
	URL url = new URL(apiURL);
	HttpURLConnection con = (HttpURLConnection) url.openConnection();
	con.setRequestMethod("POST");
	con.setRequestProperty("Authorization", header);
	// post request
	// 해당 string은 UTF-8로 encode 후 MS949로 재 encode를 수행한 값
	String subject = URLEncoder.encode(URLEncoder.encode("카페 가입 인사", "UTF-8"), "MS949");
	;
	String content = URLEncoder.encode(URLEncoder.encode("카페 가입 인사 드립니다 by Cafe API", "UTF-8"), "MS949");
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
	StringBuffer response1 = new StringBuffer();
	while ((inputLine = br.readLine()) != null) {
		response1.append(inputLine);
	}
	br.close();
	System.out.println(response.toString());
} catch (Exception e) {
	System.out.println(e);
}
%>
  <head>
    <title>네이버로그인</title>
  </head>
  <body>
  여기	${data} <h1>${daa}</h1>
  </body>
  
  