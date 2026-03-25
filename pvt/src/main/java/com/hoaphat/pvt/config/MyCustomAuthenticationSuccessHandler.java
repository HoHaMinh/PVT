package com.hoaphat.pvt.config;

import com.hoaphat.pvt.model.account.Account;
import com.hoaphat.pvt.service.account.IAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Component
public class MyCustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    @Autowired
    private IAccountService accountService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accountName = userDetails.getUsername(); // login ID — giữ nguyên không đổi
        String displayName = accountName; // fallback nếu không tìm thấy

        List<Account> accountList = accountService.findAll();
        for (int i = 0; i < accountList.size(); i++) {
            if (accountName.equals(accountList.get(i).getAccountName())) {
                displayName = accountList.get(i).getName(); // chỉ ghi đè displayName
            }
        }

        request.getSession().setAttribute("username", displayName);    // tên hiển thị: "Hồ Hà Minh"
        request.getSession().setAttribute("accountName", accountName); // login ID: "ho.ha.minh" ✅
        response.sendRedirect("/home");
    }
}
