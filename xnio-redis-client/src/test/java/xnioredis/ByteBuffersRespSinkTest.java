package xnioredis;

import com.pholser.junit.quickcheck.ForAll;
import org.junit.contrib.theories.Theories;
import org.junit.contrib.theories.Theory;
import org.junit.runner.RunWith;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Theories.class)
public class ByteBuffersRespSinkTest {

    @Theory
    public void testToBytes(@ForAll long i) throws Exception {
        assertThat(ByteBuffersRespSink.toBytes(i), equalTo(Long.toString(i).getBytes(US_ASCII)));
    }
}
