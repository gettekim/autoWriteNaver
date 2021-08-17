package org.zerock.service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class NaverPost {

	public int naverPost(String token) {

		String header = "Bearer " + token; // Bearer ������ ���� �߰�
		try {
			String clubid = "30508469";// ī���� ���� ID��
			String menuid = "1"; // ī�� �Խ��� id (��ǰ�Խ����� �Է� �Ұ�)
			String apiURL = "https://openapi.naver.com/v1/cafe/" + clubid + "/menu/" + menuid + "/articles";
			URL url = new URL(apiURL);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Authorization", header);
			// post request
			// �ش� string�� UTF-8�� encode �� MS949�� �� encode�� ������ ��
			String subject = URLEncoder.encode(URLEncoder.encode("ī�� ���� �λ�", "UTF-8"), "MS949");
			;
			String content = URLEncoder.encode(URLEncoder.encode("ī�� ���� �λ� �帳�ϴ� by Cafe API", "UTF-8"), "MS949");
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
}
