package com.trackify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {
    
    private String secret;
    private long expiration = 86400000; // 24 hours in milliseconds
    private long refreshExpiration = 604800000; // 7 days in milliseconds
    private String header = "Authorization";
    private String prefix = "Bearer ";
    
    public long getExpirationInSeconds() {
        return expiration / 1000;
    }
    
    public long getRefreshExpirationInSeconds() {
        return refreshExpiration / 1000;
    }

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public long getExpiration() {
		return expiration;
	}

	public void setExpiration(long expiration) {
		this.expiration = expiration;
	}

	public long getRefreshExpiration() {
		return refreshExpiration;
	}

	public void setRefreshExpiration(long refreshExpiration) {
		this.refreshExpiration = refreshExpiration;
	}

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
}