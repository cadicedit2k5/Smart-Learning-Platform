package com.smartlearning.common.entity;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class Authorities {
    public static final String USER_READ =
            "hasAuthority('USER_READ')";

    public static final String COURSE_READ =
            "hasAuthority('COURSE_READ')";

    public static final String COURSE_MANAGE =
            "hasAuthority('COURSE_MANAGE')";

    public static final String ROLE_MANAGE =
            "hasAuthority('ROLE_MANAGE')";
}
