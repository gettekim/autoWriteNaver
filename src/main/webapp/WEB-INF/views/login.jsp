<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<title>NAVER LOGIN</title>
</head>
<script src="http://code.jquery.com/jquery-1.11.2.min.js"></script>
<script src="http://code.jquery.com/jquery-migrate-1.2.1.min.js"></script>

<script type="text/javascript">
function refreshToken(){
	
	$.ajax({
		url: "/token",
		type: "POST",
		success: function(data){
			alert("갱신성공");
		},
		error: function (request, status, error){
		}
		});
}

function post(){
	
	$.ajax({
		url: "/post",
		type: "POST",
		success: function(data){
			alert("글쓰기 성공");
		},
		error: function (request, status, error){
		}
		});
}

</script>
<body>
	<h1>자동글쓰기</h1>
	<hr>
	<br>
	<center>
		<c:choose>
			<c:when test="${token != null}">
				<h2>네이버 아이디 로그인 성공하셨습니다!!</h2>
				<h3>
					<a href="logout">로그아웃</a>
				</h3>
				<h3>
					<button onclick="post()">글쓰기</button>
				</h3>
								<h3>
					<button onclick="refreshToken()">토큰갱신</button>
				</h3>
			</c:when>
			<c:otherwise>
				<form action="login.userdo" method="post" name="frm"
					style="width: 470px;">
					<h2>로그인</h2>
				</form>
				<br>
				<!-- 네이버 로그인 창으로 이동 -->
				<div id="naver_id_login" style="text-align: center">
					<a href="${url}"> <img width="223"
						src="https://developers.naver.com/doc/review_201802/CK_bEFnWMeEBjXpQ5o8N_20180202_7aot50.png" /></a>
				</div>
				<br>
			</c:otherwise>
		</c:choose>
	</center>
</body>
</html>
