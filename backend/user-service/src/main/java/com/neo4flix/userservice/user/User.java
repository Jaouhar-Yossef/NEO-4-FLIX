package com.neo4flix.userservice.user;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@Node("User")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(UUIDStringGenerator.class)
    private String id;

    private String email;
    private String displayName;
    private String passwordHash;
    private String role;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Boolean twoFactorEnabled;

    private String totpSecret;

    public String getRole() { return role == null ? "USER" : role; }

    public boolean isTwoFactorEnabled() { return Boolean.TRUE.equals(twoFactorEnabled); }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }
}
