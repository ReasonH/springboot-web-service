package com.reason.application.springboot.config.auth;

import com.reason.application.springboot.config.auth.dto.OAuthAttributes;
import com.reason.application.springboot.config.auth.dto.SessionUser;
import com.reason.application.springboot.domain.user.User;
import com.reason.application.springboot.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Collections;

@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService implements OAuth2UserService <OAuth2UserRequest, OAuth2User> {
    private final UserRepository userRepository;
    private final HttpSession httpSession;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
        throws OAuth2AuthenticationException {
            OAuth2UserService delegate = new
                    DefaultOAuth2UserService();
            OAuth2User oAuth2User = delegate.
                    loadUser(userRequest);

            String registraionId = userRequest.
                    getClientRegistration().getRegistrationId(); // 현재 로그인 진행 중인 서비스 구분
            String userNameAttributeName = userRequest. // OAuth2에서 로그인 키가 되는 필드값
                    getClientRegistration().getProviderDetails()
                    .getUserInfoEndpoint().getUserNameAttributeName();

            OAuthAttributes attributes = OAuthAttributes. // OAuth2User의 Att를 담을 클래스
                    of(registraionId, userNameAttributeName,
                            oAuth2User.getAttributes());

            User user = saveOrUpdate(attributes);

            httpSession.setAttribute("user", new
                    SessionUser(user)); // 세션에 사용자 정보를 저장하기 위한 Dto 클래스

            return new DefaultOAuth2User(
                    Collections.singleton(new
                            SimpleGrantedAuthority(user.getRoleKey())),
                    attributes.getAttributes(),
                    attributes.getNameAttributeKey());
    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        User user = userRepository.findByEmail(attributes.getEmail())
                .map(entity -> entity.update(attributes
                .getName(), attributes.getPicture()))
                .orElse(attributes.toEntity());

        return userRepository.save(user);
    }
}
