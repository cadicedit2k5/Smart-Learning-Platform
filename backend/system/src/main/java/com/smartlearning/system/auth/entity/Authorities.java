package com.smartlearning.system.auth.entity;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class Authorities {
    public static final String USER_READ =
            "hasAuthority('USER_READ')";

    public static final String COURSE_READ =
            "hasAuthority('COURSE_READ')";

    public static final String COURSE_CREATE =
            "hasAuthority('COURSE_CREATE')";
}
