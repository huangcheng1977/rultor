/**
 * Copyright (c) 2009-2014, rultor.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the rultor.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.rultor.drain.ftp;

import com.rultor.spi.Drain;
import com.rultor.spi.Pageable;
import com.rultor.tools.Time;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.Test;

/**
 * Integration case for {@link FileDrain}.
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @checkstyle ClassDataAbstractionCoupling (500 lines)
 */
public final class FileDrainITCase {

    /**
     * FTP host.
     */
    private final transient String host =
        System.getProperty("failsafe.ftp.host");

    /**
     * FTP user name.
     */
    private final transient String login =
        System.getProperty("failsafe.ftp.login");

    /**
     * FTP password.
     */
    private final transient String password =
        System.getProperty("failsafe.ftp.password");

    /**
     * FTP file name.
     */
    private final transient String file =
        System.getProperty("failsafe.ftp.file");

    /**
     * FileDrain can log.
     * @throws Exception If some problem inside
     */
    @Test
    public void logsMessages() throws Exception {
        Assume.assumeNotNull(this.host);
        final String msg = "some test log message \u20ac";
        final Drain drain = new FileDrain(
            this.host, this.login, this.password, this.file
        );
        drain.append(Arrays.asList(msg));
        drain.append(Arrays.asList("something else"));
        final Pageable<Time, Time> names = drain.pulses();
        MatcherAssert.assertThat(names, Matchers.<Time>iterableWithSize(0));
        MatcherAssert.assertThat(
            IOUtils.toString(drain.read(), CharEncoding.UTF_8),
            Matchers.containsString(msg)
        );
    }

}