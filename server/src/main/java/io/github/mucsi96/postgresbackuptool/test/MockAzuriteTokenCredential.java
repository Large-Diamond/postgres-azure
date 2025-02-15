package io.github.mucsi96.postgresbackuptool.test;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;

import io.jsonwebtoken.Jwts;
import reactor.core.publisher.Mono;

public class MockAzuriteTokenCredential implements TokenCredential {

    @Override
    public Mono<AccessToken> getToken(TokenRequestContext request) {
        return Mono.just(generateAccessToken(request));
    }

    private AccessToken generateAccessToken(TokenRequestContext request) {
        String audience = request.getScopes().isEmpty() ? "default-audience"
                : request.getScopes().get(0).replace("/.default", "");
        OffsetDateTime expires = OffsetDateTime.now().plusHours(1);
        String token = generateJwtToken(audience,
                new Date(System.currentTimeMillis() - 300_000),
                Date.from(expires.toInstant()));
        System.out.println("Generated token: " + token);
        return new AccessToken(token, expires);
    }

    private String generateJwtToken(String audience, Date notBefore,
            Date expires) {
        return Jwts.builder().issuer(
                "https://sts.windows.net/aaaaaaaa-aaaa-aaaa-0000-aaaaaaaaaaaa/")
                .audience().single(audience)
                .subject(UUID.randomUUID().toString())
                .claim("oid", "c0ffee00-c0ff-eeee-0000-c0ffee000000")
                .claim("tid", "aaaaaaaa-aaaa-aaaa-0000-aaaaaaaaaaaa")
                .issuedAt(new Date()).notBefore(notBefore).expiration(expires)
                .signWith(Jwts.SIG.HS512.key().build()).compact();
    }
}
