package com.trackify.security;
import com.trackify.config.JwtConfig;
import com.trackify.util.CookieUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {


	private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);
	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

	@Value("${app.oauth2.authorized-redirect-uris}")
	private String[] authorizedRedirectUris;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, 
			Authentication authentication) throws IOException, ServletException {

		String targetUrl = determineTargetUrl(request, response, authentication);

		if (response.isCommitted()) {
			logger.debug("Response has already been committed. Unable to redirect to " + targetUrl);
			return;
		}

		clearAuthenticationAttributes(request, response);
		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}

	protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, 
			Authentication authentication) {

		Optional<String> redirectUri = CookieUtils.getCookie(request, 
				HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
				.map(Cookie::getValue);

		if (redirectUri.isPresent() && !isAuthorizedRedirectUri(redirectUri.get())) {
			logger.error("Unauthorized redirect URI: {}", redirectUri.get());
			throw new RuntimeException("Unauthorized Redirect URI. Can't proceed with the authentication");
		}

		String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

		// Get OAuth2UserPrincipal
		OAuth2UserPrincipal oauth2UserPrincipal = (OAuth2UserPrincipal) authentication.getPrincipal();

		// Generate JWT tokens using the OAuth2 user methods
		String accessToken = jwtTokenProvider.generateTokenFromOAuth2User(oauth2UserPrincipal);
		String refreshToken = jwtTokenProvider.generateRefreshTokenFromOAuth2User(oauth2UserPrincipal);

		logger.info("OAuth2 authentication successful for user: {}", oauth2UserPrincipal.getEmail());

		return UriComponentsBuilder.fromUriString(targetUrl)
				.queryParam("token", accessToken)
				.queryParam("refreshToken", refreshToken)
				.queryParam("email", oauth2UserPrincipal.getEmail())
				.queryParam("name", oauth2UserPrincipal.getFirstName() + " " + oauth2UserPrincipal.getLastName())
				.build().toUriString();
	}

	protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
		super.clearAuthenticationAttributes(request);
		httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
	}

	private boolean isAuthorizedRedirectUri(String uri) {
		URI clientRedirectUri = URI.create(uri);

		for (String authorizedRedirectUri : authorizedRedirectUris) {
			URI authorizedURI = URI.create(authorizedRedirectUri);

			if (authorizedURI.getHost().equalsIgnoreCase(clientRedirectUri.getHost())
					&& authorizedURI.getPort() == clientRedirectUri.getPort()) {
				return true;
			}
		}
		return false;
	}
}